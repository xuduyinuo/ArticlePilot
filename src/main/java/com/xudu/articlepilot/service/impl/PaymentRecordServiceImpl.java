package com.xudu.articlepilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.xudu.articlepilot.config.StripeConfig;
import com.xudu.articlepilot.constant.UserConstant;
import com.xudu.articlepilot.exception.BusinessException;
import com.xudu.articlepilot.exception.ErrorCode;
import com.xudu.articlepilot.mapper.PaymentRecordMapper;
import com.xudu.articlepilot.mapper.UserMapper;
import com.xudu.articlepilot.model.entity.PaymentRecord;
import com.xudu.articlepilot.model.entity.User;
import com.xudu.articlepilot.model.enums.PaymentStatusEnum;
import com.xudu.articlepilot.model.enums.ProductTypeEnum;
import com.xudu.articlepilot.service.PaymentRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
* @author xudu
* @description 针对表【payment_record(支付记录表)】的数据库操作Service实现
* @createDate 2026-04-03 09:04:17
*/
@Slf4j
@Service
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord>
    implements PaymentRecordService{

    private static final String CURRENCY_USD = "usd";
    private static final long CENTS_MULTIPLIER = 100L;

    @Resource
    private StripeConfig stripeConfig;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    @Override
    public String createVipPaymentSession(Long userId) throws StripeException {
        User user = getUserOrThrow(userId);
        validateNotVip(user);

        ProductTypeEnum productType = ProductTypeEnum.VIP_PERMANENT;
        Session session = createStripeSession(userId, productType);
        savePaymentRecord(userId, session, productType);

        log.info("创建支付会话成功, userId={}, sessionId={}", userId, session.getId());
        return session.getUrl();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(Session session) {
        String sessionId = session.getId();
        String userId = session.getMetadata().get("userId");
        String paymentIntentId = session.getPaymentIntent();

        PaymentRecord record = findPaymentRecordBySessionId(sessionId);
        if (record == null) {
            log.warn("支付记录不存在, sessionId={}", sessionId);
            return;
        }

        // 幂等性检查
        if (PaymentStatusEnum.SUCCEEDED.getValue().equals(record.getStatus())) {
            log.info("支付记录已处理, sessionId={}", sessionId);
            return;
        }

        updatePaymentStatus(record.getId(), PaymentStatusEnum.SUCCEEDED, paymentIntentId);
        upgradeUserToVip(Long.valueOf(userId));

        log.info("支付成功，用户已升级为 VIP, userId={}, sessionId={}", userId, sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleRefund(Long userId, String reason) throws StripeException {
        User user = getUserOrThrow(userId);
        validateIsVip(user);

        PaymentRecord paymentRecord = findLatestSuccessfulPayment(userId);
        if (paymentRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到支付记录");
        }

        if (paymentRecord.getStripePaymentIntentId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "支付记录无效");
        }

        Refund refund = createStripeRefund(paymentRecord.getStripePaymentIntentId());
        if (!"succeeded".equals(refund.getStatus())) {
            return false;
        }

        updateRefundRecord(paymentRecord.getId(), reason);
        revokeVipStatus(userId);

        log.info("退款成功，已取消 VIP 身份, userId={}, refundId={}", userId, refund.getId());
        return true;
    }

    @Override
    public Event constructEvent(String payload, String sigHeader) throws Exception {
        return Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
    }

    @Override
    public List<PaymentRecord> getPaymentRecords(Long userId) {
        QueryWrapper<PaymentRecord> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(PaymentRecord::getUserId, userId)
                .orderByAsc(PaymentRecord::getCreateTime);

        return paymentRecordMapper.selectList(queryWrapper);
    }

    // ==================== 私有方法封装 ====================

    /**
     * 获取用户或抛出异常
     */
    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return user;
    }

    /**
     * 验证用户不是 VIP
     */
    private void validateNotVip(User user) {
        if (UserConstant.VIP_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已经是永久会员");
        }
    }

    /**
     * 验证用户是 VIP
     */
    private void validateIsVip(User user) {
        if (!UserConstant.VIP_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您不是会员，无法退款");
        }
    }

    /**
     * 创建 Stripe 支付会话
     */
    private Session createStripeSession(Long userId, ProductTypeEnum productType) throws StripeException {
        long amountInCents = productType.getPrice().multiply(new BigDecimal(CENTS_MULTIPLIER)).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeConfig.getSuccessUrl())
                .setCancelUrl(stripeConfig.getCancelUrl())
                .addLineItem(buildLineItem(productType, amountInCents))
                .putMetadata("userId", String.valueOf(userId))
                .putMetadata("productType", productType.getValue())
                .build();

        return Session.create(params);
    }

    /**
     * 构建支付行项目
     */
    private SessionCreateParams.LineItem buildLineItem(ProductTypeEnum productType, long amountInCents) {
        return SessionCreateParams.LineItem.builder()
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(CURRENCY_USD)
                                .setUnitAmount(amountInCents)
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(productType.getDescription())
                                                .setDescription("解锁全部高级功能，无限创作配额，终身有效")
                                                .build()
                                )
                                .build()
                )
                .setQuantity(1L)
                .build();
    }

    /**
     * 保存支付记录
     */
    private void savePaymentRecord(Long userId, Session session, ProductTypeEnum productType) {
        PaymentRecord record = PaymentRecord.builder()
                .userId(userId)
                .stripeSessionId(session.getId())
                .amount(productType.getPrice())
                .currency(CURRENCY_USD)
                .status(PaymentStatusEnum.PENDING.getValue())
                .productType(productType.getValue())
                .description(productType.getDescription())
                .build();
        paymentRecordMapper.insert(record);
    }

    /**
     * 根据 Session ID 查询支付记录
     */
    private PaymentRecord findPaymentRecordBySessionId(String sessionId) {
        QueryWrapper<PaymentRecord> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(PaymentRecord::getStripeSessionId, sessionId);

        return paymentRecordMapper.selectOne(queryWrapper);
    }

    /**
     * 查询最近的成功支付记录
     */
    private PaymentRecord findLatestSuccessfulPayment(Long userId) {
        QueryWrapper<PaymentRecord> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(PaymentRecord::getUserId, userId)
                .eq(PaymentRecord::getStatus, PaymentStatusEnum.SUCCEEDED.getValue())
                .eq(PaymentRecord::getProductType, ProductTypeEnum.VIP_PERMANENT.getValue())
                .orderByDesc(PaymentRecord::getCreateTime)
                .last("LIMIT 1");

        return paymentRecordMapper.selectOne(queryWrapper);
    }

    /**
     * 更新支付状态
     */
    private void updatePaymentStatus(Long recordId, PaymentStatusEnum status, String paymentIntentId) {
        PaymentRecord updateRecord = new PaymentRecord();

        updateRecord.setId(recordId);
        updateRecord.setStatus(status.getValue());
        updateRecord.setStripePaymentIntentId(paymentIntentId);

        paymentRecordMapper.updateById(updateRecord);
    }

    /**
     * 升级用户为 VIP
     */
    private void upgradeUserToVip(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setVipTime(LocalDateTime.now());
        user.setUserRole(UserConstant.VIP_ROLE);
        userMapper.updateById(user);
    }

    /**
     * 创建 Stripe 退款
     */
    private Refund createStripeRefund(String paymentIntentId) throws StripeException {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .build();
        return Refund.create(params);
    }

    /**
     * 更新退款记录
     */
    private void updateRefundRecord(Long recordId, String reason) {
        PaymentRecord updateRecord = new PaymentRecord();
        updateRecord.setId(recordId);
        updateRecord.setStatus(PaymentStatusEnum.REFUNDED.getValue());
        updateRecord.setRefundTime(LocalDateTime.now());
        updateRecord.setRefundReason(reason);
        paymentRecordMapper.updateById(updateRecord);
    }

    /**
     * 撤销用户 VIP 身份
     */
    private void revokeVipStatus(Long userId) {
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setVipTime(null);
        updateUser.setUserRole(UserConstant.DEFAULT_ROLE);
        updateUser.setQuota(UserConstant.DEFAULT_QUOTA);
        userMapper.updateById(updateUser);
    }

}




