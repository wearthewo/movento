package com.movento.paymentservice.repository.elasticsearch;

import com.movento.paymentservice.model.elasticsearch.PaymentDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PaymentDocumentRepository extends ElasticsearchRepository<PaymentDocument, String> {
    
    List<PaymentDocument> findByPaymentIntentId(String paymentIntentId);
    
    List<PaymentDocument> findByUserId(String userId);
    
    List<PaymentDocument> findByStatus(String status);
    
    @Query("""
        {
            "bool": {
                "must": [
                    {"match": {"userId": "?0"}},
                    {"range": {"amount": {"gte": "?1", "lte": "?2"}}},
                    {"term": {"status": "?3"}}
                ]
            }
        }
    """)
    List<PaymentDocument> searchPayments(String userId, double minAmount, double maxAmount, String status);
    
    @Query("""
        {
            "query_string": {
                "query": "*?0*",
                "fields": ["description", "cardDetails.last4"]
            }
        }
    """)
    List<PaymentDocument> searchInDescriptionAndCard(String query);
}
