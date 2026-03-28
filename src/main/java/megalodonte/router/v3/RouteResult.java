package megalodonte.router.v3;


import megalodonte.base.components.ComponentInterface;

public record RouteResult(
        ComponentInterface<?> view,
        RouteProps props
) {}
