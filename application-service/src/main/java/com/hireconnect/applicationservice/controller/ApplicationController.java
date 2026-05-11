package com.hireconnect.applicationservice.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import com.hireconnect.applicationservice.dto.ApplicationDTO;
import com.hireconnect.applicationservice.dto.StatusUpdateDTO;
import com.hireconnect.applicationservice.entity.Application;
import com.hireconnect.applicationservice.service.ApplicationService;
import com.hireconnect.applicationservice.repository.ApplicationRepository;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public Application applyJob(@RequestBody ApplicationDTO dto) {
        return service.applyJob(dto);
    }

    @GetMapping("/user/{id}")
    public List<Application> getByUser(@PathVariable Long id) {
        return service.getApplicationsByUser(id);
    }

    @GetMapping("/user/{userId}/job/{jobId}")
    public ResponseEntity<Application> getByUserAndJob(@PathVariable Long userId, @PathVariable Long jobId) {
        return applicationRepository.findByUserIdAndJobId(userId, jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/job/{id}")
    public List<Application> getByJob(@PathVariable Long id) {
        return service.getApplicationsByJob(id);
    }

    @GetMapping("/job/{id}/count")
    public Long getCountByJob(@PathVariable Long id) {
        return applicationRepository.countByJobId(id);
    }

    @PutMapping("/{id}/status")
    public Application updateStatus(@PathVariable Long id,
                                    @RequestBody StatusUpdateDTO dto) {
        return service.updateStatus(id, dto.getStatus());
    }
    
    @Autowired
    private ApplicationRepository applicationRepository;
    @GetMapping("/count")
    public Long getApplicationCount() {
        return applicationRepository.count();
    }
    
//    @GetMapping("/test")
//    public String test() {
//        return "WORKING";
//    }
}
