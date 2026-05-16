package com.hireconnect.applicationservice.service;

import java.util.List;

import com.hireconnect.applicationservice.dto.ApplicationDTO;
import com.hireconnect.applicationservice.entity.Application;

public interface ApplicationService {

    Application applyJob(ApplicationDTO dto);

    List<Application> getApplicationsByUser(Long userId);

    List<Application> getApplicationsByJob(Long jobId);

    Application updateStatus(Long id, String status);
}