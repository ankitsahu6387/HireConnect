package com.hireconnect.applicationservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.hireconnect.applicationservice.entity.Application;
import com.hireconnect.applicationservice.repository.ApplicationRepository;
import com.hireconnect.applicationservice.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService service;

    @Mock
    private ApplicationRepository applicationRepository;

    private ApplicationController controller;

    @BeforeEach
    void setUp() {
        controller = new ApplicationController(service, applicationRepository);
    }

    @Test
    void getByUserAndJobReturnsApplicationWhenPresent() {
        Application application = new Application(6L, 2L, "resume.pdf", "APPLIED");
        when(applicationRepository.findByUserIdAndJobId(2L, 6L)).thenReturn(Optional.of(application));

        ResponseEntity<Application> response = controller.getByUserAndJob(2L, 6L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(application);
    }

    @Test
    void getByUserAndJobReturnsNoContentWhenUserHasNotApplied() {
        when(applicationRepository.findByUserIdAndJobId(2L, 6L)).thenReturn(Optional.empty());

        ResponseEntity<Application> response = controller.getByUserAndJob(2L, 6L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }
}
