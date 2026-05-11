package com.hireconnect.applicationservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.applicationservice.entity.Application;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserId(Long userId);

    List<Application> findByJobId(Long jobId);

    Optional<Application> findByUserIdAndJobId(Long userId, Long jobId);

    Long countByJobId(Long jobId);
}
