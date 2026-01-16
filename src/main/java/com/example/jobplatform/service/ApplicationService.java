package com.example.jobplatform.service;

import com.example.jobplatform.model.Application;
import com.example.jobplatform.model.CV;
import com.example.jobplatform.model.Job;
import com.example.jobplatform.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    /** Създава кандидатура за дадена обява. */
    public Application submit(Job job, String candidateEmail, @Nullable CV cv) {
        Application app = new Application();
        app.setJob(job);                          // важно: иначе job_id ще е null
        app.setEmail(candidateEmail);             // вместо setApplicantEmail(...)
        app.setJobTitle(job.getTitle());
        app.setStatus("Submitted");
        if (cv != null) app.setCv(cv);
        return applicationRepository.save(app);
    }

    public List<Application> findAll() {
        return applicationRepository.findAll();
    }
}