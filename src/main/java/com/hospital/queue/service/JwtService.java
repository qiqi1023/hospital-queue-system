package com.hospital.queue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
	private static final String ISSUER = "hospital-queue-system";
	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

	private final byte[] secret;
	private final long expirationSeconds;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds,
			ObjectMapper objectMapper, Clock clock) {
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
		}
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.expirationSeconds = expirationSeconds;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	public String issue(String username, String role) {
		long issuedAt = Instant.now(clock).getEpochSecond();
		Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("iss", ISSUER);
		claims.put("sub", username);
		claims.put("role", role);
		claims.put("iat", issuedAt);
		claims.put("exp", issuedAt + expirationSeconds);
		try {
			String content = encode(objectMapper.writeValueAsBytes(header)) + "."
					+ encode(objectMapper.writeValueAsBytes(claims));
			return content + "." + encode(sign(content));
		}
		catch (Exception error) {
			throw new IllegalStateException("Unable to create access token", error);
		}
	}

	public Optional<JwtPrincipal> verify(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) return Optional.empty();
			String content = parts[0] + "." + parts[1];
			if (!MessageDigest.isEqual(sign(content), URL_DECODER.decode(parts[2]))) return Optional.empty();

			Map<String, Object> claims = objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {});
			String issuer = String.valueOf(claims.get("iss"));
			String username = String.valueOf(claims.get("sub"));
			String role = String.valueOf(claims.get("role"));
			long expiresAt = ((Number) claims.get("exp")).longValue();
			if (!ISSUER.equals(issuer) || username.isBlank() || !"ADMIN".equals(role)
					|| expiresAt <= Instant.now(clock).getEpochSecond()) return Optional.empty();
			return Optional.of(new JwtPrincipal(username, role, expiresAt));
		}
		catch (Exception ignored) {
			return Optional.empty();
		}
	}

	public long expirationSeconds() {
		return expirationSeconds;
	}

	private byte[] sign(String content) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret, "HmacSHA256"));
		return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
	}

	private String encode(byte[] value) {
		return URL_ENCODER.encodeToString(value);
	}

	public record JwtPrincipal(String username, String role, long expiresAt) {}
}
