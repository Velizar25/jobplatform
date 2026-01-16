// src/main/java/com/example/jobplatform/service/CVService.java
package com.example.jobplatform.service;

import com.example.jobplatform.model.CV;
import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.CVRepository;
import com.example.jobplatform.repository.projection.CVSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CVService {

    private final CVRepository cvRepository;

    /** Качване/запис на CV с owner */
    public CV store(MultipartFile file, User owner) throws IOException {
        CV cv = new CV();

        String original = file.getOriginalFilename();
        cv.setFilename(original == null || original.isBlank() ? "cv" : original);

        String ct = file.getContentType();
        cv.setFileType(ct == null || ct.isBlank() ? "application/octet-stream" : ct);

        cv.setData(file.getBytes());
        cv.setOwner(owner);

        return cvRepository.save(cv);
    }

    /** Само метаданни за списъка My CVs за даден потребител */
    public List<CVSummary> findSummariesByOwner(String username) {
        return cvRepository.findByOwner_UsernameOrderByIdDesc(username);
    }

    /** ВАЖНО: data е LAZY => държим транзакция и го инициализираме тук */
    @Transactional(readOnly = true)
    public CV findById(Long id) {
        CV cv = cvRepository.findById(id).orElse(null);
        if (cv != null) {
            // force-init на byte[] data, за да не гърми в controller-a
            byte[] data = cv.getData();
            if (data != null) {
                int ignore = data.length;
            }
        }
        return cv;
    }

    public void deleteById(Long id) {
        cvRepository.deleteById(id);
    }
}