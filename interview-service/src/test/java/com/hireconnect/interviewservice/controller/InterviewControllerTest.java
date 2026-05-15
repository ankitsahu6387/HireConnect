package com.hireconnect.interviewservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.hireconnect.interviewservice.dto.InterviewDTO;
import com.hireconnect.interviewservice.dto.InterviewStatusDTO;
import com.hireconnect.interviewservice.dto.RescheduleRequestDTO;
import com.hireconnect.interviewservice.entity.Interview;
import com.hireconnect.interviewservice.exception.ResourceNotFoundException;
import com.hireconnect.interviewservice.repository.InterviewRepository;
import com.hireconnect.interviewservice.service.InterviewService;

@ExtendWith(MockitoExtension.class)
class InterviewControllerTest {

    @Mock
    private InterviewService service;

    @Mock
    private InterviewRepository interviewRepository;

    private InterviewController controller;

    @BeforeEach
    void setUp() {
        controller = new InterviewController(service);
        ReflectionTestUtils.setField(controller, "interviewRepository", interviewRepository);
    }

    @Test
    void scheduleDelegatesToService() {
        InterviewDTO dto = new InterviewDTO();
        Interview interview = interview(1L);
        when(service.scheduleInterview(dto)).thenReturn(interview);

        assertThat(controller.schedule(dto)).isSameAs(interview);
    }

    @Test
    void getByIdReadsRepositoryAndThrowsWhenMissing() {
        Interview interview = interview(2L);
        when(interviewRepository.findById(2L)).thenReturn(Optional.of(interview));
        when(interviewRepository.findById(3L)).thenReturn(Optional.empty());

        assertThat(controller.getById(2L)).isSameAs(interview);
        assertThatThrownBy(() -> controller.getById(3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Interview not found");
    }

    @Test
    void listEndpointsDelegateToService() {
        List<Interview> interviews = List.of(interview(1L));
        when(service.getByUser(7L)).thenReturn(interviews);
        when(service.getByJob(8L)).thenReturn(interviews);
        when(service.getByApplication(9L)).thenReturn(interviews);

        assertThat(controller.getByUser(7L)).isSameAs(interviews);
        assertThat(controller.getByJob(8L)).isSameAs(interviews);
        assertThat(controller.getByApplication(9L)).isSameAs(interviews);
    }

    @Test
    void statusConfirmAndRescheduleDelegateToService() {
        Interview interview = interview(4L);
        InterviewStatusDTO statusDTO = new InterviewStatusDTO();
        statusDTO.setStatus("CONFIRMED");
        RescheduleRequestDTO rescheduleRequestDTO = new RescheduleRequestDTO();

        when(service.updateStatus(4L, "CONFIRMED")).thenReturn(interview);
        when(service.confirmInterview(4L)).thenReturn(interview);
        when(service.requestReschedule(4L, rescheduleRequestDTO)).thenReturn(interview);

        assertThat(controller.updateStatus(4L, statusDTO)).isSameAs(interview);
        assertThat(controller.confirm(4L)).isSameAs(interview);
        assertThat(controller.requestReschedule(4L, rescheduleRequestDTO)).isSameAs(interview);
    }

    @Test
    void countReadsRepository() {
        when(interviewRepository.count()).thenReturn(12L);

        assertThat(controller.getInterviewCount()).isEqualTo(12L);
        verify(interviewRepository).count();
    }

    private static Interview interview(Long userId) {
        Interview interview = new Interview();
        interview.setUserId(userId);
        return interview;
    }
}
