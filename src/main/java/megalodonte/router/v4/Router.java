package megalodonte.router.v4;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import megalodonte.application.Context;
import megalodonte.base.components.ComponentInterface;
import megalodonte.base.route.RouterBase;
import megalodonte.router.RouteNotFoundException;
import megalodonte.router.RouteParamsAware;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Central routing manager responsible for navigation and window spawning.
 *
 * <p>The Router handles:</p>
 * <ul>
 *   <li>Smart navigation to the currently active stage</li>
 *   <li>Dynamic route matching</li>
 *   <li>Secondary window lifecycle management</li>
 *   <li>Route parameter injection</li>
 *   <li>Active stage tracking and focus management</li>
 * </ul>
 *
 * <p>Navigation Behavior:</p>
 * <ul>
 *   <li>navigateTo() always navigates the currently active stage</li>
 *   <li>spawnWindow() automatically makes the new window active</li>
 *   <li>focusSpawn() switches navigation to a specific spawned window</li>
 *   <li>focusMainStage() returns navigation to the main window</li>
 * </ul>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * // Create router
 * Router router = new Router(routes, "home", mainStage);
 * 
 * // Navigate on main window (default behavior)
 * router.navigateTo("about");
 * 
 * // Spawn and navigate in new window
 * router.spawnWindow("user/123");
 * router.navigateTo("profile"); // Navigates in spawned window
 * 
 * // Return navigation to main window
 * router.focusMainStage();
 * router.navigateTo("home"); // Navigates in main window
 * }</pre>
 */
public final class Router implements RouterBase {
    public record Route(
            String identification,
            Function<ScreenContext, Object> factory,
            RouteProps props
    ) {}


    //TODO: talvez devesse ficar no contexto da aplicação base
    private final List<Stage> spawnedWindowList;

    private final Set<Route> routes;
    private final String entrypoint;

    public Router(Set<Route> routes, String entrypoint) {
        this.routes = routes;
        this.entrypoint = entrypoint;
        spawnedWindowList = new ArrayList<>();
    }

    public RouteResult entrypoint() {
        return resolve(entrypoint);
    }

    //Context principal da aplicação base
    private Context boundContext;

    public void bind(Context context) {
        this.boundContext = context;
    }

    public RouteResult navigateTo(String path) {
        return resolve(path);
    }

    /**
     * Spawns a new window for the given route.
     *
     * @param path route identification to spawn
     * @param errorHandler callback invoked if spawning fails
     */
    public void spawnWindow(String path, Consumer<Exception> errorHandler) {
        try {
            Stage stage = new Stage();

            // Resolve com a stage da janela nova, não a principal
            RouteResult routeResult = resolveWithStage(path, stage);
            RouteProps props = routeResult.props();

            Parent parent = (Parent) routeResult.view().getJavaFxNode();
            Scene scene = new Scene(parent, props.screenWidth(), props.screenHeight());

            stage.setTitle(props.name());
            stage.setResizable(props.screenIsExpandable());
            stage.setScene(scene);
            stage.show();

            spawnedWindowList.add(stage);
            stage.setOnCloseRequest(e -> spawnedWindowList.removeIf(w -> w == stage));

        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }


    /* ---------------- internals ---------------- */

    private RouteResult resolveWithStage(String path, Stage stage) {
        ResolvedRoute resolved = resolveRoute(path);
        Route route = resolved.route();

        ScreenContext ctx = new ScreenContext(stage, this);
        Object screen = route.factory().apply(ctx);

        if (screen instanceof RouteParamsAware aware) {
            aware.onRouteParams(resolved.params());
        }

        invokeOptional(screen, "onMount");

        ComponentInterface<?> view = extractView(screen);
        return new RouteResult(view, route.props());
    }

    private RouteResult resolve(String path) {
        Stage mainStage = boundContext != null ? boundContext.javafxStage() : null;
        return resolveWithStage(path, mainStage);
    }


    private ComponentInterface<?> extractView(Object screen) {

        // Caso 1 — Screen já é uma View
        if (screen instanceof ComponentInterface<?> view) {
            return view;
        }

        // Caso 2 — Screen expõe render()
        try {
            var method = screen.getClass().getMethod("render");
            Object result = method.invoke(screen);

            if (!(result instanceof ComponentInterface<?> component)) {
                throw new RuntimeException(
                        "render() of " + screen.getClass().getSimpleName()
                                + " must return ComponentInterface"
                );
            }

            return component;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "Screen " + screen.getClass().getSimpleName()
                            + " must expose render() returning ComponentInterface",
                    e
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to invoke render() on "
                            + screen.getClass().getSimpleName(),
                    e
            );
        }
    }


    private void invokeOptional(Object target, String method) {
        try {
            target.getClass().getMethod(method).invoke(target);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            System.err.println(
                    "Error invoking " + method + " on "
                            + target.getClass().getSimpleName()
            );
        }
    }

    /* ---------------- route matching ---------------- */

    private record ResolvedRoute(Route route, Map<String, String> params) {}

    private ResolvedRoute resolveRoute(String path) {
        String[] pathParts = path.split("/");

        for (Route route : routes) {
            String[] routeParts = route.identification().split("/");
            if (routeParts.length != pathParts.length) continue;

            Map<String, String> params = new HashMap<>();
            boolean matched = true;

            for (int i = 0; i < routeParts.length; i++) {
                String rp = routeParts[i];
                String pp = pathParts[i];

                if (rp.startsWith("${") && rp.endsWith("}")) {
                    params.put(rp.substring(2, rp.length() - 1), pp);
                } else if (!rp.equals(pp)) {
                    matched = false;
                    break;
                }
            }

            if (matched) {
                return new ResolvedRoute(route, params);
            }
        }

        throw new RouteNotFoundException(path);
    }
}
