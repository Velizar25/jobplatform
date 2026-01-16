package com.example.jobplatform.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    // ---------- Public API ----------
    public String reply(String message) {
        String q = normalize(message);

        if (q.isBlank()) {
            return hi() + "\n\nAsk me something like: “how to apply”, “CV upload”, “edit job”, “salary tips”.";
        }

        // 1) Greetings
        if (matchesAny(q, GREETINGS)) {
            return hi();
        }

        // 2) Help / What can you do
        if (matchesAny(q, HELP)) {
            return help();
        }

        // 3) Register / Login
        if (matchesAny(q, REGISTER)) {
            return """
                Register / Sign up:
                1) Go to Register
                2) Choose a unique username + email
                3) Password must be at least 4 characters
                4) If you get “already taken” → use another username/email

                Tip: If an email is “taken (maybe created via Google login)”, try logging in with Google using that email.
                """;
        }

        if (matchesAny(q, LOGIN)) {
            return """
                Login help:
                • If you registered with email/password → use those credentials
                • If your account was created via Google → use “Login with Google”
                • If you can’t log in → check if caps lock is on and try again
                """;
        }

        // 4) CV upload / file types / size
        if (matchesAny(q, CV_UPLOAD)) {
            return """
                Upload CV:
                1) Go to “Upload CV”
                2) Choose a file (PDF / DOC / DOCX)
                3) Submit

                You can also apply to a job by selecting an existing CV from the dropdown.
                """;
        }

        if (matchesAny(q, FILE_TYPES)) {
            return """
                Supported CV file types:
                • PDF (.pdf)
                • Word (.doc)
                • Word (.docx)

                Max size: 20 MB.
                """;
        }

        // 5) Apply to job
        if (matchesAny(q, APPLY)) {
            return """
                How to apply:
                1) Open Jobs
                2) Click “Apply”
                3) Choose an existing CV (dropdown) OR upload a new one
                4) Submit

                If you see “Not allowed to use this CV” → you selected a CV that isn’t yours.
                """;
        }

        // 6) Admin: post/edit/delete job
        if (matchesAny(q, POST_JOB)) {
            return """
                Admin: Post a new job
                1) Jobs → “Post new job”
                2) Fill Title / Company / Location / Employment type
                3) Paste Description (you can use sections like Responsibilities, Requirements, Offer)
                4) Publish
                """;
        }

        if (matchesAny(q, EDIT_JOB)) {
            return """
                Admin: Edit job
                1) Jobs → click “Edit”
                2) Update fields
                3) Save changes

                If changes don’t show: refresh the Jobs page and make sure DB columns exist (company/employmentType).
                """;
        }

        if (matchesAny(q, DELETE_JOB)) {
            return """
                Admin: Delete job (soft delete)
                • Click “Delete” → job goes to recycle bin
                • “Restore all” brings back all jobs
                • “Clear all jobs” moves all to recycle bin
                """;
        }

        // 7) Profile / password
        if (matchesAny(q, PROFILE)) {
            return """
                Profile:
                • You can change email anytime
                • Password change is available only for non-Google accounts
                • If logged in via Google → password cannot be changed
                """;
        }

        // 8) Interview / CV / salary tips (more “complex” answers)
        if (matchesAny(q, INTERVIEW)) {
            return """
                Interview tips:
                • Prepare 2–3 projects/stories (STAR method)
                • Review the job requirements and map your experience to them
                • Have 5–7 common questions ready (strengths, weaknesses, teamwork, conflict)
                • Ask questions back: team size, stack, onboarding, expectations for 90 days
                """;
        }

        if (matchesAny(q, SALARY)) {
            return """
                Salary negotiation:
                1) Ask for range first
                2) Give a range, not a single number (based on skills + market)
                3) Consider total compensation: bonus, benefits, hybrid/remote, learning budget
                4) If junior → focus on growth plan + review after 3–6 months
                """;
        }

        if (matchesAny(q, CV_ADVICE)) {
            return """
                CV improvement checklist:
                • 1 page (junior) / up to 2 pages (mid/senior)
                • Strong summary: role + stack + impact
                • Projects: what you built + tech + measurable results
                • Skills: group by Frontend/Backend/DB/Tools
                • Avoid long paragraphs → use bullets
                """;
        }

        // 9) Fallback (better than current)
        return fallback(message);
    }

    // ---------- Responses ----------
    private String hi() {
        return """
            Hi! 👋 I’m your JobPlatform assistant.
            Ask me about: jobs, applying, CV upload, login/register, admin job posting, profile, interview & salary tips.
            Type “help” to see examples.
            """;
    }

    private String help() {
        return """
            Examples you can ask:
            • “how to apply?”
            • “upload cv”
            • “supported file types”
            • “post new job” (admin)
            • “edit job” (admin)
            • “profile password”
            • “interview tips”
            • “salary negotiation”
            • “cv advice”
            """;
    }

    private String fallback(String original) {
        String o = original == null ? "" : original.trim();
        return "I didn’t fully understand: “" + o + "”.\n\n"
                + "Try: “help”, “how to apply”, “upload cv”, “edit job”, “interview tips”.";
    }

    // ---------- Matching ----------
    private static final List<Pattern> GREETINGS = List.of(
            p("\\bhi\\b"), p("\\bhello\\b"), p("\\bhey\\b"),
            p("здрасти"), p("здравей"), p("добър ден"), p("добър вечер")
    );

    private static final List<Pattern> HELP = List.of(
            p("\\bhelp\\b"), p("какво можеш"), p("какво правиш"), p("как да питам")
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
            p("profile"), p("password"), p("email change"), p("профил"), p("смяна\\s*парола")
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
        for (Pattern pat : patterns) {
            if (pat.matcher(q).find()) return true;
        }
        return false;
    }

    private static Pattern p(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase(Locale.ROOT);
        t = t.replaceAll("\\s+", " ");
        return t;
    }
}