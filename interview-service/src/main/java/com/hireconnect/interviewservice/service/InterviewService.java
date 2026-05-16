package com.hireconnect.interviewservice.service;

import java.util.List;

import com.hireconnect.interviewservice.dto.InterviewDTO;
import com.hireconnect.interviewservice.dto.RescheduleRequestDTO;
import com.hireconnect.interviewservice.entity.Interview;

public interface InterviewService {

    Interview scheduleInterview(InterviewDTO dto);

    List<Interview> getByUser(Long userId);

    List<Interview> getByJob(Long jobId);

    List<Interview> getByApplication(Long applicationId);

    Interview updateStatus(Long id, String status);

    Interview confirmInterview(Long id);

    Interview requestReschedule(Long id, RescheduleRequestDTO dto);
}
