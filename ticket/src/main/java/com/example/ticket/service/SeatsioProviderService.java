package com.example.ticket.service;

import com.example.ticket.config.SeatsioProperties;
import com.example.ticket.dto.request.SeatsioChartCreateRequest;
import com.example.ticket.dto.request.SeatsioEventCreateRequest;
import com.example.ticket.dto.response.SeatsioCategoryResponse;
import com.example.ticket.dto.response.SeatsioChartResponse;
import com.example.ticket.dto.response.SeatsioEventResponse;
import com.example.ticket.dto.response.SeatsioWorkspaceResponse;
import com.example.ticket.exception.AppException;
import com.example.ticket.model.entity.ProviderSeatWorkspace;
import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.UserRole;
import com.example.ticket.repository.ProviderSeatWorkspaceRepository;
import com.example.ticket.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Transactional
public class SeatsioProviderService {
    private final SeatsioProperties properties;
    private final ProviderSeatWorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final RestClient restClient;

    public SeatsioProviderService(
            SeatsioProperties properties,
            ProviderSeatWorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.restClient = restClientBuilder.baseUrl(properties.apiBaseUrl()).build();
    }

    @Transactional(readOnly = true)
    public SeatsioWorkspaceResponse getWorkspace(UUID providerId) {
        requireProvider(providerId);
        return workspaceRepository.findById(providerId)
                .map(this::toWorkspaceResponse)
                .orElseGet(() -> new SeatsioWorkspaceResponse(
                        properties.isConfigured(),
                        properties.getRegion(),
                        properties.cdnUrl(),
                        null,
                        null,
                        null,
                        properties.isSandbox(),
                        false,
                        properties.isConfigured()
                                ? "No seats.io workspace exists for this provider yet."
                                : "Seats.io admin key is not configured. Manual key entry is still available."
                ));
    }

    public SeatsioWorkspaceResponse ensureWorkspace(UUID providerId) {
        User provider = requireProvider(providerId);
        return workspaceRepository.findById(providerId)
                .map(this::toWorkspaceResponse)
                .orElseGet(() -> toWorkspaceResponse(createWorkspace(provider)));
    }

    public SeatsioChartResponse createChart(UUID providerId, SeatsioChartCreateRequest request) {
        ProviderSeatWorkspace workspace = workspaceRepository.findById(providerId)
                .orElseGet(() -> createWorkspace(requireProvider(providerId)));

        String venueType = normalizeVenueType(request.venueType());
        String chartName = request.name() == null || request.name().isBlank()
                ? "TicketRush chart"
                : request.name().trim();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", chartName);
        body.put("venueType", venueType);
        body.put("categories", defaultCategories());

        JsonNode response = withWorkspaceActivationRetry(
                workspace,
                () -> post("/charts", workspace.getWorkspaceSecretKey(), body)
        );
        return new SeatsioChartResponse(
                text(response, "key"),
                text(response, "name"),
                text(response, "status"),
                text(response, "publishedVersionThumbnailUrl")
        );
    }

    public SeatsioEventResponse createEvent(UUID providerId, SeatsioEventCreateRequest request) {
        ProviderSeatWorkspace workspace = workspaceRepository.findById(providerId)
                .orElseGet(() -> createWorkspace(requireProvider(providerId)));

        String eventKey = request.eventKey() == null || request.eventKey().isBlank()
                ? "tr-" + UUID.randomUUID()
                : request.eventKey().trim();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chartKey", request.chartKey().trim());
        body.put("eventKey", eventKey);
        if (request.name() != null && !request.name().isBlank()) {
            body.put("name", request.name().trim());
        }
        if (request.date() != null) {
            body.put("date", request.date().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        JsonNode response = withWorkspaceActivationRetry(
                workspace,
                () -> post("/events", workspace.getWorkspaceSecretKey(), body)
        );
        return new SeatsioEventResponse(
                text(response, "eventKey", text(response, "key", eventKey)),
                request.chartKey().trim(),
                text(response, "name"),
                text(response, "date")
        );
    }

    @Transactional(readOnly = true)
    public List<SeatsioCategoryResponse> listChartCategories(UUID providerId, String chartKey) {
        if (chartKey == null || chartKey.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Seats.io chart key is required");
        }
        ProviderSeatWorkspace workspace = workspaceRepository.findById(providerId)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Seats.io workspace is not ready"));

        JsonNode response = withWorkspaceActivationRetry(
                workspace,
                () -> get("/charts/{chartKey}/categories", workspace.getWorkspaceSecretKey(), chartKey.trim())
        );
        JsonNode categoriesNode = response == null ? null : (response.isArray() ? response : response.path("categories"));
        List<SeatsioCategoryResponse> categories = new ArrayList<>();
        if (categoriesNode != null && categoriesNode.isArray()) {
            for (JsonNode category : categoriesNode) {
                String key = text(category, "key");
                if (key == null) {
                    continue;
                }
                categories.add(new SeatsioCategoryResponse(
                        key,
                        text(category, "label", key),
                        text(category, "color"),
                        category.path("accessible").asBoolean(false)
                ));
            }
        }
        return categories;
    }

    @Transactional(readOnly = true)
    public ProviderSeatWorkspace workspaceForProvider(UUID providerId) {
        return workspaceRepository.findById(providerId).orElse(null);
    }

    private ProviderSeatWorkspace createWorkspace(User provider) {
        if (!properties.isConfigured()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Seats.io admin key is not configured");
        }

        String workspaceName = "TicketRush - " + providerName(provider) + " - " + shortId();
        JsonNode response = post("/workspaces", properties.getAdminKey(), Map.of("name", workspaceName));

        ProviderSeatWorkspace workspace = new ProviderSeatWorkspace();
        workspace.setProviderId(provider.getId());
        workspace.setSeatsioWorkspaceId(response.path("id").isNumber() ? response.path("id").asLong() : null);
        workspace.setWorkspaceName(text(response, "name", workspaceName));
        workspace.setWorkspaceKey(text(response, "key"));
        workspace.setWorkspaceSecretKey(text(response, "secretKey"));
        workspace.setTest(response.path("isTest").asBoolean(properties.isSandbox()));
        workspace.setActive(response.path("isActive").asBoolean(true));

        if (workspace.getWorkspaceKey() == null || workspace.getWorkspaceSecretKey() == null) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "Seats.io workspace response did not include required keys");
        }
        return workspaceRepository.save(workspace);
    }

