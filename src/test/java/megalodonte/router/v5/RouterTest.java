package megalodonte.router.v5;

import megalodonte.base.route.RouteNotFoundException;
import megalodonte.base.route.Route;
import megalodonte.base.route.RouteProps;
import megalodonte.base.route.RouteTable;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RouterTest {
    @Test void unknownRouteFailsBeforeMountingAStage() {
        var router = new Router(new RouteTable(Set.of(), "home"));
        assertThrows(RouteNotFoundException.class, () -> router.navigateOnStage("missing", null));
    }

    @Test void unknownRouteFailsBeforeClosingOtherWindows() {
        var router = new Router(new RouteTable(Set.of(), "home"));
        assertThrows(RouteNotFoundException.class, () -> router.navigateAndCloseOthers("missing"));
    }

    @Test void validRouteRequiresBindingBeforeClosingOtherWindows() {
        var route = new Route("home", ctx -> null, new RouteProps(800, 600, null, false));
        var router = new Router(new RouteTable(Set.of(route), "home"));
        assertThrows(IllegalStateException.class, () -> router.navigateAndCloseOthers("home"));
    }
}
