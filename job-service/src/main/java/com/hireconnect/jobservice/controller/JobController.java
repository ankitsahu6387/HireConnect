package com.hireconnect.jobservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.jobservice.dto.JobDTO;
import com.hireconnect.jobservice.entity.Job;
import com.hireconnect.jobservice.repository.JobRepository;
import com.hireconnect.jobservice.service.JobService;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @PostMapping
    public Job createJob(@RequestBody JobDTO dto) {
        return service.createJob(dto);
    }

    @GetMapping("/{id}")
    public Job getJob(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        return service.getJobById(id, userId);
    }

    @GetMapping
    public List<Job> getAllJobs(@RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String location,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String type,
                                @RequestParam(required = false) String status) {
        if (keyword != null || location != null || category != null || type != null || status != null) {
            return service.searchJobs(keyword, location, category, type, status);
        }
        return service.getAllJobs();
    }

    @GetMapping("/employer/{id}")
    public List<Job> getJobsByEmployer(@PathVariable Long id) {
        return service.getJobsByEmployer(id);
    }

    @PutMapping("/{id}")
    public Job updateJob(@PathVariable Long id, @RequestBody JobDTO dto) {
        return service.updateJob(id, dto);
    }

    @DeleteMapping("/{id}/{employerId}")
    public String deleteJob(@PathVariable Long id, @PathVariable Long employerId) {
        service.deleteJob(id, employerId);
        return "Job deleted successfully";
    }

    @Autowired
    private JobRepository jobRepository;

    @GetMapping("/count")
    public Long getJobCount() {
        return jobRepository.count();
    }
}
