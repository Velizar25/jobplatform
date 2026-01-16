package com.example.jobplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.event.EventListener;
import org.springframework.boot.web.context.WebServerInitializedEvent;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
@EntityScan("com.example.jobplatform.model")
@EnableJpaRepositories("com.example.jobplatform.repository")
public class JobPlatformApp {

    public static void main(String[] args) {
        SpringApplication.run(JobPlatformApp.class, args);
    }

    @EventListener
    public void onServerReady(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        String url = "http://localhost:" + port + "/home";
        System.out.println("App started at: " + url);
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.out.println("Open manually: " + url);
            }
        } catch (Exception e) {
            System.out.println("Could not open browser: " + e.getMessage() + " -> " + url);
        }
    }
}