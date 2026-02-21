package com.fleetflow.service;

import com.fleetflow.entity.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendNewTripEmail(Trip trip) {
        if (trip.getDriver() == null || trip.getDriver().getEmail() == null) {
            log.warn("Cannot send email for Trip #{}: Driver email is missing", trip.getId());
            return;
        }

        try {
            String driverEmail = trip.getDriver().getEmail();
            String driverName = trip.getDriver().getFullName();
            String origin = trip.getOriginName() != null ? trip.getOriginName() : trip.getOrigin();
            String destination = trip.getDestinationName() != null ? trip.getDestinationName() : trip.getDestination();
            
            // Build the Google Maps URL based on the coordinates
            String originCoords = URLEncoder.encode(trip.getOrigin(), StandardCharsets.UTF_8);
            String destCoords = URLEncoder.encode(trip.getDestination(), StandardCharsets.UTF_8);
            String mapsUrl = String.format("https://www.google.com/maps/dir/?api=1&origin=%s&destination=%s", originCoords, destCoords);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(driverEmail);
            message.setSubject("New Trip Assigned: " + origin + " to " + destination);
            
            String body = String.format("""
                Hello %s,
                
                You have been assigned a new trip on FleetFlow.
                
                TRIP DETAILS:
                - Vehicle: %s (%s)
                - Origin: %s
                - Destination: %s
                - Cargo Weight: %.2f kg
                - Scheduled Status: DRAFT
                
                🗺️ CLICK HERE FOR GOOGLE MAPS DIRECTIONS:
                %s
                
                Please review the assignment on your driver dashboard.
                
                Safe travels,
                FleetFlow Dispatch Team
                """, 
                driverName, 
                trip.getVehicle().getName(), trip.getVehicle().getLicensePlate(),
                origin, 
                destination, 
                trip.getCargoWeightKg(),
                mapsUrl
            );

            message.setText(body);
            mailSender.send(message);
            log.info("Successfully sent trip assignment email to {}", driverEmail);
            
        } catch (Exception e) {
            log.error("Failed to send trip assignment email to {}", trip.getDriver().getEmail(), e);
        }
    }
}