    private JsonNode post(String path, String secretOrAdminKey, Map<String, Object> body) {
        try {
            return restClient.post()
                    .uri(path)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .headers(headers -> headers.setBasicAuth(secretOrAdminKey, ""))
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "Seats.io request failed: " + exception.getMessage());
        }
    }

    private JsonNode get(String path, String secretOrAdminKey, Object... uriVariables) {
        try {
            return restClient.get()
                    .uri(path, uriVariables)
                    .headers(headers -> headers.setBasicAuth(secretOrAdminKey, ""))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "Seats.io request failed: " + exception.getMessage());
        }
    }

    private JsonNode withWorkspaceActivationRetry(ProviderSeatWorkspace workspace, Supplier<JsonNode> request) {
        try {
            return request.get();
        } catch (AppException exception) {
            if (!isUserInactive(exception)) {
                throw exception;
            }
            activateWorkspace(workspace);
            return request.get();
        }
    }

    private void activateWorkspace(ProviderSeatWorkspace workspace) {
        if (!properties.isConfigured()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Seats.io admin key is not configured");
        }
        try {
            restClient.post()
                    .uri("/workspaces/{workspaceKey}/actions/activate", workspace.getWorkspaceKey())
                    .headers(headers -> headers.setBasicAuth(properties.getAdminKey(), ""))
                    .retrieve()
                    .toBodilessEntity();
            workspace.setActive(true);
            workspaceRepository.save(workspace);
        } catch (RestClientException exception) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "Seats.io workspace activation failed: " + exception.getMessage());
        }
    }

    private boolean isUserInactive(AppException exception) {
        return exception.getMessage() != null && exception.getMessage().contains("USER_INACTIVE");
    }

    private SeatsioWorkspaceResponse toWorkspaceResponse(ProviderSeatWorkspace workspace) {
        return new SeatsioWorkspaceResponse(
                properties.isConfigured(),
                properties.getRegion(),
                properties.cdnUrl(),
                workspace.getWorkspaceKey(),
                workspace.getWorkspaceSecretKey(),
                workspace.getWorkspaceName(),
                workspace.isTest(),
                workspace.isActive(),
                null
        );
    }

    private User requireProvider(UUID providerId) {
        User user = userRepository.findById(providerId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User does not exist"));
        if (user.getRole() != UserRole.PROVIDER && user.getRole() != UserRole.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Approved provider access is required");
        }
        return user;
    }

    private String providerName(User provider) {
        String fullName = provider.getFullName();
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        return provider.getEmail();
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String normalizeVenueType(String value) {
        if (value == null || value.isBlank()) {
            return "WITH_SECTIONS_AND_FLOORS";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "SIMPLE", "WITH_SECTIONS_AND_FLOORS", "WITH_ZONES" -> normalized;
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "Seats.io venue type is invalid");
        };
    }

    private Object[] defaultCategories() {
        return new Object[]{
                Map.of("key", "standard", "label", "Standard", "color", "#2563EB"),
                Map.of("key", "vip", "label", "VIP", "color", "#F97316"),
                Map.of("key", "accessible", "label", "Accessible", "color", "#16A34A", "accessible", true)
        };
    }

    private String text(JsonNode node, String field) {
        return text(node, field, null);
    }

    private String text(JsonNode node, String field, String fallback) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return fallback;
        }
        String value = node.path(field).asText();
        return value == null || value.isBlank() ? fallback : value;
    }
}
