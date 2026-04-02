package com.xudu.articlepilot.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xudu.articlepilot.model.dto.article.ArticleQueryRequest;
import com.xudu.articlepilot.model.dto.article.ArticleState;
import com.xudu.articlepilot.model.entity.Article;
import com.xudu.articlepilot.model.entity.User;
import com.xudu.articlepilot.model.enums.ArticleStatusEnum;
import com.xudu.articlepilot.model.vo.ArticleVO;

/**
* @author xudu
* @description 针对表【article(文章表)】的数据库操作Service
* @createDate 2026-03-16 19:43:06
*/
public interface ArticleService extends IService<Article> {
    /**
     * 创建文章任务
     *
     * @param topic     选题
     * @param style     文章风格（可为空）
     * @param loginUser 当前登录用户
     * @return 任务ID
     */
    String createArticleTask(String topic, String style, User loginUser);


    /**
     * 创建文章任务（带配额检查）
     * 将配额扣减和任务创建放在同一事务中，确保原子性
     *
     * @param topic     选题
     * @param style     文章风格（可为空）
     * @param loginUser 当前登录用户
     * @return 任务ID
     */
    String createArticleTaskWithQuotaCheck(String topic, String style, User loginUser);

    /**
     * 根据任务ID获取文章
     *
     * @param taskId 任务ID
     * @return 文章实体
     */
    Article getByTaskId(String taskId);

    /**
     * 获取文章详情（带权限校验）
     *
     * @param taskId    任务ID
     * @param loginUser 当前登录用户
     * @return 文章VO
     */
    ArticleVO getArticleDetail(String taskId, User loginUser);

    /**
     * 分页查询文章列表
     *
     * @param request   查询请求
     * @param loginUser 当前登录用户
     * @return 分页结果
     */
    Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser);

    /**
     * 删除文章（带权限校验）
     *
     * @param id        文章ID
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean deleteArticle(Long id, User loginUser);

    /**
     * 更新文章状态
     *
     * @param taskId       任务ID
     * @param status       状态枚举
     * @param errorMessage 错误信息（可选）
     */
    void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage);

    /**
     * 保存文章内容
     *
     * @param taskId 任务ID
     * @param state  文章状态对象
     */
    void saveArticleContent(String taskId, ArticleState state);
}
