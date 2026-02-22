package com.movento.paymentservice.repository.elasticsearch;

import com.movento.paymentservice.model.elasticsearch.PaymentAuditLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentAuditLogRepository extends ElasticsearchRepository<PaymentAuditLog, String> {
    
    List<PaymentAuditLog> findByPaymentIntentIdOrderByTimestampDesc(String paymentIntentId);
    
    List<PaymentAuditLog> findByEventTypeAndTimestampBetween(
        String eventType, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    List<PaymentAuditLog> findByChangedByAndTimestampAfter(String changedBy, LocalDateTime since);
    
    List<PaymentAuditLog> findByMetadata_KeyAndMetadata_Value(String key, String value);
}
