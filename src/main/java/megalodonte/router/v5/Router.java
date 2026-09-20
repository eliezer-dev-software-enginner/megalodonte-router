package megalodonte.router.v5;

import javafx.stage.Stage;
import megalodonte.application.Context;
import megalodonte.base.route.RouteResult;
import megalodonte.base.route.RouteTable;
import megalodonte.base.route.RouterBase;
import megalodonte.base.route.ScreenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central routing manager responsible ONLY for navigation.
 *
 * <p>Route storage ({@link RouteTable}) and window spawning
 * ({@code megalodonte.base.route.v2.ScreenContextBase}) live in megalodonte-base.</p>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * RouteTable table = new RouteTable(new AppRoutes().routes(), AppRoutes.Screens.SPLASH.name());
 * context.useRouter(new Router(table)).start();
 *
 * // Inside a screen:
 * ctx.navigate("about");           // navigate the current window
 * ctx.spawnWindow("user/123");     // spawn a new window
 * }</pre>
 */
public final class Router implements RouterBase {
    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private final RouteTable table;
    private final ScreenManager screens;
    private Context boundContext;

    public Router(RouteTable table) {
        this.table = table;
        this.screens = new ScreenManager(table);
        log.info("Router initialized with {} route(s), entrypoint: '{}'", table.routes().size(), table.entrypoint());
    }

    @Override
    public void bind(Context context) {
        this.boundContext = context;
        log.debug("Router bound to application context");
    }

    @Override
    public RouteResult entrypoint() {
        Stage mainStage = boundContext != null ? boundContext.javafxStage() : null;
        return mount(mainStage, table.entrypoint());
    }

    @Override
    public RouteResult navigateOnStage(String path, Stage stage) {
        log.debug("Navigating to '{}' on stage", path);
        return mount(stage, path);
    }

    @Override
    public RouteResult navigateAndCloseOthers(String path) {
        log.info("Navigating to '{}' and closing all spawned windows", path);
        screens.closeAllSpawned();
        return mount(boundContext.javafxStage(), path);
    }

    @Override
    public Stage mainStage() {
        return boundContext.javafxStage();
    }

    /* ---------------- internals ---------------- */

    private RouteResult mount(Stage stage, String path) {
        log.debug("Resolving route '{}'", path);
        RouteTable.ResolvedRoute resolved = screens.resolve(path);
        return screens.mount(stage, resolved.route(), resolved.params(), this);
    }
}