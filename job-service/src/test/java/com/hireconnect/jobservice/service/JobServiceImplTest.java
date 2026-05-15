package com.hireconnect.jobservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.hireconnect.jobservice.dto.JobDTO;
import com.hireconnect.jobservice.entity.Job;
import com.hireconnect.jobservice.entity.JobView;
import com.hireconnect.jobservice.exception.ResourceNotFoundException;
import com.hireconnect.jobservice.exception.UnauthorizedActionException;
import com.hireconnect.jobservice.repository.JobRepository;
import com.hireconnect.jobservice.repository.JobViewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository repository;

    @Mock
    private JobViewRepository viewRepository;

    @InjectMocks
    private JobServiceImpl service;

    @Test
    void createJobAllowsEmployersAndDefaultsStatus() {
        JobDTO dto = jobDto();
        dto.setStatus(" ");
        when(repository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = service.createJob(dto);

        assertThat(result.getEmployerId()).isEqualTo(4L);
        assertThat(result.getTitle()).isEqualTo("Java Developer");
        assertThat(result.getCompanyName()).isEqualTo("HireConnect");
        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getViewCount()).isZero();
    }

    @Test
    void createJobRejectsNonEmployerRole() {
        JobDTO dto = jobDto();
        dto.setRole("CANDIDATE");

        assertThatThrownBy(() -> service.createJob(dto))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void getJobByIdTracksFirstViewForUser() {
        Job job = job();
        job.setViewCount(2L);
        when(repository.findById(9L)).thenReturn(Optional.of(job));
        when(viewRepository.existsByJobIdAndUserId(9L, 21L)).thenReturn(false);
        when(repository.save(job)).thenReturn(job);

        Job result = service.getJobById(9L, 21L);

        assertThat(result.getViewCount()).isEqualTo(3L);
        verify(viewRepository).save(any(JobView.class));
    }

    @Test
    void getJobByIdDoesNotTrackAnonymousOrRepeatedView() {
        Job job = job();
        when(repository.findById(9L)).thenReturn(Optional.of(job));

        assertThat(service.getJobById(9L, null)).isSameAs(job);
        verify(viewRepository, never()).save(any());
        verify(repository, never()).save(any(Job.class));
    }

    @Test
    void updateJobRequiresSameEmployer() {
        Job job = job();
        when(repository.findById(9L)).thenReturn(Optional.of(job));
        JobDTO dto = jobDto();
        dto.setEmployerId(99L);

        assertThatThrownBy(() -> service.updateJob(9L, dto))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void updateJobSavesChangedFields() {
        Job job = job();
        when(repository.findById(9L)).thenReturn(Optional.of(job));
        when(repository.save(job)).thenReturn(job);
        JobDTO dto = jobDto();
        dto.setTitle("Senior Java Developer");
        dto.setStatus("CLOSED");

        Job result = service.updateJob(9L, dto);

        assertThat(result.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(result.getStatus()).isEqualTo("CLOSED");
        verify(repository).save(job);
    }

    @Test
    void deleteJobRequiresExistingJobAndOwner() {
        Job job = job();
        when(repository.findById(9L)).thenReturn(Optional.of(job));

        service.deleteJob(9L, 4L);

        verify(repository).delete(job);
    }

    @Test
    void readAndSearchMethodsDelegateToRepository() {
        Job job = job();
        when(repository.findAll()).thenReturn(List.of(job));
        when(repository.findByEmployerId(4L)).thenReturn(List.of(job));
        when(repository.searchJobs("java", null, "IT", null, "OPEN")).thenReturn(List.of(job));

        assertThat(service.getAllJobs()).containsExactly(job);
        assertThat(service.getJobsByEmployer(4L)).containsExactly(job);
        assertThat(service.searchJobs("java", " ", "IT", "", "OPEN")).containsExactly(job);
    }

    @Test
    void missingJobThrowsResourceNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJobById(404L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private JobDTO jobDto() {
        JobDTO dto = new JobDTO();
        dto.setEmployerId(4L);
        dto.setRole("EMPLOYER");
        dto.setTitle("Java Developer");
        dto.setCompanyName("HireConnect");
        dto.setDescription("Build services");
        dto.setLocation("Remote");
        dto.setSalary("100000");
        dto.setCategory("IT");
        dto.setType("FULL_TIME");
        dto.setSkills("Java, Spring");
        dto.setExperienceRequired("3 years");
        dto.setStatus("OPEN");
        return dto;
    }

    private Job job() {
        Job job = new Job(4L, "Java Developer", "Build services", "Remote", "100000");
        job.setCompanyName("HireConnect");
        job.setCategory("IT");
        job.setType("FULL_TIME");
        return job;
    }
}
