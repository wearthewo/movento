package com.movento.paymentservice.repository;
import com.movento.paymentservice.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {}
