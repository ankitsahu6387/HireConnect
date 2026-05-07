package com.hireconnect.jobservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.jobservice.entity.JobView;

public interface JobViewRepository extends JpaRepository<JobView, Long> {
    boolean existsByJobIdAndUserId(Long jobId, Long userId);
}
