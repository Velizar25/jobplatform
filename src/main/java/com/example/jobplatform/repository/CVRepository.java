package com.example.jobplatform.repository;

import com.example.jobplatform.model.CV;
import com.example.jobplatform.repository.projection.CVSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CVRepository extends JpaRepository<CV, Long> {


    List<CV> findByOwner_Username(String username);

    // Само метаданни за списъка My CV
    List<CVSummary> findByOwner_UsernameOrderByIdDesc(String username);
}