package com.hireconnect.interviewservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.hireconnect.interviewservice.dto.InterviewDTO;
import com.hireconnect.interviewservice.dto.RescheduleRequestDTO;
import com.hireconnect.interviewservice.entity.Interview;
import com.hireconnect.interviewservice.exception.InvalidInterviewStatusException;
import com.hireconnect.interviewservice.exception.ResourceNotFoundException;
import com.hireconnect.interviewservice.repository.InterviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    private InterviewRepository repository;

    @InjectMocks
    private InterviewServiceImpl service;

    @Test
    void scheduleInterviewCreatesScheduledOnlineInterview() {
        InterviewDTO dto = dto();
        dto.setJobId(null);
        dto.setUserId(null);
        when(repository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Interview result = service.scheduleInterview(dto);

        assertThat(result.getApplicationId()).isEqualTo(10L);
        assertThat(result.getMode()).isEqualTo("ONLINE");
        assertThat(result.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void scheduleInterviewBuildsNotificationFromJobAndUserDetails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        InterviewDTO dto = dto();
        when(repository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        server.expect(requestTo("http://localhost:8083/jobs/3"))
                .andRespond(withSuccess("{\"title\":\"Backend Developer\",\"companyName\":\"HireConnect\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8082/users/2"))
                .andRespond(withSuccess("{\"name\":\"Ankit\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8086/notify/send"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        Interview result = service.scheduleInterview(dto);

        assertThat(result.getStatus()).isEqualTo("SCHEDULED");
        server.verify();
    }

    @Test
    void scheduleInterviewRejectsPastDate() {
        InterviewDTO dto = dto();
        dto.setInterviewDate(LocalDateTime.now().minusDays(1).toString());

        assertThatThrownBy(() -> service.scheduleInterview(dto))
                .isInstanceOf(InvalidInterviewStatusException.class)
                .hasMessageContaining("future");
    }

    @Test
    void scheduleInterviewRejectsInvalidMode() {
        InterviewDTO dto = dto();
        dto.setMode("telepathy");

        assertThatThrownBy(() -> service.scheduleInterview(dto))
                .isInstanceOf(InvalidInterviewStatusException.class)
                .hasMessageContaining("mode");
    }

    @Test
    void updateStatusNormalizesAndSaves() {
        Interview interview = interview("SCHEDULED");
        when(repository.findById(1L)).thenReturn(Optional.of(interview));
        when(repository.save(interview)).thenReturn(interview);

        Interview result = service.updateStatus(1L, "reschedule requested");

        assertThat(result.getStatus()).isEqualTo("RESCHEDULE_REQUESTED");
    }

    @Test
    void updateStatusRejectsInvalidValue() {
        when(repository.findById(1L)).thenReturn(Optional.of(interview("SCHEDULED")));

        assertThatThrownBy(() -> service.updateStatus(1L, "paused"))
                .isInstanceOf(InvalidInterviewStatusException.class);
    }

    @Test
    void confirmInterviewOnlyAllowsActiveInterviews() {
        Interview interview = interview("RESCHEDULED");
        when(repository.findById(1L)).thenReturn(Optional.of(interview));
        when(repository.save(interview)).thenReturn(interview);

        Interview result = service.confirmInterview(1L);

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        verify(repository).save(interview);
    }

    @Test
    void confirmInterviewRejectsCompletedInterview() {
        when(repository.findById(1L)).thenReturn(Optional.of(interview("COMPLETED")));

        assertThatThrownBy(() -> service.confirmInterview(1L))
                .isInstanceOf(InvalidInterviewStatusException.class);
    }

    @Test
    void requestRescheduleStoresRequestAndNotes() {
        Interview interview = interview("CONFIRMED");
        interview.setRecruiterId(null);
        interview.setJobId(null);
        RescheduleRequestDTO request = new RescheduleRequestDTO();
        request.setRequestedInterviewDate(LocalDateTime.now().plusDays(3).toString());
        request.setReason("Need more time");
        request.setNotes("Updated notes");
        when(repository.findById(1L)).thenReturn(Optional.of(interview));
        when(repository.save(interview)).thenReturn(interview);

        Interview result = service.requestReschedule(1L, request);

        assertThat(result.getStatus()).isEqualTo("RESCHEDULE_REQUESTED");
        assertThat(result.getRequestedInterviewDate()).isEqualTo(request.getRequestedInterviewDate());
        assertThat(result.getRescheduleReason()).isEqualTo("Need more time");
        assertThat(result.getNotes()).isEqualTo("Updated notes");
    }

    @Test
    void requestRescheduleBuildsRecruiterNotificationWithFallbacks() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        Interview interview = interview("SCHEDULED");
        interview.setInterviewDate(LocalDateTime.now().plusDays(2).toString());
        RescheduleRequestDTO request = new RescheduleRequestDTO();
        request.setRequestedInterviewDate(LocalDateTime.now().plusDays(4).toString());
        request.setReason("");
        request.setNotes("  ");
        when(repository.findById(1L)).thenReturn(Optional.of(interview));
        when(repository.save(interview)).thenReturn(interview);

        server.expect(requestTo("http://localhost:8083/jobs/3"))
                .andRespond(withSuccess("{\"companyName\":\"HireConnect\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8082/users/2"))
                .andRespond(withSuccess("{\"email\":\"candidate@example.com\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8086/notify/send"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        Interview result = service.requestReschedule(1L, request);

        assertThat(result.getStatus()).isEqualTo("RESCHEDULE_REQUESTED");
        assertThat(result.getNotes()).isEqualTo("Notes");
        server.verify();
    }

    @Test
    void requestRescheduleRejectsInactiveInterview() {
        RescheduleRequestDTO request = new RescheduleRequestDTO();
        request.setRequestedInterviewDate(LocalDateTime.now().plusDays(3).toString());
        when(repository.findById(1L)).thenReturn(Optional.of(interview("CANCELLED")));

        assertThatThrownBy(() -> service.requestReschedule(1L, request))
                .isInstanceOf(InvalidInterviewStatusException.class)
                .hasMessageContaining("active");
    }

    @Test
    void requestRescheduleRejectsPastDate() {
        RescheduleRequestDTO request = new RescheduleRequestDTO();
        request.setRequestedInterviewDate(LocalDateTime.now().minusDays(1).toString());
        when(repository.findById(1L)).thenReturn(Optional.of(interview("SCHEDULED")));

        assertThatThrownBy(() -> service.requestReschedule(1L, request))
                .isInstanceOf(InvalidInterviewStatusException.class);
    }

    @Test
    void readMethodsDelegateToRepository() {
        Interview interview = interview("SCHEDULED");
        when(repository.findByUserId(2L)).thenReturn(List.of(interview));
        when(repository.findByJobId(3L)).thenReturn(List.of(interview));
        when(repository.findByApplicationId(10L)).thenReturn(List.of(interview));

        assertThat(service.getByUser(2L)).containsExactly(interview);
        assertThat(service.getByJob(3L)).containsExactly(interview);
        assertThat(service.getByApplication(10L)).containsExactly(interview);
    }

    @Test
    void missingInterviewThrowsResourceNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmInterview(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private InterviewDTO dto() {
        InterviewDTO dto = new InterviewDTO();
        dto.setApplicationId(10L);
        dto.setJobId(3L);
        dto.setUserId(2L);
        dto.setRecruiterId(4L);
        dto.setInterviewDate(LocalDateTime.now().plusDays(2).toString());
        dto.setMode("online");
        dto.setLocation("Meet");
        dto.setMeetingLink("https://meet.example");
        dto.setNotes("Bring portfolio");
        return dto;
    }

    private Interview interview(String status) {
        return new Interview(
                10L,
                3L,
                2L,
                4L,
                LocalDateTime.now().plusDays(2).toString(),
                "ONLINE",
                "Meet",
                "https://meet.example",
                "Notes",
                status
        );
    }
}
