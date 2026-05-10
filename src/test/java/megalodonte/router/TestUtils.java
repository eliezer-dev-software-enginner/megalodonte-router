package megalodonte.router;

import megalodonte.base.components.Component;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Disabled;

import java.util.Map;
import java.util.HashMap;

/**
 * Test utility class providing mock implementations for testing.
 */

@Disabled
public class TestUtils {

    /**
     * Mock component implementation for testing purposes.
     */
    public static class MockComponent extends Component {
        public MockComponent() {
            super(new VBox());
        }
    }

    /**
     * Mock screen that implements RouteParamsAware.
     */
    public static class MockParamAwareScreen implements RouteParamsAware {
        private Map<String, String> params = new HashMap<>();

        @Override
        public void onRouteParams(Map<String, String> params) {
            this.params = params != null ? new HashMap<>(params) : new HashMap<>();
        }

        public Map<String, String> getParams() {
            return params;
        }

        public Component render() {
            return new MockComponent();
        }
    }

    /**
     * Mock screen that doesn't implement RouteParamsAware.
     */
    public static class MockSimpleScreen {
        public Component render() {
            return new MockComponent();
        }
    }

    /**
     * Mock screen that throws exception during render.
     */
    public static class MockErrorScreen {
        public Component render() {
            throw new RuntimeException("Render error for testing");
        }
    }

    /**
     * Creates a basic route for testing.
     */
    public static megalodonte.router.v4.Router.Route createTestRoute(String path) {
        return new megalodonte.router.v4.Router.Route(
            path,
            router -> new MockSimpleScreen(),
            new megalodonte.router.v4.RouteProps(800, 600, "Test Window", false)
        );
    }

    /**
     * Creates a route with parameter-aware screen for testing.
     */
    public static megalodonte.router.v4.Router.Route createParamAwareRoute(String path) {
        return new megalodonte.router.v4.Router.Route(
            path,
            router -> new MockParamAwareScreen(),
            new megalodonte.router.v4.RouteProps(800, 600, "Test Window", false)
        );
    }

    /**
     * Creates a route that causes render errors for testing.
     */
    public static megalodonte.router.v4.Router.Route createErrorRoute(String path) {
        return new megalodonte.router.v4.Router.Route(
            path,
            router -> new MockErrorScreen(),
            new megalodonte.router.v4.RouteProps(800, 600, "Error Window", false)
        );
    }
}