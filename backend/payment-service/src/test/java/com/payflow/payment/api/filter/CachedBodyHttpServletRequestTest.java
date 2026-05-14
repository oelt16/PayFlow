package com.payflow.payment.api.filter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * Unit tests for CachedBodyHttpServletRequest.
 */
@DisplayName("CachedBodyHttpServletRequest")
@ExtendWith(MockitoExtension.class)
class CachedBodyHttpServletRequestTest {

    private jakarta.servlet.http.HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
    }

    private CachedBodyHttpServletRequest createRequestWithBody(String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        when(mockRequest.getInputStream()).thenReturn(
                new MockServletInputStream(new ByteArrayInputStream(bodyBytes))
        );
        return new CachedBodyHttpServletRequest(mockRequest);
    }

    @Nested
    @DisplayName("Body Caching")
    class BodyCaching {

        @Test
        @DisplayName("should cache request body")
        void cachesRequestBody() throws IOException {
            String body = "{\"test\":\"data\"}";
            CachedBodyHttpServletRequest request = createRequestWithBody(body);

            // Read body multiple times - should be cached
            byte[] firstRead = request.getCachedBody();
            byte[] secondRead = request.getCachedBody();

            assertArrayEquals(firstRead, secondRead);
            assertEquals(body, new String(firstRead, StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("should return cached body as byte array")
        void returnsCachedBodyAsByteArray() throws IOException {
            String body = "test body content";
            CachedBodyHttpServletRequest request = createRequestWithBody(body);

            byte[] cached = request.getCachedBody();

            assertNotNull(cached);
            assertEquals(body.length(), cached.length);
            assertEquals(body, new String(cached, StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("should handle empty body")
        void handlesEmptyBody() throws IOException {
            CachedBodyHttpServletRequest request = createRequestWithBody("");

            byte[] cached = request.getCachedBody();

            assertNotNull(cached);
            assertEquals(0, cached.length);
        }
    }

    @Nested
    @DisplayName("InputStream Access")
    class InputStreamAccess {

        @Test
        @DisplayName("should return reusable input stream")
        void returnsReusableInputStream() throws IOException {
            String body = "test content";
            CachedBodyHttpServletRequest request = createRequestWithBody(body);

            ServletInputStream stream1 = request.getInputStream();
            ServletInputStream stream2 = request.getInputStream();

            // Should be able to read from stream multiple times because body is cached
            assertNotNull(stream1);
            assertNotNull(stream2);
        }

        @Test
        @DisplayName("should allow reading body via getReader")
        void allowsReadingViaGetReader() throws IOException {
            String body = "test body for reader";
            CachedBodyHttpServletRequest request = createRequestWithBody(body);

            BufferedReader reader = request.getReader();
            String readContent = reader.readLine();

            assertEquals(body, readContent);
        }
    }

    // Helper class to create mock ServletInputStream
    private static class MockServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public MockServletInputStream(ByteArrayInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}