package megalodonte.router;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for RouteParamsAware interface and implementations.
 */

@Disabled
class RouteParamsAwareTest {

    private TestUtils.MockParamAwareScreen paramAwareScreen;

    @BeforeEach
    void setUp() {
        paramAwareScreen = new TestUtils.MockParamAwareScreen();
    }

    @Test
    @DisplayName("should receive route parameters through onRouteParams")
    void onRouteParams_whenGivenParams_shouldStoreThem() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("id", "123");
        params.put("name", "test");

        // Act
        paramAwareScreen.onRouteParams(params);

        // Assert
        Map<String, String> storedParams = paramAwareScreen.getParams();
        assertEquals("123", storedParams.get("id"));
        assertEquals("test", storedParams.get("name"));
        assertEquals(2, storedParams.size());
    }

    @Test
    @DisplayName("should handle empty parameters map")
    void onRouteParams_whenGivenEmptyMap_shouldHandleGracefully() {
        // Arrange
        Map<String, String> emptyParams = new HashMap<>();

        // Act
        paramAwareScreen.onRouteParams(emptyParams);

        // Assert
        assertTrue(paramAwareScreen.getParams().isEmpty());
    }

    @Test
    @DisplayName("should create defensive copy of parameters")
    void onRouteParams_whenGivenParams_shouldCreateDefensiveCopy() {
        // Arrange
        Map<String, String> originalParams = new HashMap<>();
        originalParams.put("id", "123");

        // Act
        paramAwareScreen.onRouteParams(originalParams);
        originalParams.put("id", "456"); // Modify original map

        // Assert
        assertEquals("123", paramAwareScreen.getParams().get("id"));
    }

    @Test
    @DisplayName("should handle null parameters map")
    void onRouteParams_whenGivenNullMap_shouldHandleGracefully() {
        // Act & Assert
        assertDoesNotThrow(() -> paramAwareScreen.onRouteParams(null));
        // After null, params should still be accessible (even if empty)
        assertNotNull(paramAwareScreen.getParams());
    }

    @Test
    @DisplayName("should work with dynamic route parameters")
    void onRouteParams_whenGivenDynamicRouteParams_shouldExtractCorrectly() {
        // Arrange
        Map<String, String> dynamicParams = new HashMap<>();
        dynamicParams.put("userId", "user-123");
        dynamicParams.put("productId", "prod-456");
        dynamicParams.put("action", "edit");

        // Act
        paramAwareScreen.onRouteParams(dynamicParams);

        // Assert
        Map<String, String> storedParams = paramAwareScreen.getParams();
        assertEquals("user-123", storedParams.get("userId"));
        assertEquals("prod-456", storedParams.get("productId"));
        assertEquals("edit", storedParams.get("action"));
    }

    @Test
    @DisplayName("should support multiple parameter updates")
    void onRouteParams_whenCalledMultipleTimes_shouldUpdateParameters() {
        // Arrange
        Map<String, String> firstParams = new HashMap<>();
        firstParams.put("id", "123");

        // Act
        paramAwareScreen.onRouteParams(firstParams);
        
        Map<String, String> secondParams = new HashMap<>();
        secondParams.put("id", "456");
        secondParams.put("mode", "edit");
        
        paramAwareScreen.onRouteParams(secondParams);

        // Assert
        Map<String, String> finalParams = paramAwareScreen.getParams();
        assertEquals("456", finalParams.get("id"));
        assertEquals("edit", finalParams.get("mode"));
        assertEquals(2, finalParams.size());
    }

    /**
     * Test implementation of RouteParamsAware for interface contract testing.
     */
    @Test
    @DisplayName("should satisfy interface contract requirements")
    void interfaceContract_shouldMeetRequirements() {
        // Arrange & Act
        RouteParamsAware implementation = new TestUtils.MockParamAwareScreen();

        // Assert
        assertInstanceOf(RouteParamsAware.class, implementation);
        assertDoesNotThrow(() -> implementation.onRouteParams(new HashMap<>()));
    }
}