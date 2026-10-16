package com.example.idempotencymvp.repository;

import com.example.idempotencymvp.domain.PaymentIntent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, String> {

    Optional<PaymentIntent> findByAccountIdAndMerchantOrderNo(
            String accountId, String merchantOrderNo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "UPDATE payment_intent "
                    + "SET status = :status, channel_transaction_id = :channelTransactionId, "
                    + "updated_at = CURRENT_TIMESTAMP "
                    + "WHERE id = :id "
                    + "AND status IN ('PROCESSING', 'REQUIRES_ACTION')",
            nativeQuery = true)
    int transitionFromPending(
            @Param("id") String id,
            @Param("status") String status,
            @Param("channelTransactionId") String channelTransactionId);
}
