package com.example.jobplatform.service;

import com.example.jobplatform.model.CV;
import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.CVRepository;
import com.example.jobplatform.repository.projection.CVSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CVServiceTest {

    @Mock private CVRepository cvRepository;

    @InjectMocks private CVService cvService;

    @Test
    void store_savesCvWithOwnerAndBytes() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("cv.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn("HELLO".getBytes());

        User owner = new User();
        owner.setId(10L);
        owner.setUsername("alice");

        // repo.save(...) -> връща запазения обект
        when(cvRepository.save(any(CV.class))).thenAnswer(inv -> inv.getArgument(0));

        CV saved = cvService.store(file, owner);

        ArgumentCaptor<CV> captor = ArgumentCaptor.forClass(CV.class);
        verify(cvRepository).save(captor.capture());

        CV toSave = captor.getValue();
        assertThat(toSave.getFilename()).isEqualTo("cv.pdf");
        assertThat(toSave.getFileType()).isEqualTo("application/pdf");
        assertThat(toSave.getData()).isEqualTo("HELLO".getBytes());
        assertThat(toSave.getOwner()).isSameAs(owner);

        // и върнатият резултат е същия (заради thenAnswer)
        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getFilename()).isEqualTo("cv.pdf");
    }

    @Test
    void findSummariesByOwner_delegatesToRepository() {
        String username = "alice";

        @SuppressWarnings("unchecked")
        List<CVSummary> mockList = (List<CVSummary>) (List<?>) List.of(mock(CVSummary.class), mock(CVSummary.class));

        when(cvRepository.findByOwner_UsernameOrderByIdDesc(username)).thenReturn(mockList);

        List<CVSummary> result = cvService.findSummariesByOwner(username);

        assertThat(result).hasSize(2);
        verify(cvRepository).findByOwner_UsernameOrderByIdDesc(username);
    }

    @Test
    void findById_returnsCvOrNull() {
        CV cv = new CV();
        cv.setId(5L);

        when(cvRepository.findById(5L)).thenReturn(Optional.of(cv));
        when(cvRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(cvService.findById(5L)).isNotNull();
        assertThat(cvService.findById(5L).getId()).isEqualTo(5L);

        assertThat(cvService.findById(99L)).isNull();
    }

    @Test
    void deleteById_callsRepository() {
        cvService.deleteById(7L);
        verify(cvRepository).deleteById(7L);
    }
}