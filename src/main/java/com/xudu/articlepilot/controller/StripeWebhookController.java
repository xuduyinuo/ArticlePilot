package com.xudu.articlepilot.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.xudu.articlepilot.service.PaymentRecordService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stripe Webhook 控制器
 *
 */
@RestController
@RequestMapping("/webhook")
@Slf4j
@Hidden
public class StripeWebhookController {

    @Resource
    private PaymentRecordService paymentService;

    /**
     * 处理 Stripe Webhook 回调
     */
    @PostMapping("/stripe")
    public String handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            // 验证 Webhook 签名
            Event event = paymentService.constructEvent(payload, sigHeader);

            log.info("收到 Stripe Webhook 事件，type={}", event.getType());

            // 处理事件
            switch (event.getType()) {
                case "checkout.session.completed":
                    // 支付成功 - 使用新方法提取 Session ID，然后重新获取完整对象
                    String sessionId = extractSessionIdFromEvent(event, payload);
                    if (sessionId == null || sessionId.isEmpty()) {
                        log.error("无法从事件中提取 Session ID");
                        return "error";
                    }

                    log.info("从事件中提取到 Session ID: {}", sessionId);

                    // 通过 Stripe API 重新获取完整的 Session 对象
                    Session session = Session.retrieve(sessionId);
                    if (session == null) {
                        log.error("无法通过 API 检索到 Session 对象，sessionId={}", sessionId);
                        return "error";
                    }

                    log.info("成功检索到 Session 对象，sessionId={}", session.getId());
                    paymentService.handlePaymentSuccess(session);
                    break;

                case "checkout.session.async_payment_succeeded":
                    // 异步支付成功 - 使用同样的方式处理
                    String asyncSessionId = extractSessionIdFromEvent(event, payload);
                    if (asyncSessionId != null && !asyncSessionId.isEmpty()) {
                        Session asyncSession = Session.retrieve(asyncSessionId);
                        if (asyncSession != null) {
                            paymentService.handlePaymentSuccess(asyncSession);
                        } else {
                            log.warn("无法检索异步支付 Session，sessionId={}", asyncSessionId);
                        }
                    }
                    break;

                default:
                    log.info("未处理的事件类型：{}", event.getType());
                    break;
            }

            return "success";
        } catch (Exception e) {
            log.error("处理 Stripe Webhook 失败", e);
            return "error";
        }
    }

    /**
     * 从 Event 对象或原始 JSON 中提取 Session ID
     * 这是最可靠的方式，避免 SDK 反序列化问题
     */
    private String extractSessionIdFromEvent(Event event, String rawPayload) {
        try {
            // 方法 1: 尝试从 Stripe 对象直接获取 ID
            // checkout.session.completed 事件的 data.object.id 就是 sessionId
            var dataObject = event.getDataObjectDeserializer();
            if (dataObject.getObject().isPresent()) {
                var stripeObject = dataObject.getObject().get();
                if (stripeObject instanceof com.stripe.model.HasId) {
                    return ((com.stripe.model.HasId) stripeObject).getId();
                }
            }

            // 方法 2: 如果 getObject() 失败，直接从原始 JSON 解析
            log.warn("getObject() 返回空，尝试从原始 JSON 解析 Session ID");
            Gson gson = new Gson();
            JsonObject jsonElement = gson.fromJson(rawPayload, JsonObject.class);

            if (jsonElement.has("data") && jsonElement.getAsJsonObject("data").has("object")) {
                JsonObject dataObjectJson = jsonElement.getAsJsonObject("data").getAsJsonObject("object");

                // 检查是否是 checkout session 类型
                if ("checkout.session".equals(dataObjectJson.get("object").getAsString())) {
                    String sessionId = dataObjectJson.has("id") ?
                            dataObjectJson.get("id").getAsString() : null;

                    if (sessionId != null) {
                        log.info("成功从原始 JSON 解析出 Session ID: {}", sessionId);
                        return sessionId;
                    }
                }
            }

            log.error("无法从任何来源提取 Session ID");
            return null;

        } catch (Exception e) {
            log.error("提取 Session ID 过程中发生异常", e);
            return null;
        }
    }
}