package com.example.jobplatform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "applications")
public class Application {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobTitle;
    private String email;
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id")
    private CV cv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    // Кой подава кандидатурата
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id")
    private User applicant;

    public Long getId() { return id; }
    public String getJobTitle() { return jobTitle; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public CV getCv() { return cv; }
    public Job getJob() { return job; }
    public User getApplicant() { return applicant; }

    public void setId(Long id) { this.id = id; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public void setEmail(String email) { this.email = email; }
    public void setStatus(String status) { this.status = status; }
    public void setCv(CV cv) { this.cv = cv; }
    public void setJob(Job job) { this.job = job; }
    public void setApplicant(User applicant) { this.applicant = applicant; }
}