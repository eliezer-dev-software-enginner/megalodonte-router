package megalodonte.router.v2;

import megalodonte.application.View;
import megalodonte.base.RouteProps;

public record RouteResult(
        View view,
        RouteProps props
) {}
