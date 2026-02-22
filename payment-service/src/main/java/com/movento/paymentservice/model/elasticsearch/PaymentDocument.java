package com.movento.paymentservice.model.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "payments")
@Setting(settingPath = "/elasticsearch/settings/payment-settings.json")
public class PaymentDocument {

    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String paymentIntentId;
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    @Field(type = FieldType.Double)
    private BigDecimal amount;
    
    @Field(type = FieldType.Keyword)
    private String currency;
    
    @Field(type = FieldType.Keyword)
    private String userId;
    
    @Field(type = FieldType.Keyword)
    private String planId;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Nested)
    private CardDetails cardDetails;
    
    @Data
    public static class CardDetails {
        @Field(type = FieldType.Keyword)
        private String last4;
        
        @Field(type = FieldType.Keyword)
        private String brand;
        
        @Field(type = FieldType.Keyword)
        private String country;
    }
}
