package com.movento.paymentservice.model.elasticsearch;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(indexName = "payment_audit_logs")
@Setting(settingPath = "/elasticsearch/settings/audit-log-settings.json")
public class PaymentAuditLog {

    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String paymentIntentId;
    
    @Field(type = FieldType.Keyword)
    private String eventType; // CREATED, UPDATED, FAILED, etc.
    
    @Field(type = FieldType.Object)
    private Map<String, Object> oldValues;
    
    @Field(type = FieldType.Object)
    private Map<String, Object> newValues;
    
    @Field(type = FieldType.Keyword)
    private String changedBy;
    
    @Field(type = FieldType.Keyword)
    private String ipAddress;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime timestamp;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String message;
    
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;
}
