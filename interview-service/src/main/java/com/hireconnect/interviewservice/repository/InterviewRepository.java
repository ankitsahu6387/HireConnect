package com.hireconnect.interviewservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.interviewservice.entity.Interview;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByUserId(Long userId);

    List<Interview> findByJobId(Long jobId);

    List<Interview> findByApplicationId(Long applicationId);
}
