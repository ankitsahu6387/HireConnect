package com.hireconnect.interviewservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.interviewservice.dto.InterviewDTO;
import com.hireconnect.interviewservice.dto.InterviewStatusDTO;
import com.hireconnect.interviewservice.dto.RescheduleRequestDTO;
import com.hireconnect.interviewservice.entity.Interview;
import com.hireconnect.interviewservice.repository.InterviewRepository;
import com.hireconnect.interviewservice.service.InterviewService;

@RestController
@RequestMapping("/interviews")
public class InterviewController {

    private final InterviewService service;

    public InterviewController(InterviewService service) {
        this.service = service;
    }

    @PostMapping
    public Interview schedule(@RequestBody InterviewDTO dto) {
        return service.scheduleInterview(dto);
    }

    @GetMapping("/{id}")
    public Interview getById(@PathVariable Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new com.hireconnect.interviewservice.exception.ResourceNotFoundException("Interview not found"));
    }

    @GetMapping("/user/{id}")
    public List<Interview> getByUser(@PathVariable Long id) {
        return service.getByUser(id);
    }

    @GetMapping("/job/{id}")
    public List<Interview> getByJob(@PathVariable Long id) {
        return service.getByJob(id);
    }

    @GetMapping("/application/{id}")
    public List<Interview> getByApplication(@PathVariable Long id) {
        return service.getByApplication(id);
    }

    @PutMapping("/{id}/status")
    public Interview updateStatus(@PathVariable Long id,
            @RequestBody InterviewStatusDTO dto) {
        return service.updateStatus(id, dto.getStatus());
    }

    @PatchMapping("/{id}/confirm")
    public Interview confirm(@PathVariable Long id) {
        return service.confirmInterview(id);
    }

    @PatchMapping("/{id}/reschedule-request")
    public Interview requestReschedule(@PathVariable Long id,
            @RequestBody RescheduleRequestDTO dto) {
        return service.requestReschedule(id, dto);
    }

    @Autowired
    private InterviewRepository interviewRepository;

    @GetMapping("/count")
    public Long getInterviewCount() {
        return interviewRepository.count();
    }
}
