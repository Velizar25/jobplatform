package com.example.jobplatform.controller;

import com.example.jobplatform.model.Application;
import com.example.jobplatform.model.CV;
import com.example.jobplatform.model.Job;
import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.ApplicationRepository;
import com.example.jobplatform.repository.CVRepository;
import com.example.jobplatform.repository.JobRepository;
import com.example.jobplatform.repository.UserRepository;
import com.example.jobplatform.security.CustomOAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/jobs")
public class JobRestController {

    private final JobRepository jobRepository;
    private final CVRepository cvRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public JobRestController(JobRepository jobRepository,
                             CVRepository cvRepository,
                             ApplicationRepository applicationRepository,
                             UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.cvRepository = cvRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    private static final long MAX_SIZE = 20L * 1024 * 1024; // 20MB

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @GetMapping
    public List<JobResponse> listJobs() {
        return jobRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(this::toJobResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable Long id) {
        Job job = jobRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        return toJobResponse(job);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin")
    public ResponseEntity<JobResponse> createJob(@RequestBody JobRequest request) {
        Job job = new Job();

        job.setTitle(trim(request.title()));
        job.setCompany(trim(request.company()));
        job.setLocation(trim(request.location()));
        job.setEmploymentType(trim(request.employmentType()));
        job.setRequiredSkills(trim(request.requiredSkills()));
        job.setDescription(request.description() == null ? "" : request.description());

        Job savedJob = jobRepository.save(job);

        return ResponseEntity.status(HttpStatus.CREATED).body(toJobResponse(savedJob));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}")
    public JobResponse updateJob(@PathVariable Long id,
                                 @RequestBody JobRequest request) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        job.setTitle(trim(request.title()));
        job.setCompany(trim(request.company()));
        job.setLocation(trim(request.location()));
        job.setEmploymentType(trim(request.employmentType()));
        job.setRequiredSkills(trim(request.requiredSkills()));
        job.setDescription(request.description() == null ? "" : request.description());

        Job savedJob = jobRepository.save(job);

        return toJobResponse(savedJob);
    }

    @GetMapping("/{id}/apply-data")
    public ApplyDataResponse getApplyData(@PathVariable Long id,
                                          Authentication auth) {

        Job job = jobRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        User currentUser = getCurrentUser(auth);

        List<CvSummaryResponse> myCvs = cvRepository.findByOwner_Username(currentUser.getUsername())
                .stream()
                .map(this::toCvSummaryResponse)
                .toList();

        return new ApplyDataResponse(toJobResponse(job), myCvs);
    }

    @Transactional
    @PostMapping("/{id}/apply")
    public ResponseEntity<Map<String, String>> apply(@PathVariable Long id,
                                                     @RequestParam(value = "cvId", required = false) Long cvId,
                                                     @RequestParam(value = "cv", required = false) MultipartFile file,
                                                     Authentication auth) throws IOException {

        Job job = jobRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        User currentUser = getCurrentUser(auth);

        CV selectedCv;

        if (cvId != null) {
            selectedCv = cvRepository.findById(cvId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

            boolean isAdmin = auth.getAuthorities()
                    .stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            boolean isOwner = selectedCv.getOwner() != null
                    && selectedCv.getOwner().getId() != null
                    && selectedCv.getOwner().getId().equals(currentUser.getId());

            if (!isOwner && !isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to use this CV");
            }
        } else {
            if (file == null || file.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Please choose an existing CV or upload a new one"
                );
            }

            if (file.getSize() > MAX_SIZE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "File too large. Max 20 MB"
                );
            }

            String contentType = file.getContentType() == null ? "" : file.getContentType();

            if (!ALLOWED_TYPES.contains(contentType)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported file type. Allowed: PDF, DOC, DOCX"
                );
            }

            String originalName = file.getOriginalFilename();
            String safeName = originalName == null ? "cv" : originalName.replaceAll("[\\\\/]+", "_");

            CV newCv = new CV();
            newCv.setFilename(safeName);
            newCv.setFileType(contentType);
            newCv.setData(file.getBytes());
            newCv.setOwner(currentUser);

            selectedCv = cvRepository.save(newCv);
        }

        Application application = new Application();
        application.setJob(job);
        application.setCv(selectedCv);
        application.setEmail(currentUser.getEmail());
        application.setJobTitle(job.getTitle());
        application.setStatus("Submitted");
        application.setApplicant(currentUser);

        applicationRepository.save(application);

        return ResponseEntity.ok(Map.of("message", "Application submitted successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{id}/delete")
    public ResponseEntity<Map<String, String>> deleteJob(@PathVariable Long id) {
        jobRepository.softDeleteById(id);
        return ResponseEntity.ok(Map.of("message", "Job moved to recycle bin"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/clear")
    @Transactional
    public ResponseEntity<Map<String, String>> clearJobs() {
        jobRepository.softDeleteAll();
        return ResponseEntity.ok(Map.of("message", "All jobs moved to recycle bin"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/restore-all")
    @Transactional
    public ResponseEntity<Map<String, String>> restoreAllJobs() {
        jobRepository.restoreAll();
        return ResponseEntity.ok(Map.of("message", "All jobs restored"));
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String username = resolveUsername(auth);

        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private String resolveUsername(Authentication auth) {
        Object principal = auth.getPrincipal();

        if (principal instanceof CustomOAuth2User customOAuth2User) {
            return customOAuth2User.getUsername();
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");

            if (email != null && !email.isBlank()) {
                return email;
            }
        }

        return auth.getName();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private JobResponse toJobResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getRequiredSkills(),
                job.getDescription()
        );
    }

    private CvSummaryResponse toCvSummaryResponse(CV cv) {
        return new CvSummaryResponse(
                cv.getId(),
                cv.getFilename(),
                cv.getFileType()
        );
    }

    public record JobRequest(
            String title,
            String company,
            String location,
            String employmentType,
            String requiredSkills,
            String description
    ) {
    }

    public record JobResponse(
            Long id,
            String title,
            String company,
            String location,
            String employmentType,
            String requiredSkills,
            String description
    ) {
    }

    public record CvSummaryResponse(
            Long id,
            String filename,
            String fileType
    ) {
    }

    public record ApplyDataResponse(
            JobResponse job,
            List<CvSummaryResponse> myCvs
    ) {
    }
}