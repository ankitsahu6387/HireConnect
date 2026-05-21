package com.hireconnect.applicationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.hireconnect.applicationservice.dto.ApplicationDTO;
import com.hireconnect.applicationservice.entity.Application;
import com.hireconnect.applicationservice.exception.DuplicateApplicationException;
import com.hireconnect.applicationservice.exception.InvalidStatusException;
import com.hireconnect.applicationservice.exception.ResourceNotFoundException;
import com.hireconnect.applicationservice.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository repository;

    @InjectMocks
    private ApplicationServiceImpl service;

    @Test
    void applyJobCreatesApplicationWhenUserHasNotApplied() {
        ApplicationDTO dto = applicationDto();
        when(repository.findByUserIdAndJobId(7L, 11L)).thenReturn(Optional.empty());
        when(repository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application result = service.applyJob(dto);

        assertThat(result.getJobId()).isEqualTo(11L);
        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(result.getResumeLink()).isEqualTo("resume.pdf");
        assertThat(result.getCoverLetter()).isEqualTo("hello");
        assertThat(result.getStatus()).isEqualTo("APPLIED");
    }

    @Test
    void applyJobRejectsDuplicateApplication() {
        ApplicationDTO dto = applicationDto();
        when(repository.findByUserIdAndJobId(7L, 11L)).thenReturn(Optional.of(new Application()));

        assertThatThrownBy(() -> service.applyJob(dto))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("Already applied");

        verify(repository, never()).save(any());
    }

    @Test
    void updateStatusNormalizesAndSavesAllowedStatus() {
        Application application = new Application(11L, 7L, "resume.pdf", "APPLIED");
        when(repository.findById(1L)).thenReturn(Optional.of(application));
        when(repository.save(application)).thenReturn(application);

        Application result = service.updateStatus(1L, " interview scheduled ");

        assertThat(result.getStatus()).isEqualTo("INTERVIEW_SCHEDULED");
        verify(repository).save(application);
    }

    @Test
    void updateStatusRejectsInvalidStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Application()));

        assertThatThrownBy(() -> service.updateStatus(1L, "maybe later"))
                .isInstanceOf(InvalidStatusException.class);
    }

    @Test
    void updateStatusThrowsWhenApplicationIsMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(99L, "APPLIED"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void readMethodsDelegateToRepository() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        Application application = new Application(11L, 7L, "resume.pdf", "APPLIED");
        when(repository.findByUserId(7L)).thenReturn(List.of(application));
        when(repository.findByJobId(11L)).thenReturn(List.of(application));
        server.expect(requestTo("http://localhost:8083/jobs/11"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"title\":\"Developer\"}", MediaType.APPLICATION_JSON));

        assertThat(service.getApplicationsByUser(7L)).containsExactly(application);
        assertThat(service.getApplicationsByJob(11L)).containsExactly(application);
        server.verify();
    }

    @Test
    void updateStatusSendsSavedEntityToRepository() {
        Application application = new Application(11L, 7L, "resume.pdf", "APPLIED");
        when(repository.findById(1L)).thenReturn(Optional.of(application));
        when(repository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(1L, "offered");

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("OFFERED");
    }

    @Test
    void updateStatusBuildsNotificationMessagesForMajorStatuses() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        when(repository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(1L)).thenReturn(
                Optional.of(new Application(11L, 7L, "resume.pdf", "APPLIED")),
                Optional.of(new Application(11L, 7L, "resume.pdf", "APPLIED")),
                Optional.of(new Application(11L, 7L, "resume.pdf", "APPLIED")),
                Optional.of(new Application(11L, 7L, "resume.pdf", "APPLIED"))
        );

        expectNotificationCalls(server);
        expectNotificationCalls(server);
        expectNotificationCalls(server);
        expectNotificationCalls(server);

        assertThat(service.updateStatus(1L, "shortlisted").getStatus()).isEqualTo("SHORTLISTED");
        assertThat(service.updateStatus(1L, "offered").getStatus()).isEqualTo("OFFERED");
        assertThat(service.updateStatus(1L, "rejected").getStatus()).isEqualTo("REJECTED");
        assertThat(service.updateStatus(1L, "withdrawn").getStatus()).isEqualTo("WITHDRAWN");
        server.verify();
    }

    private void expectNotificationCalls(MockRestServiceServer server) {
        server.expect(requestTo("http://localhost:8083/jobs/11"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"title\":\"Developer\",\"companyName\":\"HireConnect\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8082/users/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"name\":\"Ankit\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8086/notify/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    }

    private ApplicationDTO applicationDto() {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setJobId(11L);
        dto.setUserId(7L);
        dto.setResumeLink("resume.pdf");
        dto.setCoverLetter("hello");
        return dto;
    }
}
