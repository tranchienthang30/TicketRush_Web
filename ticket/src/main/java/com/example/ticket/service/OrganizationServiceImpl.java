package com.example.ticket.service;

import com.example.ticket.dto.request.RegisterOrganizationRequest;
import com.example.ticket.dto.response.OrganizationResponse;
import com.example.ticket.exception.AppException;
import com.example.ticket.model.entity.Organization;
import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.UserRole;
import com.example.ticket.repository.OrganizationRepository;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.security.JwtPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationServiceImpl implements OrganizationService {
    private static final String TOKEN_KEY_PREFIX = "auth:organization-verify:token:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private static final String PAYLOAD_SEPARATOR = "|";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final VerificationTokenService tokenService;
    private final EmailService emailService;
    private final String frontendUrl;

    public OrganizationServiceImpl(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            VerificationTokenService tokenService,
            EmailService emailService,
            @Value("${frontend.url}") String frontendUrl
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void requestOrganizationRegistration(RegisterOrganizationRequest request) {
        String businessEmail = normalizeEmail(request.businessEmail());
        if (organizationRepository.existsByBusinessEmailIgnoreCase(businessEmail)) {
            throw new AppException(HttpStatus.CONFLICT, "Business email is already registered");
        }

        User user = currentUser();
        String payload = user.getId() + PAYLOAD_SEPARATOR + encode(request.name().trim()) + PAYLOAD_SEPARATOR + businessEmail;
        String token = tokenService.createToken(TOKEN_KEY_PREFIX, payload, TOKEN_TTL);
        emailService.sendOrganizationVerificationEmail(
                businessEmail,
                request.name().trim(),
                frontendUrl + "/organization/verify?token=" + token
        );
    }

    @Override
    @Transactional
    public OrganizationResponse verifyOrganization(String token) {
        String payload = tokenService.consumeToken(TOKEN_KEY_PREFIX, token);
        if (payload == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Organization verification token is invalid or expired");
        }

        String[] parts = payload.split("\\|", 3);
        if (parts.length != 3) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Organization verification token is invalid");
        }

        UUID userId = UUID.fromString(parts[0]);
        String organizationName = decode(parts[1]);
        String businessEmail = parts[2];

        if (organizationRepository.existsByBusinessEmailIgnoreCase(businessEmail)) {
            throw new AppException(HttpStatus.CONFLICT, "Business email is already registered");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "User does not exist"));

        Organization organization = organizationRepository.save(Organization.builder()
                .name(organizationName)
                .businessEmail(businessEmail)
                .ownerId(user.getId())
                .verifiedAt(Instant.now())
                .build());

        user.setRole(UserRole.ORGANIZER);
        user.setPrimaryOrganizationId(organization.getId());
        userRepository.save(user);

        return OrganizationResponse.from(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> myOrganizations() {
        UUID userId = currentUserId();
        return organizationRepository.findByOwnerId(userId)
                .stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    private User currentUser() {
        return userRepository.findById(currentUserId())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User does not exist"));
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "You need to sign in");
        }
        return principal.userId();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
