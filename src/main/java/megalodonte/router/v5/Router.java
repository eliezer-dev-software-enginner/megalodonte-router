package megalodonte.router.v5;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import megalodonte.application.Context;
import megalodonte.application.ErrorReporter;
import megalodonte.base.components.ComponentInterface;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.RouteProps;
import megalodonte.base.route.RouteResult;
import megalodonte.base.route.RouterBase;
import megalodonte.base.scale.ScaleProvider;
import megalodonte.base.theme.ThemeManager;
import megalodonte.router.RouteNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

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
    private static final Logger log = LoggerFactory.getLogger(Router.class);

    public record Route(
            String identification,
            ScreenFactory factory,
            RouteProps props
    ) {}

    private record ActiveScreen(ScreenComponent screen, ScreenContext ctx) {}

    private final Map<Stage, ActiveScreen> activeScreens = new HashMap<>();
    //TODO: talvez devesse ficar no contexto da aplicação base
    private final List<Stage> spawnedWindowList;

    private final Set<Route> routes;
    private final String entrypoint;

    public Router(Set<Route> routes, String entrypoint) {
        this.routes = routes;
        this.entrypoint = entrypoint;
        spawnedWindowList = new ArrayList<>();
        log.info("Router initialized with {} route(s), entrypoint: '{}'", routes.size(), entrypoint);
    }

    @Override
    public RouteResult entrypoint() {
        Stage mainStage = boundContext != null ? boundContext.javafxStage() : null;
        return resolveWithStage(entrypoint, mainStage);
    }

    //Context principal da aplicação base
    private Context boundContext;

    @Override
    public void bind(Context context) {
        this.boundContext = context;
        log.debug("Router bound to application context");
    }

    /**
     * Spawns a new window for the given route.
     *
     * @param path route identification to spawn
     */
    public void spawnWindow(String path) {
        spawnWindow(path, e-> ErrorReporter.handle(e));
    }

    /**
     * Spawns a new window for the given route.
     *
     * @param path route identification to spawn
     * @param errorHandler callback invoked if spawning fails
     */
    public void spawnWindow(String path, Consumer<Exception> errorHandler) {
        try {
            log.info("Spawning window for route '{}'", path);
            Stage stage = new Stage();

            // Resolve com a stage da janela nova, não a principal
            RouteResult routeResult = resolveWithStage(path, stage);
            RouteProps props = routeResult.props();

            Parent parent = (Parent) routeResult.view().getJavaFxNode();
            Scene scene = new Scene(parent,
                    ScaleProvider.scale(props.screenWidth()),
                    ScaleProvider.scale(props.screenHeight()));
            ThemeManager.applyFontFamily(scene);

            stage.setTitle(props.name());
            if (props.iconPath() != null && !props.iconPath().isEmpty()) {
                stage.getIcons().add(new Image(props.iconPath()));
            }
            stage.setResizable(props.screenIsExpandable());
            stage.setScene(scene);
            stage.show();

            spawnedWindowList.add(stage);
            log.info("Window spawned successfully for route '{}'", path);
            stage.setOnCloseRequest(e -> {
                log.debug("Spawned window closed for route '{}'", path);
                spawnedWindowList.removeIf(w -> w == stage);
                ActiveScreen active = activeScreens.remove(stage);
                if (active != null) {
                    active.ctx().scope().cancel();
                    active.screen().onDestroy();
                }
            });
        } catch (Exception e) {
            log.error("Failed to spawn window for route '{}'", path, e);
            errorHandler.accept(e);
        }
    }

    /**
     * Chamado no @ScreenContext
     * **/
    @Override
    public RouteResult navigateOnStage(String path, Stage stage) {
        log.debug("Navigating to '{}' on stage", path);
        return resolveWithStage(path, stage);
    }

    @Override
    public RouteResult navigateAndCloseOthers(String path) {
        log.info("Navigating to '{}' and closing {} spawned window(s)", path, spawnedWindowList.size());
        Stage mainStage = boundContext.javafxStage();

        List<Stage> toClose = new ArrayList<>(spawnedWindowList);
        for (Stage stage : toClose) {
            destroyAndCloseStage(stage);
        }

        return resolveWithStage(path, mainStage);
    }

    @Override
    public Stage mainStage() {
        return boundContext.javafxStage();
    }

    private void destroyAndCloseStage(Stage stage) {
        log.debug("Destroying and closing stage");
        spawnedWindowList.remove(stage);
        ActiveScreen active = activeScreens.remove(stage);
        if (active != null) {
            active.ctx().scope().cancel();
            active.screen().onDestroy();
        }
        stage.close();
    }

    /* ---------------- internals ---------------- */

    private RouteResult resolveWithStage(String path, Stage stage) {
        log.debug("Resolving route '{}'", path);
        ResolvedRoute resolved = resolveRoute(path);
        Route route = resolved.route();
        log.debug("Route '{}' resolved to screen factory", path);

        ActiveScreen previous = activeScreens.get(stage);
        if (previous != null) {
            log.debug("Destroying previous screen on stage");
           previous.ctx().scope().cancel();
            previous.screen().onDestroy();
        }

        ScreenContext ctx = new ScreenContext(stage, this);
        ctx.setParams(resolved.params());

        ScreenComponent screen;
        try {
            screen = route.factory().create(ctx);
            log.debug("Screen created for route '{}'", path);
        } catch (Exception e) {
            log.error("Failed to create screen for route '{}'", path, e);
            ErrorReporter.handle(e);
            throw new RouteResolutionException(path, e);
        }

        activeScreens.put(stage, new ActiveScreen(screen, ctx));

        ComponentInterface<?> view = extractView(screen);
        log.debug("Calling onMount() for route '{}'", path);
        screen.onMount();
        return new RouteResult(view, route.props());
    }

    private ComponentInterface<?> extractView(Object screen) {

        if (screen instanceof ComponentInterface<?> view) {
            log.debug("Screen implements ComponentInterface directly");
            return view;
        }

        try {
            var method = screen.getClass().getMethod("render");
            Object result = method.invoke(screen);

            if (!(result instanceof ComponentInterface<?> component)) {
                var e = new IllegalStateException(
                        "render() de " + screen.getClass().getSimpleName()
                                + " deve retornar ComponentInterface"
                );
                log.error("render() did not return ComponentInterface for {}", screen.getClass().getSimpleName(), e);
                ErrorReporter.handle(e);
                throw e;
            }

            return component;
        } catch (NoSuchMethodException e) {
            var wrapped = new IllegalStateException(
                    "Screen " + screen.getClass().getSimpleName()
                            + " deve expor render() retornando ComponentInterface",
                    e
            );
            log.error("Screen {} does not expose render() returning ComponentInterface", screen.getClass().getSimpleName(), e);
            ErrorReporter.handle(wrapped);
            throw wrapped;
        } catch (Exception e) {
            var wrapped = new IllegalStateException(
                    "Falha ao invocar render() em " + screen.getClass().getSimpleName(),
                    e
            );
            log.error("Failed to invoke render() on {}", screen.getClass().getSimpleName(), e);
            ErrorReporter.handle(wrapped);
            throw wrapped;
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
                log.debug("Route matched: '{}' -> '{}'", path, route.identification());
                return new ResolvedRoute(route, params);
            }
        }

        log.warn("No route found for path '{}'", path);
        throw new RouteNotFoundException(path);
    }
}
