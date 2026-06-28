package com.example.jobplatform.service;

import com.example.jobplatform.model.CV;
import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.CVRepository;
import com.example.jobplatform.repository.projection.CVSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class CVService {

    private final CVRepository cvRepository;

    public CVService(CVRepository cvRepository) {
        this.cvRepository = cvRepository;
    }

    /** Качване/запис на CV с owner */
    public CV store(MultipartFile file, User owner) throws IOException {
        CV cv = new CV();

        String original = file.getOriginalFilename();
        cv.setFilename(original == null || original.isBlank() ? "cv" : original.replaceAll("[\\\\/]+", "_"));

        String contentType = file.getContentType();
        cv.setFileType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType);

        cv.setData(file.getBytes());
        cv.setOwner(owner);

        return cvRepository.save(cv);
    }

    /** Само метаданни за списъка My CVs за даден потребител */
    public List<CVSummary> findSummariesByOwner(String username) {
        return cvRepository.findByOwner_UsernameOrderByIdDesc(username);
    }

    /** data е LAZY => държим транзакция и го инициализираме тук */
    @Transactional(readOnly = true)
    public CV findById(Long id) {
        CV cv = cvRepository.findById(id).orElse(null);

        if (cv != null) {
            byte[] data = cv.getData();

            if (data != null) {
                int ignored = data.length;
            }
        }

        return cv;
    }

    public void deleteById(Long id) {
        cvRepository.deleteById(id);
    }
}