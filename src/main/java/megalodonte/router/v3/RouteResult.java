package megalodonte.router.v3;

import megalodonte.base.ComponentInterface;
import megalodonte.base.RouteProps;

public record RouteResult(
        ComponentInterface<?> view,
        RouteProps props
) {}
