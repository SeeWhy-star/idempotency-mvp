package com.example.idempotencymvp.service;

import com.example.idempotencymvp.controller.PaymentCallbackRequest;
import com.example.idempotencymvp.controller.PaymentCallbackResponse;
import com.example.idempotencymvp.domain.PaymentIntent;
import com.example.idempotencymvp.domain.PaymentIntentStatus;
import com.example.idempotencymvp.exception.ResourceNotFoundException;
import com.example.idempotencymvp.repository.PaymentIntentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentIntentTransitionService {

    private final PaymentIntentRepository repository;

    public PaymentIntentTransitionService(PaymentIntentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PaymentCallbackResponse transition(
            String paymentIntentId, PaymentCallbackRequest request) {
        validateTargetStatus(request.getStatus());

        // 锁只能串行化协作方；拿锁后重查，才能看到等待期间其他节点提交的最新状态。
        PaymentIntent current = repository.findById(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment intent not found: " + paymentIntentId));

        int affectedRows = repository.transitionFromPending(
                paymentIntentId,
                request.getStatus().name(),
                request.getChannelTransactionId());
        if (affectedRows == 1) {
            return new PaymentCallbackResponse(
                    true, true, paymentIntentId, request.getStatus());
        }

        // affectedRows == 0 代表重复回调或非法迁移；不再执行任何副作用，但向渠道返回成功。
        return new PaymentCallbackResponse(
                true, false, paymentIntentId, current.getStatus());
    }

    private void validateTargetStatus(PaymentIntentStatus status) {
        if (status != PaymentIntentStatus.SUCCEEDED && status != PaymentIntentStatus.FAILED) {
            throw new IllegalArgumentException(
                    "Callback status must be SUCCEEDED or FAILED");
        }
    }
}
