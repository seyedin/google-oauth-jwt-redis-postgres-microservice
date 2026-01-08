package com.auth.service;

import com.auth.dto.GoogleUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.client-id}")
    private String clientId;

    public GoogleUserInfoDto verifyIdToken(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body =
                    restTemplate.getForObject(url, Map.class);

            if (body == null) {
                throw new IllegalStateException("Invalid Google token");
            }

            String aud = (String) body.get("aud");
            if (!clientId.equals(aud)) {
                throw new IllegalStateException("Invalid Google token audience");
            }

            String email = (String) body.get("email");
            String sub = (String) body.get("sub");
            boolean emailVerified = Boolean.parseBoolean(
                    String.valueOf(body.get("email_verified"))
            );

            if (email == null || sub == null) {
                throw new IllegalStateException("Invalid Google token payload");
            }

            return new GoogleUserInfoDto(email, sub, emailVerified);

        } catch (RestClientException ex) {
            throw new IllegalStateException("Invalid Google token", ex);
        }
    }
}
