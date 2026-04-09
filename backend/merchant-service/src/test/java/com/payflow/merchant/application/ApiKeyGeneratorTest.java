package com.payflow.merchant.application;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyGeneratorTest {

    @Test
    void newApiKeyHasExpectedPrefixAndLength() {
        ApiKeyGenerator gen = new ApiKeyGenerator();
        String key = gen.newApiKey();
        assertThat(key).startsWith("sk_test_");
        assertThat(key).hasSize("sk_test_".length() + 32);
    }

    @RepeatedTest(5)
    void newApiKeysAreUnique() {
        ApiKeyGenerator gen = new ApiKeyGenerator();
        assertThat(gen.newApiKey()).isNotEqualTo(gen.newApiKey());
    }

    @Test
    void keyPrefixFirstEightChars() {
        assertThat(ApiKeyGenerator.keyPrefix("sk_test_abcdef")).isEqualTo("sk_test_");
    }

    @Test
    void keyPrefixRejectsShortKey() {
        assertThatThrownBy(() -> ApiKeyGenerator.keyPrefix("short"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
