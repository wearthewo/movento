package com.movento.paymentservice.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="webhook_events") @Getter @Setter
public class WebhookEvent { @Id private String id; @Column(nullable=false) private String type; @Column(nullable=false) private Instant processedAt = Instant.now(); }
