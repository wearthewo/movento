package com.movento.paymentservice.service.elasticsearch;

import com.movento.paymentservice.model.elasticsearch.PaymentDocument;
import com.movento.paymentservice.repository.elasticsearch.PaymentDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSearchService {

    private final PaymentDocumentRepository paymentDocumentRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public void indexPayment(PaymentDocument paymentDocument) {
        try {
            paymentDocumentRepository.save(paymentDocument);
            log.debug("Indexed payment document with ID: {}", paymentDocument.getId());
        } catch (Exception e) {
            log.error("Error indexing payment document: {}", paymentDocument.getId(), e);
            // Consider implementing a retry mechanism or dead letter queue here
        }
    }

    public void bulkIndexPayments(List<PaymentDocument> payments) {
        try {
            paymentDocumentRepository.saveAll(payments);
            log.debug("Bulk indexed {} payment documents", payments.size());
        } catch (Exception e) {
            log.error("Error bulk indexing payment documents", e);
        }
    }

    public Page<PaymentDocument> searchPayments(String query, int page, int size) {
        StringQuery stringQuery = StringQuery.builder(query)
            .withPageable(PageRequest.of(page, size))
            .build();
        SearchHits<PaymentDocument> searchHits = elasticsearchOperations.search(stringQuery, PaymentDocument.class);
        List<PaymentDocument> content = searchHits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
        return new PageImpl<>(content, PageRequest.of(page, size), searchHits.getTotalHits());
    }

    public List<PaymentDocument> findPaymentsByAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        Criteria criteria = new Criteria("amount").between(minAmount.doubleValue(), maxAmount.doubleValue());
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(0, 100));
        
        return elasticsearchOperations.search(query, PaymentDocument.class)
            .stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    }

    public List<PaymentDocument> findPaymentsByStatus(String status) {
        return paymentDocumentRepository.findByStatus(status);
    }

    public List<PaymentDocument> findPaymentsByUserId(String userId) {
        return paymentDocumentRepository.findByUserId(userId);
    }

    public Map<String, Long> getPaymentStats() {
        Criteria criteria = new Criteria();
        CriteriaQuery query = new CriteriaQuery(criteria);
        
        // For now, return basic stats since aggregations require more complex setup
        List<PaymentDocument> allPayments = elasticsearchOperations.search(query, PaymentDocument.class)
            .stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
            
        return allPayments.stream()
            .collect(Collectors.groupingBy(
                PaymentDocument::getStatus,
                Collectors.counting()
            ));
    }

    public List<PaymentDocument> searchInDescription(String query) {
        return paymentDocumentRepository.searchInDescriptionAndCard(query);
    }

    public List<PaymentDocument> searchPayments(String userId, double minAmount, double maxAmount, String status) {
        return paymentDocumentRepository.searchPayments(userId, minAmount, maxAmount, status);
    }
}
