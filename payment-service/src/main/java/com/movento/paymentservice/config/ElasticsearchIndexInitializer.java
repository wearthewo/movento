package com.movento.paymentservice.config;

import com.movento.paymentservice.model.elasticsearch.PaymentAuditLog;
import com.movento.paymentservice.model.elasticsearch.PaymentDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @Value("${spring.elasticsearch.indices.payment.name:payments}")
    private String paymentIndexName;

    @Value("${spring.elasticsearch.indices.audit-log.name:payment_audit_logs}")
    private String auditLogIndexName;

    @Value("${elasticsearch.index.recreate:false}")
    private boolean recreateIndices;

    @PostConstruct
    public void init() {
        try {
            initPaymentIndex();
            initAuditLogIndex();
            log.info("Elasticsearch indices initialized successfully");
        } catch (IOException e) {
            log.error("Error initializing Elasticsearch indices", e);
            throw new RuntimeException("Failed to initialize Elasticsearch indices", e);
        }
    }

    private void initPaymentIndex() throws IOException {
        IndexOperations indexOps = elasticsearchOperations.indexOps(PaymentDocument.class);
        
        if (recreateIndices && indexExists(paymentIndexName)) {
            log.info("Deleting existing index: {}", paymentIndexName);
            indexOps.delete();
        }

        if (!indexExists(paymentIndexName)) {
            log.info("Creating index: {}", paymentIndexName);
            indexOps.create();
            
            // Apply custom mappings if needed
            String mapping = loadMapping("elasticsearch/mappings/payment-mapping.json");
            if (mapping != null) {
                indexOps.putMapping(Document.parse(mapping));
            }
        }
    }

    private void initAuditLogIndex() throws IOException {
        IndexOperations indexOps = elasticsearchOperations.indexOps(PaymentAuditLog.class);
        
        if (recreateIndices && indexExists(auditLogIndexName)) {
            log.info("Deleting existing index: {}", auditLogIndexName);
            indexOps.delete();
        }

        if (!indexExists(auditLogIndexName)) {
            log.info("Creating index: {}", auditLogIndexName);
            indexOps.create();
            
            // Apply custom mappings if needed
            String mapping = loadMapping("elasticsearch/mappings/audit-log-mapping.json");
            if (mapping != null) {
                indexOps.putMapping(Document.parse(mapping));
            }
        }
    }

    private boolean indexExists(String indexName) {
        return elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).exists();
    }

    private String loadMapping(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to load mapping from {}", path, e);
            return null;
        }
    }
}
