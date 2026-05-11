package com.hireconnect.jobservice.service;

import java.util.List;

import com.hireconnect.jobservice.entity.Job;
import com.hireconnect.jobservice.dto.JobDTO;

public interface JobService {

    Job createJob(JobDTO dto);

    Job getJobById(Long id, Long userId);

    List<Job> getAllJobs();

    List<Job> searchJobs(String keyword, String location, String category, String type, String status);

    List<Job> getJobsByEmployer(Long employerId);

    Job updateJob(Long id, JobDTO dto);

    void deleteJob(Long id, Long employerId);
}
