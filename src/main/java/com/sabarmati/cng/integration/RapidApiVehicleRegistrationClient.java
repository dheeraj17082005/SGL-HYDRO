package com.sabarmati.cng.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "vehicle.verification.provider", havingValue = "rapidapi")
public class RapidApiVehicleRegistrationClient implements VehicleRegistrationClient {

    private static final Logger log = LoggerFactory.getLogger(RapidApiVehicleRegistrationClient.class);

    private final String rapidApiKey;
    private final String rapidApiHost;
    private final String rapidApiUrl;
    private final RestTemplate restTemplate;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    @org.springframework.beans.factory.annotation.Autowired
    public RapidApiVehicleRegistrationClient(
            @Value("${rapidapi.key:${RAPIDAPI_KEY:}}") String rapidApiKey,
            @Value("${rapidapi.host:${RAPIDAPI_HOST:vehicle-rc-information.p.rapidapi.com}}") String rapidApiHost,
            @Value("${rapidapi.url:${RAPIDAPI_URL:https://vehicle-rc-information.p.rapidapi.com/}}") String rapidApiUrl) {
        this.rapidApiKey = rapidApiKey;
        this.rapidApiHost = rapidApiHost;
        this.rapidApiUrl = rapidApiUrl;
        this.restTemplate = new RestTemplate();
    }

    public RapidApiVehicleRegistrationClient(String rapidApiKey, String rapidApiHost, String rapidApiUrl, RestTemplate restTemplate) {
        this.rapidApiKey = rapidApiKey;
        this.rapidApiHost = rapidApiHost;
        this.rapidApiUrl = rapidApiUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProviderName() {
        return "RAPIDAPI";
    }

    @Override
    @SuppressWarnings("unchecked")
    public VehicleRegistrationResult verifyRegistration(String registrationNumber) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return VehicleRegistrationResult.builder()
                    .registrationNumber(registrationNumber)
                    .found(false)
                    .valid(false)
                    .build();
        }

        String cleanRegNumber = registrationNumber.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        if (rapidApiKey == null || rapidApiKey.isBlank()) {
            log.error("RapidAPI key is not configured. Set RAPIDAPI_KEY environment variable.");
            return VehicleRegistrationResult.builder()
                    .registrationNumber(cleanRegNumber)
                    .found(false)
                    .valid(false)
                    .build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-rapidapi-key", rapidApiKey);
            headers.set("x-rapidapi-host", rapidApiHost);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("registrationNo", cleanRegNumber);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending vehicle verification request to RapidAPI host: {} for plate: {}", rapidApiHost, cleanRegNumber);

            ResponseEntity<Map> response = restTemplate.exchange(rapidApiUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null) {
                log.warn("Empty response body from RapidAPI for plate: {}", cleanRegNumber);
                return VehicleRegistrationResult.builder()
                        .registrationNumber(cleanRegNumber)
                        .found(false)
                        .valid(false)
                        .build();
            }

            Boolean success = (Boolean) body.get("success");
            if (Boolean.TRUE.equals(success) && body.get("data") instanceof Map<?, ?> dataMapObj) {
                Map<String, Object> dataMap = (Map<String, Object>) dataMapObj;

                String ownerName = (String) dataMap.get("ownerName");
                String vehicleClass = (String) dataMap.get("vehicleClass");
                String fuelType = (String) dataMap.get("fuelType");
                String rcStatus = (String) dataMap.get("rcStatus");
                String fitnessUptoStr = (String) dataMap.get("fitnessUpto");

                LocalDate fitnessExpiry = parseDate(fitnessUptoStr);

                boolean isValidStatus = rcStatus == null || (!rcStatus.toUpperCase().contains("SUSPENDED") && !rcStatus.toUpperCase().contains("CANCELLED"));
                boolean isNotExpired = fitnessExpiry == null || !fitnessExpiry.isBefore(LocalDate.now());

                boolean valid = isValidStatus && isNotExpired;

                log.info("RapidAPI vehicle match found for plate: {}, Owner: {}, Status: {}, Fuel: {}, FitnessExpiry: {}, Valid: {}",
                        cleanRegNumber, ownerName, rcStatus, fuelType, fitnessExpiry, valid);

                return VehicleRegistrationResult.builder()
                        .registrationNumber(cleanRegNumber)
                        .found(true)
                        .valid(valid)
                        .vehicleType(vehicleClass != null ? vehicleClass : "CAR")
                        .ownerName(ownerName != null ? ownerName : "Registered Owner")
                        .registrationExpiry(fitnessExpiry != null ? fitnessExpiry : LocalDate.now().plusYears(1))
                        .build();
            } else {
                String errorMsg = (String) body.get("error");
                log.info("RapidAPI returned unsuccessful response for plate: {}: {}", cleanRegNumber, errorMsg);
                return VehicleRegistrationResult.builder()
                        .registrationNumber(cleanRegNumber)
                        .found(false)
                        .valid(false)
                        .build();
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.info("Vehicle registration not found in RapidAPI registry for plate: {}", cleanRegNumber);
            return VehicleRegistrationResult.builder()
                    .registrationNumber(cleanRegNumber)
                    .found(false)
                    .valid(false)
                    .build();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.error("RapidAPI authentication failed (HTTP {}): Check RAPIDAPI_KEY and RAPIDAPI_HOST", e.getStatusCode());
            return VehicleRegistrationResult.builder()
                    .registrationNumber(cleanRegNumber)
                    .found(false)
                    .valid(false)
                    .build();
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("RapidAPI rate limit exceeded (HTTP 429) for plate: {}", cleanRegNumber);
            return VehicleRegistrationResult.builder()
                    .registrationNumber(cleanRegNumber)
                    .found(false)
                    .valid(false)
                    .build();
        } catch (ResourceAccessException e) {
            log.error("RapidAPI connection timeout or network failure for plate {}: {}", cleanRegNumber, e.getMessage());
            return VehicleRegistrationResult.builder()
                    .registrationNumber(cleanRegNumber)
                    .found(false)
                    .valid(false)
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error executing RapidAPI vehicle verification for plate {}: {}", cleanRegNumber, e.getMessage(), e);
            return VehicleRegistrationResult.builder()
                    .registrationNumber(cleanRegNumber)
                    .found(false)
                    .valid(false)
                    .build();
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        log.warn("Could not parse fitness date string: {}", dateStr);
        return null;
    }
}
