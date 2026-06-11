package com.realestate.exception;

import com.realestate.exception.GlobalExceptionHandler.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Status-mapping tests for {@link GlobalExceptionHandler} — regression for #37:
 * a missing static resource (e.g. a deleted /uploads/** file) must surface as
 * 404, not fall into the catch-all 500.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void noResourceFound_mapsTo404() {
        ResponseEntity<ErrorResponse> response = handler.handleNoResource(
            new NoResourceFoundException(HttpMethod.GET, "uploads/properties/x/gone.png"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void unexpectedException_stillMapsTo500() {
        ResponseEntity<ErrorResponse> response =
            handler.handleGeneral(new IllegalStateException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
    }
}
