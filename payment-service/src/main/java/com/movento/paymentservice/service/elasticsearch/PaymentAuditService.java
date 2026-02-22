package com.movento.paymentservice.service.elasticsearch;

import com.movento.paymentservice.model.elasticsearch.PaymentAuditLog;
import com.movento.paymentservice.repository.elasticsearch.PaymentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAuditService {

    private final PaymentAuditLogRepository auditLogRepository;

    public PaymentAuditLog logPaymentEvent(
            String paymentIntentId,
            String eventType,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            String changedBy,
            String ipAddress,
            String message,
            Map<String, Object> metadata) {
        
        PaymentAuditLog auditLog = new PaymentAuditLog();
        auditLog.setPaymentIntentId(paymentIntentId);
        auditLog.setEventType(eventType);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        auditLog.setChangedBy(changedBy);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setMessage(message);
        auditLog.setMetadata(metadata);

        try {
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log payment audit event: {}", paymentIntentId, e);
            // Consider implementing a fallback logging mechanism here
            return null;
        }
    }

    public List<PaymentAuditLog> getPaymentAuditLogs(String paymentIntentId) {
        return auditLogRepository.findByPaymentIntentIdOrderByTimestampDesc(paymentIntentId);
    }

    public List<PaymentAuditLog> getRecentAuditLogs(String eventType, int hours) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusHours(hours);
        return auditLogRepository.findByEventTypeAndTimestampBetween(eventType, startDate, endDate);
    }

    public List<PaymentAuditLog> getUserActivity(String userId, LocalDateTime since) {
        return auditLogRepository.findByChangedByAndTimestampAfter(userId, since);
    }

    public List<PaymentAuditLog> searchAuditLogsByMetadata(String key, String value) {
        return auditLogRepository.findByMetadata_KeyAndMetadata_Value(key, value);
    }

    public void deleteOldAuditLogs(LocalDateTime cutoffDate) {
        // Note: In a production environment, consider using a scheduled task with a proper retention policy
        // and potentially using Elasticsearch's index lifecycle management (ILM) for better performance
        log.info("Deleting audit logs older than: {}", cutoffDate);
        // Implementation depends on your specific requirements and data retention policies
    }
}
