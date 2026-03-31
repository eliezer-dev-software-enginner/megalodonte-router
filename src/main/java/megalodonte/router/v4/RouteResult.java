package megalodonte.router.v4;


import megalodonte.base.components.ComponentInterface;

public record RouteResult(
        ComponentInterface<?> view,
        RouteProps props
) {}
