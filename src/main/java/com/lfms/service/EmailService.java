package com.lfms.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String API_KEY = System.getenv("RESEND_API_KEY");

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void sendEmail(String toEmail, String subject, String htmlBody) {
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            System.out.println("[EmailService - MOCK] Sending Email to: " + toEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + htmlBody);
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("from", "UCC LFMS <onboarding@resend.dev>");
        
        JsonArray toArray = new JsonArray();
        toArray.add(toEmail);
        payload.add("to", toArray);
        
        payload.addProperty("subject", subject);
        payload.addProperty("html", htmlBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        System.out.println("[EmailService] Email sent successfully to " + toEmail);
                    } else {
                        System.err.println("[EmailService] Failed to send email: " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("[EmailService] Exception while sending email: " + ex.getMessage());
                    return null;
                });
    }
}
