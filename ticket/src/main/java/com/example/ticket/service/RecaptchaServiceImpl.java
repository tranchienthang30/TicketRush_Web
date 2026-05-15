package com.example.ticket.service;

import com.example.ticket.exception.AppException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class RecaptchaServiceImpl implements RecaptchaService {
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestClient restClient;
    private final boolean enabled;
    private final String secret;
    private final double minScore;

    public RecaptchaServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${app.recaptcha.enabled:false}") boolean enabled,
            @Value("${app.recaptcha.secret:}") String secret,
            @Value("${app.recaptcha.min-score:0.5}") double minScore
    ) {
        this.restClient = restClientBuilder.build();
        this.enabled = enabled;
        this.secret = secret;
        this.minScore = minScore;
    }

    @Override
    public void verify(String token, String expectedAction, String clientIp) {
        if (!enabled) {
            return;
        }
        if (secret == null || secret.isBlank()) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "reCAPTCHA is not configured");
        }
        if (token == null || token.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "reCAPTCHA token is required");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);
        if (clientIp != null && !clientIp.isBlank()) {
            form.add("remoteip", clientIp);
        }

        RecaptchaVerifyResponse response = restClient.post()
                .uri(VERIFY_URL)
                .body(form)
                .retrieve()
                .body(RecaptchaVerifyResponse.class);

        if (response == null || !response.success()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "reCAPTCHA verification failed");
        }
        if (response.score() != null && response.score() < minScore) {
            throw new AppException(HttpStatus.BAD_REQUEST, "reCAPTCHA score is too low");
        }
        if (response.action() != null && !response.action().equals(expectedAction)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "reCAPTCHA action is invalid");
        }
    }

    private record RecaptchaVerifyResponse(
            boolean success,
            Double score,
            String action,
            String challenge_ts,
            String hostname,
            List<String> errorCodes
    ) {
    }
}
