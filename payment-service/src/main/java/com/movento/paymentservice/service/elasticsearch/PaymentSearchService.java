package com.movento.paymentservice.service.elasticsearch;

import com.movento.paymentservice.model.elasticsearch.PaymentDocument;
import com.movento.paymentservice.repository.elasticsearch.PaymentDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
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
        return paymentDocumentRepository.search(
            QueryBuilders.queryStringQuery(query),
            PageRequest.of(page, size)
        );
    }

    public List<PaymentDocument> findPaymentsByAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.rangeQuery("amount")
                .gte(minAmount.doubleValue())
                .lte(maxAmount.doubleValue()))
            .withSort(SortBuilders.fieldSort("amount").order(SortOrder.ASC))
            .build();

        return elasticsearchOperations.search(searchQuery, PaymentDocument.class)
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
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .addAggregation(org.elasticsearch.search.aggregations.AggregationBuilders
                .terms("status_agg").field("status"))
            .build();

        return elasticsearchOperations.search(searchQuery, PaymentDocument.class)
            .getAggregations()
            .get("status_agg")
            .getBuckets()
            .stream()
            .collect(Collectors.toMap(
                bucket -> bucket.getKeyAsString(),
                bucket -> bucket.getDocCount()
            ));
    }

    public List<PaymentDocument> searchInDescription(String query) {
        return paymentDocumentRepository.searchInDescriptionAndCard(query);
    }

    public List<PaymentDocument> searchPayments(String userId, double minAmount, double maxAmount, String status) {
        return paymentDocumentRepository.searchPayments(userId, minAmount, maxAmount, status);
    }
}
