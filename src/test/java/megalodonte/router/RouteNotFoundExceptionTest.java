package megalodonte.router;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for RouteNotFoundException.
 */

@Disabled
class RouteNotFoundExceptionTest {

    @Test
    @DisplayName("should create exception with route identification in message")
    void constructor_whenGivenRouteIdentification_shouldIncludeInMessage() {
        // Arrange
        String routeId = "non-existent-route";

        // Act
        RouteNotFoundException exception = new RouteNotFoundException(routeId);

        // Assert
        assertTrue(exception.getMessage().contains(routeId));
        assertEquals("Route not found: " + routeId, exception.getMessage());
    }

    @Test
    @DisplayName("should be unchecked exception extending RuntimeException")
    void inheritance_shouldExtendRuntimeException() {
        // Arrange & Act
        RouteNotFoundException exception = new RouteNotFoundException("test");

        // Assert
        assertInstanceOf(RuntimeException.class, exception);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    @DisplayName("should preserve cause when using standard exception constructors")
    void constructor_shouldSupportStandardExceptionFeatures() {
        // Arrange
        String routeId = "test-route";
        RouteNotFoundException exception = new RouteNotFoundException(routeId);

        // Act & Assert
        assertNull(exception.getCause());
        
        // Test that we can set cause like any RuntimeException
        RuntimeException cause = new RuntimeException("cause");
        RouteNotFoundException withCause = new RouteNotFoundException(routeId);
        withCause.initCause(cause);
        
        assertEquals(cause, withCause.getCause());
    }

    @Test
    @DisplayName("should handle empty and null route identifications")
    void constructor_shouldHandleEdgeCases() {
        // Act & Assert - Empty string
        RouteNotFoundException emptyException = new RouteNotFoundException("");
        assertEquals("Route not found: ", emptyException.getMessage());

        // Act & Assert - Null string
        RouteNotFoundException nullException = new RouteNotFoundException(null);
        assertEquals("Route not found: null", nullException.getMessage());

        // Act & Assert - Special characters
        String specialRoute = "/user/${id}/profile";
        RouteNotFoundException specialException = new RouteNotFoundException(specialRoute);
        assertEquals("Route not found: /user/${id}/profile", specialException.getMessage());
    }
}