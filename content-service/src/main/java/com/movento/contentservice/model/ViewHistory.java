package com.movento.contentservice.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "view_history")
public class ViewHistory extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();
    
    @Column(name = "stopped_at_minute")
    private Integer stoppedAtMinute;
    
    @Column(name = "is_completed")
    private boolean completed = false;
    
    // Default constructor
    public ViewHistory() {
    }
    
    // Constructor with required fields
    public ViewHistory(Content content, String userId) {
        this.content = content;
        this.userId = userId;
    }
    
    // Helper method to mark as completed
    public void markAsCompleted() {
        this.completed = true;
        if (content != null) {
            this.stoppedAtMinute = content.getDurationMinutes();
        }
    }
    
    // Helper method to update progress
    public void updateProgress(int minute) {
        this.stoppedAtMinute = minute;
        if (content != null && minute >= content.getDurationMinutes()) {
            markAsCompleted();
        }
    }
}
