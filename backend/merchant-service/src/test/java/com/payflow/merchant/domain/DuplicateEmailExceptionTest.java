package com.payflow.merchant.domain;

import com.payflow.merchant.domain.exception.DuplicateEmailException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateEmailExceptionTest {

    @Test
    void messageContainsEmail() {
        DuplicateEmailException ex = new DuplicateEmailException("a@b.com");
        assertThat(ex.getMessage()).contains("a@b.com");
    }
}
