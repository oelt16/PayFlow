package com.payflow.payment.api.security;

import com.payflow.payment.domain.MerchantId;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Resolves {@link MerchantId} from a raw API key by reading {@code merchants.merchants}
 * (prefix lookup + BCrypt), matching merchant-service behaviour.
 */
@Component
public class JdbcApiKeyAuthenticator {

    private static final int KEY_PREFIX_LENGTH = 8;

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public JdbcApiKeyAuthenticator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @param rawApiKey Bearer token value (no "Bearer " prefix)
     * @return merchant id when a row matches; empty when unknown or invalid
     */
    public Optional<MerchantId> resolveMerchantId(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.length() < KEY_PREFIX_LENGTH) {
            return Optional.empty();
        }
        String prefix = rawApiKey.substring(0, KEY_PREFIX_LENGTH);
        return jdbcTemplate.query(
                "SELECT id, key_hash FROM merchants.merchants WHERE key_prefix = ? AND is_active = TRUE",
                ps -> ps.setString(1, prefix),
                rs -> {
                    while (rs.next()) {
                        if (bcrypt.matches(rawApiKey, rs.getString("key_hash"))) {
                            return Optional.of(MerchantId.of(rs.getString("id")));
                        }
                    }
                    return Optional.empty();
                }
        );
    }
}
