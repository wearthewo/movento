package com.movento.contentservice.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="watchlist_items", uniqueConstraints=@UniqueConstraint(columnNames={"profile_id","content_id"})) @Getter @Setter
public class WatchlistItem { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="profile_id",nullable=false) private UUID profileId; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="content_id") private Content content; @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); }
