package com.xudu.articlepilot.controller;

import com.xudu.articlepilot.annotation.AuthCheck;
import com.xudu.articlepilot.common.BaseResponse;
import com.xudu.articlepilot.common.ResultUtils;
import com.xudu.articlepilot.constant.UserConstant;
import com.xudu.articlepilot.exception.BusinessException;
import com.xudu.articlepilot.exception.ErrorCode;
import com.xudu.articlepilot.model.entity.PaymentRecord;
import com.xudu.articlepilot.model.entity.User;
import com.xudu.articlepilot.service.PaymentRecordService;
import com.xudu.articlepilot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 支付控制器
 *
 */
@RestController
@RequestMapping("/payment")
@Slf4j
@Tag(name = "PaymentController", description = "支付接口")
public class PaymentController {

    @Resource
    private PaymentRecordService paymentService;

    @Resource
    private UserService userService;

    /**
     * 创建 VIP 支付会话
     */
    @PostMapping("/create-vip-session")
    @Operation(summary = "创建 VIP 支付会话")
    public BaseResponse<String> createVipPaymentSession(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        try {
            String sessionUrl = paymentService.createVipPaymentSession(loginUser.getId());
            return ResultUtils.success(sessionUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建支付会话失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建支付会话失败");
        }
    }

    /**
     * 申请退款
     */
    @PostMapping("/refund")
    @Operation(summary = "申请退款")
    @AuthCheck(mustRole = UserConstant.VIP_ROLE)
    public BaseResponse<Boolean> refund(
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        try {
            boolean success = paymentService.handleRefund(loginUser.getId(), reason);
            return ResultUtils.success(success);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("退款失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退款失败");
        }
    }

    /**
     * 获取当前用户支付记录
     */
    @GetMapping("/records")
    @Operation(summary = "获取当前用户支付记录")
    public BaseResponse<List<PaymentRecord>> getPaymentRecords(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<PaymentRecord> records = paymentService.getPaymentRecords(loginUser.getId());
        return ResultUtils.success(records);
    }
}