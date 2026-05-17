package com.hireconnect.jobservice.service;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hireconnect.jobservice.dto.JobDTO;
import com.hireconnect.jobservice.entity.Job;
import com.hireconnect.jobservice.entity.JobView;
import com.hireconnect.jobservice.exception.ResourceNotFoundException;
import com.hireconnect.jobservice.exception.UnauthorizedActionException;
import com.hireconnect.jobservice.repository.JobRepository;
import com.hireconnect.jobservice.repository.JobViewRepository;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository repository;
    private final JobViewRepository viewRepository;
    private final RestTemplate restTemplate;

    public JobServiceImpl(JobRepository repository, JobViewRepository viewRepository, RestTemplate restTemplate) {
        this.repository = repository;
        this.viewRepository = viewRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public Job createJob(JobDTO dto) {

        if (dto.getRole() == null || !dto.getRole().equalsIgnoreCase("EMPLOYER")) {
            throw new UnauthorizedActionException("Only employer can create job");
        }

        Job job = new Job(
                dto.getEmployerId(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getLocation(),
                dto.getSalary()
        );
        applyDetails(job, dto);

        Job saved = repository.save(job);
        sendEmployerJobPostedNotification(saved);
        sendNewJobAlert(saved);
        return saved;
    }

    @Override
    public Job getJobById(Long id, Long userId) {
        Job job = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (userId == null || viewRepository.existsByJobIdAndUserId(id, userId)) {
            return job;
        }

        viewRepository.save(new JobView(id, userId));
        job.setViewCount((job.getViewCount() == null ? 0L : job.getViewCount()) + 1);
        return repository.save(job);
    }

    @Override
    public List<Job> getAllJobs() {
        return repository.findAll();
    }

    @Override
    public List<Job> searchJobs(String keyword, String location, String category, String type, String status) {
        return repository.searchJobs(blankToNull(keyword), blankToNull(location), blankToNull(category), blankToNull(type), blankToNull(status));
    }

    @Override
    public List<Job> getJobsByEmployer(Long employerId) {
        return repository.findByEmployerId(employerId);
    }

    @Override
    public Job updateJob(Long id, JobDTO dto) {

        Job job = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Only same employer can update
        if (!job.getEmployerId().equals(dto.getEmployerId())) {
            throw new UnauthorizedActionException("You are not allowed to update this job");
        }

        job.setTitle(dto.getTitle());
        job.setCompanyName(dto.getCompanyName());
        job.setDescription(dto.getDescription());
        job.setLocation(dto.getLocation());
        job.setSalary(dto.getSalary());
        applyDetails(job, dto);

        return repository.save(job);
    }

    @Override
    public void deleteJob(Long id, Long employerId) {

        Job job = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Only same employer can delete
        if (!job.getEmployerId().equals(employerId)) {
            throw new UnauthorizedActionException("You are not allowed to delete this job");
        }

        repository.delete(job);
    }

    private void applyDetails(Job job, JobDTO dto) {
        job.setCompanyName(dto.getCompanyName());
        job.setCategory(dto.getCategory());
        job.setType(dto.getType());
        job.setSkills(dto.getSkills());
        job.setExperienceRequired(dto.getExperienceRequired());
        if (job.getViewCount() == null) {
            job.setViewCount(0L);
        }
        job.setStatus(dto.getStatus() == null || dto.getStatus().isBlank() ? "OPEN" : dto.getStatus());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void sendNewJobAlert(Job job) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("jobId", job.getId());
            payload.put("title", job.getTitle());
            payload.put("companyName", job.getCompanyName());
            payload.put("location", job.getLocation());
            payload.put("category", job.getCategory());
            restTemplate.postForObject("http://notification-service/notify/new-job-alert", payload, Object.class);
        } catch (Exception ignored) {
            // Notification delivery should not block job posting.
        }
    }

    private void sendEmployerJobPostedNotification(Job job) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", job.getEmployerId());
            payload.put("type", "JOB_POSTED");
            payload.put("subject", "Job posted successfully");
            payload.put("message", "Your job post \"" + emptyFallback(job.getTitle(), "Open role")
                    + "\" is now live on HireConnect.");
            payload.put("sendEmail", false);
            restTemplate.postForObject("http://notification-service/notify/send", payload, Object.class);
        } catch (Exception ignored) {
            // Notification delivery should not block job posting.
        }
    }

    private String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
