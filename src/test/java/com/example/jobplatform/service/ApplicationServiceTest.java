package com.example.jobplatform.service;

import com.example.jobplatform.model.Application;
import com.example.jobplatform.model.CV;
import com.example.jobplatform.model.Job;
import com.example.jobplatform.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void submit_withCv_savesApplicationWithAllFields() {
        // Arrange
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Junior Java Developer");

        CV cv = new CV();
        cv.setId(10L);

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        Application result = applicationService.submit(
                job,
                "candidate@test.com",
                cv
        );

        // Assert
        assertThat(result.getJob()).isEqualTo(job);
        assertThat(result.getEmail()).isEqualTo("candidate@test.com");
        assertThat(result.getJobTitle()).isEqualTo("Junior Java Developer");
        assertThat(result.getStatus()).isEqualTo("Submitted");
        assertThat(result.getCv()).isEqualTo(cv);

        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void submit_withoutCv_savesApplicationWithoutCv() {
        // Arrange
        Job job = new Job();
        job.setTitle("Trainee JS Developer");

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        Application result = applicationService.submit(
                job,
                "no.cv@test.com",
                null
        );

        // Assert
        assertThat(result.getJob()).isEqualTo(job);
        assertThat(result.getEmail()).isEqualTo("no.cv@test.com");
        assertThat(result.getJobTitle()).isEqualTo("Trainee JS Developer");
        assertThat(result.getStatus()).isEqualTo("Submitted");
        assertThat(result.getCv()).isNull();

        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void findAll_returnsRepositoryResult() {
        // Arrange
        Application a1 = new Application();
        a1.setId(1L);

        Application a2 = new Application();
        a2.setId(2L);

        when(applicationRepository.findAll())
                .thenReturn(List.of(a1, a2));

        // Act
        List<Application> result = applicationService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);

        verify(applicationRepository).findAll();
    }
}