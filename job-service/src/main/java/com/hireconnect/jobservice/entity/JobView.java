package com.hireconnect.jobservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "user_id"})
)
public class JobView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private LocalDateTime viewedAt = LocalDateTime.now();

    public JobView() {}

    public JobView(Long jobId, Long userId) {
        this.jobId = jobId;
        this.userId = userId;
    }

    public Long getId() { return id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
}
