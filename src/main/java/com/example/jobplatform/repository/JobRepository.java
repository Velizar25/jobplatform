package com.example.jobplatform.repository;

import com.example.jobplatform.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByDeletedAtIsNull();

    Optional<Job> findByIdAndDeletedAtIsNull(Long id);

    @Modifying
    @Transactional
    @Query("update Job j set j.deletedAt = CURRENT_TIMESTAMP where j.id = :id")
    void softDeleteById(Long id);

    @Modifying
    @Transactional
    @Query("update Job j set j.deletedAt = CURRENT_TIMESTAMP where j.deletedAt is null")
    void softDeleteAll();

    @Modifying
    @Transactional
    @Query("update Job j set j.deletedAt = null where j.deletedAt is not null")
    void restoreAll();
}