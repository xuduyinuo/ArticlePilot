package com.xudu.articlepilot.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xudu.articlepilot.annotation.AuthCheck;
import com.xudu.articlepilot.common.BaseResponse;
import com.xudu.articlepilot.common.DeleteRequest;
import com.xudu.articlepilot.common.ResultUtils;
import com.xudu.articlepilot.exception.BusinessException;
import com.xudu.articlepilot.exception.ErrorCode;
import com.xudu.articlepilot.exception.ThrowUtils;
import com.xudu.articlepilot.manager.SseEmitterManager;
import com.xudu.articlepilot.mapper.ArticleMapper;
import com.xudu.articlepilot.model.dto.article.ArticleCreateRequest;
import com.xudu.articlepilot.model.dto.article.ArticleQueryRequest;
import com.xudu.articlepilot.model.entity.Article;
import com.xudu.articlepilot.model.entity.User;
import com.xudu.articlepilot.model.vo.ArticleVO;
import com.xudu.articlepilot.service.ArticleAsyncService;
import com.xudu.articlepilot.service.ArticleService;
import com.xudu.articlepilot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/article")
@Slf4j
public class ArticleController {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private ArticleService articleService;

    @Resource
    private ArticleAsyncService articleAsyncService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private UserService userService;

    /**
     * 创建文章任务
     */
    @PostMapping("/create")
    @Operation(summary = "创建文章任务")
    @AuthCheck(mustRole = "user")
    public BaseResponse<String> createArticle(@RequestBody ArticleCreateRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTopic() == null || request.getTopic().trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "选题不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 生成任务ID
        String taskId = IdUtil.simpleUUID();

        // 创建文章记录
        Article article = new Article();
        article.setTaskId(taskId);
        article.setUserId(loginUser.getId());
        article.setTopic(request.getTopic());
        article.setStatus("PENDING");
        article.setCreateTime(LocalDateTime.now());
        
        articleMapper.insert(article);

        // 异步执行文章生成
        articleAsyncService.executeArticleGeneration(taskId, request.getTopic());

        log.info("文章任务已创建, taskId={}, userId={}", taskId, loginUser.getId());
        return ResultUtils.success(taskId);
    }

    /**
     * SSE 进度推送
     */
    @GetMapping("/progress/{taskId}")
    @Operation(summary = "获取文章生成进度(SSE)")
    public SseEmitter getProgress(@PathVariable String taskId, HttpServletRequest httpServletRequest) {        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        // 校验权限（内部会检查任务是否存在以及用户是否有权限访问）
        User loginUser = userService.getLoginUser(httpServletRequest);
        articleService.getArticleDetail(taskId, loginUser);
        // 创建 SSE Emitter
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);
        
        log.info("SSE 连接已建立, taskId={}", taskId);
        return emitter;
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取文章详情")
    public BaseResponse<ArticleVO> getArticle(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        ArticleVO articleVO = articleService.getArticleDetail(taskId, loginUser);

        return ResultUtils.success(articleVO);
    }

    /**
     * 分页查询文章列表
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询文章列表")
    public BaseResponse<Page<ArticleVO>> listArticle(@RequestBody ArticleQueryRequest request,
                                                     HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);

        Page<ArticleVO> articleVOPage = articleService.listArticleByPage(request, loginUser);

        return ResultUtils.success(articleVOPage);
    }

    /**
     * 删除文章
     */
    @PostMapping("/delete")
    @Operation(summary = "删除文章")
    public BaseResponse<Boolean> deleteArticle(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, 
                ErrorCode.PARAMS_ERROR);
        
        User loginUser = userService.getLoginUser(httpServletRequest);

        boolean result = articleService.deleteArticle(deleteRequest.getId(), loginUser);
        
        return ResultUtils.success(result);
    }
}