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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final CVRepository cvRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    private static final long MAX_SIZE = 20L * 1024 * 1024; // 20MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @GetMapping("/jobs")
    public String listJobs(Model model) {
        model.addAttribute("jobs", jobRepository.findAllByDeletedAtIsNull());
        return "jobs";
    }

    // -------------------------
    // CREATE JOB (ADMIN)
    // -------------------------
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/jobs/new")
    public String newJob(Model model) {
        model.addAttribute("job", new Job());
        return "new-job";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/jobs")
    public String createJob(@ModelAttribute Job job, RedirectAttributes ra) {
        // минимална нормализация
        job.setTitle(trim(job.getTitle()));
        job.setCompany(trim(job.getCompany()));
        job.setLocation(trim(job.getLocation()));
        job.setEmploymentType(trim(job.getEmploymentType()));
        job.setDescription(job.getDescription() == null ? "" : job.getDescription());

        jobRepository.save(job);
        ra.addFlashAttribute("message", "Job posted.");
        return "redirect:/jobs";
    }

    // -------------------------
    // EDIT JOB (ADMIN)
    // -------------------------
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/jobs/{id}/edit")
    public String editJobForm(@PathVariable Long id, Model model) {
        Job job = jobRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        model.addAttribute("job", job);
        return "edit-job";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/jobs/{id}/edit")
    public String editJobSave(@PathVariable Long id,
                              @ModelAttribute("job") Job form,
                              RedirectAttributes ra) {

        Job job = jobRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        // ✅ ТОВА ти липсваше:
        job.setTitle(trim(form.getTitle()));
        job.setCompany(trim(form.getCompany()));
        job.setLocation(trim(form.getLocation()));
        job.setEmploymentType(trim(form.getEmploymentType()));
        job.setDescription(form.getDescription() == null ? "" : form.getDescription());

        jobRepository.save(job);
        ra.addFlashAttribute("message", "Job updated.");
        return "redirect:/jobs";
    }

    // -------------------------
    // APPLY PAGE (job + dropdown с моите CV-та)
    // -------------------------
    @GetMapping("/jobs/{id}/apply")
    public String applyForm(@PathVariable Long id, Model model, Authentication auth) {
        Job job = jobRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (auth == null) return "redirect:/login";

        String username = resolveUsername(auth);

        User me = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        List<CV> myCvs = cvRepository.findByOwner_Username(me.getUsername());

        model.addAttribute("job", job);
        model.addAttribute("myCvs", myCvs);
        return "apply-job";
    }

    // -------------------------
    // APPLY ACTION (select existing CV OR upload new)
    // -------------------------
    @Transactional
    @PostMapping("/jobs/{id}/apply")
    public String apply(@PathVariable Long id,
                        @RequestParam(value = "cvId", required = false) Long cvId,
                        @RequestParam(value = "cv", required = false) MultipartFile file,
                        Authentication auth,
                        RedirectAttributes ra) throws IOException {

        Job job = jobRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (auth == null) return "redirect:/login";

        String username = resolveUsername(auth);

        User currentUser = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // 1) Избрано CV от dropdown
        if (cvId != null) {
            CV existing = cvRepository.findById(cvId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            boolean isOwner = existing.getOwner() != null
                    && existing.getOwner().getId() != null
                    && existing.getOwner().getId().equals(currentUser.getId());

            if (!isOwner && !isAdmin) {
                ra.addFlashAttribute("message", "Not allowed to use this CV.");
                return "redirect:/jobs/" + id + "/apply";
            }

            Application app = new Application();
            app.setJob(job);
            app.setCv(existing);
            app.setEmail(currentUser.getEmail());
            app.setJobTitle(job.getTitle());
            app.setStatus("Submitted");
            app.setApplicant(currentUser);
            applicationRepository.save(app);

            ra.addFlashAttribute("message", "Application submitted (using existing CV).");
            return "redirect:/applications";
        }

        // 2) Ако няма избрано CV -> трябва upload
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("message", "Please choose an existing CV or upload a new one.");
            return "redirect:/jobs/" + id + "/apply";
        }

        if (file.getSize() > MAX_SIZE) {
            ra.addFlashAttribute("message", "File too large. Max 20 MB.");
            return "redirect:/jobs/" + id + "/apply";
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            ra.addFlashAttribute("message", "Unsupported file type. Allowed: PDF, DOC, DOCX.");
            return "redirect:/jobs/" + id + "/apply";
        }

        String originalName = file.getOriginalFilename();
        String safeName = originalName == null ? "cv" : originalName.replaceAll("[\\\\/]+", "_");

        CV cv = new CV();
        cv.setFilename(safeName);
        cv.setFileType(contentType);
        cv.setData(file.getBytes());
        cv.setOwner(currentUser);
        cvRepository.save(cv);

        Application app = new Application();
        app.setJob(job);
        app.setCv(cv);
        app.setEmail(currentUser.getEmail());
        app.setJobTitle(job.getTitle());
        app.setStatus("Submitted");
        app.setApplicant(currentUser);
        applicationRepository.save(app);

        ra.addFlashAttribute("message", "Application submitted.");
        return "redirect:/applications";
    }

    private String resolveUsername(Authentication auth) {
        Object p = auth.getPrincipal();

        if (p instanceof CustomOAuth2User cu) return cu.getUsername();
        if (p instanceof UserDetails ud) return ud.getUsername();

        if (p instanceof OAuth2User ou) {
            String email = ou.getAttribute("email");
            if (email != null && !email.isBlank()) return email;
        }

        return auth.getName();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    // -------------------------
    // DELETE / CLEAR / RESTORE (ADMIN)
    // -------------------------
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/jobs/{id}/delete")
    public String deleteJob(@PathVariable Long id, RedirectAttributes ra) {
        jobRepository.softDeleteById(id);
        ra.addFlashAttribute("message", "Job moved to recycle bin.");
        return "redirect:/jobs";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/jobs/clear")
    @Transactional
    public String clearJobs(RedirectAttributes ra) {
        jobRepository.softDeleteAll();
        ra.addFlashAttribute("message", "All jobs moved to recycle bin.");
        return "redirect:/jobs";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/jobs/restore-all")
    @Transactional
    public String restoreAllJobs(RedirectAttributes ra) {
        jobRepository.restoreAll();
        ra.addFlashAttribute("message", "All jobs restored.");
        return "redirect:/jobs";
    }
}