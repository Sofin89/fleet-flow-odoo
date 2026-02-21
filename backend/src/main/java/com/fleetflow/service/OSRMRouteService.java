package com.fleetflow.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OSRMRouteService {

    private final RestTemplate restTemplate;
    private final Map<String, RouteData> routeCache = new ConcurrentHashMap<>();

    private static final int MAX_RETRIES = 3;
    private static final int BASE_DELAY_MS = 500;
    private static final int TIMEOUT_MS = 8000;

    public OSRMRouteService() {
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(TIMEOUT_MS))
                .setReadTimeout(Duration.ofMillis(TIMEOUT_MS))
                .build();
    }

    @Data
    @AllArgsConstructor
    public static class RouteData {
        private List<double[]> polyline; // [[lat, lng], ...]
        private double distanceKm;
        private boolean isFallback;
    }

    /**
     * Get route between two points, checking cache first.
     */
    public RouteData getRoute(double originLat, double originLng,
                              double destLat, double destLng) {
        String cacheKey = String.format("%.5f,%.5f-%.5f,%.5f",
                originLat, originLng, destLat, destLng);

        // Check cache first
        if (routeCache.containsKey(cacheKey)) {
            log.debug("Route cache hit for {}", cacheKey);
            return routeCache.get(cacheKey);
        }

        // Attempt OSRM fetch with retries
        RouteData route = fetchWithRetry(originLat, originLng, destLat, destLng);
        routeCache.put(cacheKey, route);
        return route;
    }

    /**
     * Fetch route from OSRM with exponential backoff retry logic.
     */
    private RouteData fetchWithRetry(double originLat, double originLng,
                                     double destLat, double destLng) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String url = String.format(
                        "https://router.project-osrm.org/route/v1/driving/%.5f,%.5f;%.5f,%.5f?overview=full&geometries=polyline",
                        originLng, originLat, destLng, destLat
                );

                log.debug("OSRM fetch attempt {} for route", attempt);

                // Fetch with timeout
                ResponseEntity<OSRMResponse> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, OSRMResponse.class
                );

                if (response.getBody() != null &&
                        "Ok".equals(response.getBody().getCode()) &&
                        response.getBody().getRoutes() != null &&
                        !response.getBody().getRoutes().isEmpty()) {

                    OSRMRoute route = response.getBody().getRoutes().get(0);
                    List<double[]> polyline = decodePolyline(route.getGeometry());
                    double distanceKm = route.getDistance() / 1000.0;

                    log.info("OSRM route fetched successfully on attempt {}", attempt);
                    return new RouteData(polyline, distanceKm, false);
                }
            } catch (Exception e) {
                log.warn("OSRM fetch attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        long delay = BASE_DELAY_MS * (long) Math.pow(2, attempt - 1);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // Fallback to straight line
        log.warn("OSRM fetch failed after {} attempts, using fallback", MAX_RETRIES);
        return createFallbackRoute(originLat, originLng, destLat, destLng);
    }

    /**
     * Create a straight-line fallback route when OSRM fails.
     */
    private RouteData createFallbackRoute(double originLat, double originLng,
                                          double destLat, double destLng) {
        List<double[]> straightLine = List.of(
                new double[]{originLat, originLng},
                new double[]{destLat, destLng}
        );
        double distance = haversine(originLat, originLng, destLat, destLng);
        return new RouteData(straightLine, distance, true);
    }

    /**
     * Decode OSRM polyline format to list of [lat, lng] coordinates.
     * Based on Google's polyline encoding algorithm.
     */
    private List<double[]> decodePolyline(String encoded) {
        List<double[]> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double latitude = lat / 1E5;
            double longitude = lng / 1E5;
            poly.add(new double[]{latitude, longitude});
        }

        return poly;
    }

    /**
     * Calculate distance between two points using Haversine formula.
     * Returns distance in kilometers.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // OSRM API response DTOs
    @Data
    private static class OSRMResponse {
        private String code;
        private List<OSRMRoute> routes;
    }

    @Data
    private static class OSRMRoute {
        private String geometry;
        private double distance; // in meters
    }
}
