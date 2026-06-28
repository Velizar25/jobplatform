package com.example.jobplatform.repository;

import com.example.jobplatform.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {


    List<Application> findByApplicant_UsernameOrderByIdDesc(String username);


    List<Application> findAllByOrderByIdDesc();


    List<Application> findByCv_Owner_Username(String username);

    List<Application> findByCv_Id(Long cvId);
    List<Application> findByJob_Id(Long jobId);

    boolean existsByCv_Id(Long cvId);
    long countByCv_Id(Long cvId);

    @Modifying @Transactional
    long deleteByJob_Id(Long jobId);

    @Modifying @Transactional
    long deleteByCv_Id(Long cvId);
}