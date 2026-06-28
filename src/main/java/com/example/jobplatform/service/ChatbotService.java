package com.example.jobplatform.service;

import com.example.jobplatform.model.Job;
import com.example.jobplatform.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    private final JobRepository jobRepository;

    public ChatbotService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // ---------- Public API ----------
    public String reply(String message) {
        String q = normalize(message);

        if (q.isBlank()) {
            return hi() + "\n\nAsk me something like: “how to apply”, “CV upload”, “recommend jobs”, “Java jobs”.";
        }

        if (matchesAny(q, GREETINGS)) {
            return hi();
        }

        if (matchesAny(q, HELP)) {
            return help();
        }

        if (matchesAny(q, JOB_RECOMMENDATION)) {
            return recommendJobs(q);
        }

        if (matchesAny(q, REGISTER)) {
            return """
                    Register / Sign up:
                    1) Go to Register
                    2) Choose a unique username and email
                    3) Enter a secure password
                    4) Submit the form

                    You can also use Google Login if you prefer faster authentication.
                    """;
        }

        if (matchesAny(q, LOGIN)) {
            return """
                    Login help:
                    • If you registered with email and password, use your credentials.
                    • If your account was created with Google, use “Login with Google”.
                    • If you cannot log in, check your email, password and Caps Lock.
                    """;
        }

        if (matchesAny(q, CV_UPLOAD)) {
            return """
                    Upload CV:
                    1) Open the CV section
                    2) Choose a PDF, DOC or DOCX file
                    3) Upload the file
                    4) Use the uploaded CV when applying for a job

                    The system supports multiple CV files per user.
                    """;
        }

        if (matchesAny(q, FILE_TYPES)) {
            return """
                    Supported CV file types:
                    • PDF (.pdf)
                    • Word (.doc)
                    • Word (.docx)

                    Maximum file size: 20 MB.
                    """;
        }

        if (matchesAny(q, APPLY)) {
            return """
                    How to apply:
                    1) Open the Jobs page
                    2) Choose a suitable job offer
                    3) Click Apply
                    4) Select an existing CV or upload a new one
                    5) Submit your application

                    You can later review your submitted applications.
                    """;
        }

        if (matchesAny(q, POST_JOB)) {
            return """
                    Admin: Post a new job
                    1) Open the admin job section
                    2) Enter title, company, location and employment type
                    3) Add required skills
                    4) Add a detailed job description
                    5) Publish the job offer

                    Only users with ADMIN role can create job offers.
                    """;
        }

        if (matchesAny(q, EDIT_JOB)) {
            return """
                    Admin: Edit job
                    1) Open the selected job offer
                    2) Update the required fields
                    3) Save the changes

                    Only administrators can edit job offers.
                    """;
        }

        if (matchesAny(q, DELETE_JOB)) {
            return """
                    Admin: Delete job
                    The platform uses soft delete:
                    • Deleted jobs are moved to a recycle state
                    • They are hidden from normal users
                    • Administrators can restore them later
                    """;
        }

        if (matchesAny(q, PROFILE)) {
            return """
                    Profile:
                    • You can update your email address
                    • You can add your full name, skills, preferred location and preferred job type
                    • Password change is available only for standard accounts
                    • Google OAuth2 users cannot change password inside the platform
                    """;
        }

        if (matchesAny(q, INTERVIEW)) {
            return """
                    Interview tips:
                    • Read the job description carefully
                    • Prepare examples from your projects
                    • Explain what technologies you used and why
                    • Be ready to talk about teamwork and problem solving
                    • Prepare questions about the company, team and project
                    """;
        }

        if (matchesAny(q, SALARY)) {
            return """
                    Salary negotiation:
                    • Research the usual salary range for the position
                    • Give a range instead of one fixed number
                    • Consider benefits, remote work, learning budget and bonuses
                    • For junior roles, focus also on growth opportunities
                    """;
        }

        if (matchesAny(q, CV_ADVICE)) {
            return """
                    CV improvement checklist:
                    • Keep the CV clear and structured
                    • Add contact information and technical skills
                    • Describe projects with technologies and results
                    • Use bullet points instead of long paragraphs
                    • Adapt the CV to the job offer
                    """;
        }

        return fallback(message);
    }

    // ---------- Database-based job recommendation ----------
    private String recommendJobs(String query) {
        List<Job> jobs = jobRepository.findAllByDeletedAtIsNull();

        if (jobs.isEmpty()) {
            return "There are currently no active job offers in the system.";
        }

        List<Job> matchedJobs = jobs.stream()
                .filter(job -> jobMatchesQuery(job, query))
                .limit(5)
                .toList();

        if (matchedJobs.isEmpty()) {
            matchedJobs = jobs.stream()
                    .limit(5)
                    .toList();
        }

        StringBuilder response = new StringBuilder();

        response.append("Here are some job offers that may be suitable for you:\n\n");

        for (Job job : matchedJobs) {
            response.append("• ")
                    .append(nullToEmpty(job.getTitle()));

            if (job.getCompany() != null && !job.getCompany().isBlank()) {
                response.append(" at ").append(job.getCompany());
            }

            if (job.getLocation() != null && !job.getLocation().isBlank()) {
                response.append(" — ").append(job.getLocation());
            }

            if (job.getEmploymentType() != null && !job.getEmploymentType().isBlank()) {
                response.append(" (").append(job.getEmploymentType()).append(")");
            }

            if (job.getRequiredSkills() != null && !job.getRequiredSkills().isBlank()) {
                response.append("\n  Required skills: ").append(job.getRequiredSkills());
            }

            response.append("\n");
        }

        response.append("\nTip: Open the Jobs page to view details and apply with your CV.");

        return response.toString();
    }

    private boolean jobMatchesQuery(Job job, String query) {
        String text = normalize(
                nullToEmpty(job.getTitle()) + " " +
                        nullToEmpty(job.getCompany()) + " " +
                        nullToEmpty(job.getLocation()) + " " +
                        nullToEmpty(job.getEmploymentType()) + " " +
                        nullToEmpty(job.getRequiredSkills()) + " " +
                        nullToEmpty(job.getDescription())
        );

        String[] keywords = query.split(" ");

        for (String keyword : keywords) {
            if (keyword.length() >= 3 && text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    // ---------- Responses ----------
    private String hi() {
        return """
                Hi! 👋 I’m your JobPlatform assistant.
                I can help you with job offers, applications, CV upload, login, profile settings, interview preparation and salary tips.
                Type “help” to see examples.
                """;
    }

    private String help() {
        return """
                Examples you can ask:
                • “how to apply?”
                • “upload CV”
                • “supported file types”
                • “recommend jobs”
                • “Java jobs”
                • “frontend jobs”
                • “React jobs”
                • “Spring Boot jobs”
                • “remote jobs”
                • “post new job”
                • “profile password”
                • “interview tips”
                • “salary negotiation”
                • “CV advice”
                """;
    }

    private String fallback(String original) {
        String text = original == null ? "" : original.trim();

        return "I didn’t fully understand: “" + text + "”.\n\n"
                + "Try asking: “help”, “recommend jobs”, “Java jobs”, “how to apply”, “upload CV”, “interview tips”.";
    }

    // ---------- Matching patterns ----------
    private static final List<Pattern> GREETINGS = List.of(
            p("\\bhi\\b"), p("\\bhello\\b"), p("\\bhey\\b"),
            p("здрасти"), p("здравей"), p("добър ден"), p("добър вечер")
    );

    private static final List<Pattern> HELP = List.of(
            p("\\bhelp\\b"), p("какво можеш"), p("какво правиш"), p("как да питам")
    );

    private static final List<Pattern> JOB_RECOMMENDATION = List.of(
            p("recommend"), p("suggest"), p("suitable"), p("job offers"),
            p("\\bjobs?\\b"), p("позиции"), p("работа"), p("обяви"),
            p("препоръч"), p("подходящ"),
            p("java"), p("frontend"), p("backend"), p("fullstack"), p("full-stack"),
            p("react"), p("angular"), p("vue"), p("spring"), p("spring boot"),
            p("postgresql"), p("sql"), p("developer"), p("remote"),
            p("софия"), p("пловдив"), p("варна"), p("бургас")
    );

    private static final List<Pattern> REGISTER = List.of(
            p("register"), p("sign\\s*up"), p("регист"), p("акаунт"), p("account create")
    );

    private static final List<Pattern> LOGIN = List.of(
            p("\\blogin\\b"), p("log\\s*in"), p("вход"), p("не мога да вляза")
    );

    private static final List<Pattern> CV_UPLOAD = List.of(
            p("upload\\s*cv"), p("кач(и|вам)\\s*cv"), p("кач(и|вам)\\s*автобиограф"),
            p("my\\s*cvs"), p("cvs?")
    );

    private static final List<Pattern> FILE_TYPES = List.of(
            p("file\\s*types"), p("supported"), p("pdf"), p("docx?"), p("формат"), p("тип файл")
    );

    private static final List<Pattern> APPLY = List.of(
            p("\\bapply\\b"), p("кандидат"), p("application"), p("submit application")
    );

    private static final List<Pattern> POST_JOB = List.of(
            p("post\\s*(new\\s*)?job"), p("publish\\s*job"), p("обява"), p("пусн(а|и)\\s*обява")
    );

    private static final List<Pattern> EDIT_JOB = List.of(
            p("edit\\s*job"), p("update\\s*job"), p("редак"), p("промени\\s*обява")
    );

    private static final List<Pattern> DELETE_JOB = List.of(
            p("delete\\s*job"), p("remove\\s*job"), p("изтрий\\s*обява"), p("кош")
    );

    private static final List<Pattern> PROFILE = List.of(
            p("profile"), p("password"), p("email change"), p("профил"), p("смяна\\s*парола"),
            p("skills"), p("умения"), p("предпочитания")
    );

    private static final List<Pattern> INTERVIEW = List.of(
            p("interview"), p("подготовка"), p("техническо"), p("hr"), p("въпроси\\s*за\\s*интервю")
    );

    private static final List<Pattern> SALARY = List.of(
            p("salary"), p("заплат"), p("money"), p("оферта"), p("negotiat")
    );

    private static final List<Pattern> CV_ADVICE = List.of(
            p("\\bcv\\b.*(tips|advice)"), p("cv\\s*advice"), p("подобри\\s*cv"), p("автобиограф")
    );

    private static boolean matchesAny(String q, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(q).find()) {
                return true;
            }
        }

        return false;
    }

    private static Pattern p(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}