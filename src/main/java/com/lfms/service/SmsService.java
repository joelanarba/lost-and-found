package com.lfms.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SmsService {

    private static final String API_KEY = System.getenv("MNOTIFY_API_KEY");
    // The mNotify v2 endpoint structure using a query param for API key:
    private static final String MNOTIFY_API_URL = "https://api.mnotify.com/api/sms/quick?key=" + (API_KEY != null ? API_KEY : "");

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void sendSms(String recipientPhone, String message) {
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            System.out.println("[SmsService - MOCK] Sending SMS to: " + recipientPhone);
            System.out.println("Message: " + message);
            return;
        }

        if (recipientPhone == null || recipientPhone.trim().isEmpty()) {
            System.err.println("[SmsService] Phone number is empty, aborting SMS.");
            return;
        }

        JsonObject payload = new JsonObject();
        JsonArray recipientsArray = new JsonArray();
        recipientsArray.add(recipientPhone);
        payload.add("recipient", recipientsArray);
        
        payload.addProperty("sender", "UCC-LFMS");
        payload.addProperty("message", message);
        payload.addProperty("is_schedule", "false");
        payload.addProperty("schedule_date", "");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MNOTIFY_API_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        System.out.println("[SmsService] SMS sent successfully to " + recipientPhone);
                    } else {
                        System.err.println("[SmsService] Failed to send SMS: " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("[SmsService] Exception while sending SMS: " + ex.getMessage());
                    return null;
                });
    }
}
