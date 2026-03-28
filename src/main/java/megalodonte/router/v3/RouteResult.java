package megalodonte.router.v3;

import megalodonte.application.View;
import megalodonte.base.RouteProps;

public record RouteResult(
        View view,
        RouteProps props
) {}
