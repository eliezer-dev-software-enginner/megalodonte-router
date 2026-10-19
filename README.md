# Megalodonte Router

Megalodonte Router is a routing library for **JavaFX desktop applications**.
This repository contains the v5 navigation adapter; route definitions,
screen contexts and screen lifecycle are provided by `megalodonte-base`.

It was created to solve a common pain point in JavaFX projects:
**navigation and screen management become messy very quickly** when
everything is handled manually with `Stage`, `Scene`, and imperative
navigation logic.

The router resolves paths through a `RouteTable` and mounts the resolved
screen on a JavaFX `Stage` through a `ScreenManager`.

> Since v5 the router is **navigation-only**: route storage (`RouteTable`),
> screen lifecycle (`ScreenManager`) and window spawning (`ctx.spawnWindow()`)
> live in `megalodonte-base`. This repository exports only
> `megalodonte.router.v5`.

------------------------------------------------------------------------

## Motivation

In traditional JavaFX applications:

-   Navigation logic is spread across multiple classes
-   Opening and closing windows requires repetitive boilerplate
-   Passing parameters between screens is awkward
-   There is no concept of dynamic routes (e.g. `/product/123`)

Megalodonte Router introduces:

-   Centralized route definitions
-   Dynamic route parameters
-   Window spawning and closing through `megalodonte-base`
-   Decoupled navigation logic
-   Screen lifecycle management through `megalodonte-base`

The goal is **simplicity, clarity, and control** --- without introducing
heavy frameworks.

------------------------------------------------------------------------

## Key Features

-   Navigation to static and dynamic routes defined in `megalodonte-base`
-   Route resolution and parameter extraction through `RouteTable`
-   Screen mounting and spawned-window management through `ScreenManager`
-   Screen APIs supplied by `megalodonte-base`, including `ctx.spawnWindow(...)` and `ctx.scope()`
-   Works with plain JavaFX or custom UI layers

------------------------------------------------------------------------

## Build requirements

The Gradle build requests a Java 25 toolchain and JavaFX 25.0.1. The included
Gradle wrapper uses Gradle 9.2.0. It resolves dependencies from Maven Central
and Maven Local, including `megalodonte:megalodonte-base:1.0.0-beta`.
That base artifact must be available in one of those repositories before the
project can be built; this repository does not include its source or a setup
procedure for it. With the dependency available, run `./gradlew build` from
the repository root on a system that can execute the wrapper script.

This is a library, not a standalone application. Its Gradle publication
coordinates are `megalodonte:megalodonte-router:1.0.0-beta`.

------------------------------------------------------------------------

## Defining Routes

Routes are defined in a single place using `Route` (from `megalodonte-base`).
Each route pairs an identifier with a `ScreenFactory` (`ctx -> new SomeScreen(ctx)`)
and a `RouteProps` describing the window:
`RouteProps(screenWidth, screenHeight, name, screenIsExpandable)`.

``` java
import megalodonte.base.route.Route;
import megalodonte.base.route.RouteProps;
import java.util.Set;

public class AppRoutes {

    public Set<Route> routes() {
        return Set.of(
            new Route(
                "home",
                ctx -> new HomeScreen(ctx),
                new RouteProps(1300, 700, null, false)
            ),
            new Route(
                "cad-produtos/${id}",
                ctx -> new ProdutoScreen(ctx),
                new RouteProps(1500, 900, "Cadastro de produtos", true)
            ),
            new Route(
                "detail",
                ctx -> new DetailScreen(ctx),
                new RouteProps(900, 700, null, true)
            )
        );
    }
}
```

Wiring it up in `Main`:

``` java
import megalodonte.base.route.RouteTable;
import megalodonte.router.v5.Router;

MegalodonteApp.run(AppHost.class, args, context -> {
    RouteTable table = new RouteTable(new AppRoutes().routes(), "home");
    context.useRouter(new Router(table)).start();
});
```

### Route Pattern

-   Static route: `home`
-   Dynamic route: `cad-produtos/${id}`

When navigating to:

    cad-produtos/123

`RouteTable` supplies the resolved parameters to `ScreenManager`. For this
example, the expected parameter is:

    id = "123"

------------------------------------------------------------------------

## Navigating Between Screens

Screens receive a context in their constructor — `ScreenContextInterface`
(from `megalodonte.base.route.v2`). Use it to navigate:

``` java
// Navigate within the current stage
ctx.navigate("cad-produtos/123");

// Open a new window for the route
ctx.spawnWindow("cad-produtos/123", error -> {
    // called if the route fails to resolve
});

// Close every spawned window and navigate the main stage back to a route
// (e.g. logout)
ctx.navigateAndCloseOthers("home");
```

This makes navigation explicit and predictable — no direct `Stage`/`Scene` handling in screens.

------------------------------------------------------------------------

## Receiving Route Parameters

Dynamic segments (`${id}`) are resolved into a `Map<String, String>` on the
context — read them with `ctx.getParams()`, typically in the screen's constructor:

``` java
import megalodonte.base.route.v2.ScreenContextInterface;

public class ProdutoScreen implements ScreenComponent {

    private final String id;

    public ProdutoScreen(ScreenContextInterface ctx) {
        this.id = ctx.getParams().get("id");
    }

    public Component render() {
        System.out.println("Product ID: " + id);
        return new Column(...);
    }
}
```

------------------------------------------------------------------------

## Error Handling

If a route cannot be resolved, `RouteTable` can throw:

``` java
RouteNotFoundException
```

This helps catch configuration or navigation errors early during
development.

------------------------------------------------------------------------

## Example Use Case

A home screen with menu items that open other features in their own window:

``` java
new Column(...)
    .c_child(
        new Clickable(icon)
            .onClick(() -> ctx.spawnWindow("cad-produtos/teste", e -> {}))
    );
```

Each item controls navigation without knowing anything about stages or scenes.

------------------------------------------------------------------------

## Screen-owned asynchronous work (`ctx.scope()`)

`ctx.scope()` belongs to `megalodonte-base`, not this router. The example
below illustrates using that API for screen-owned asynchronous work. Confirm
its cancellation and lifecycle behavior against the version of
`megalodonte-base` used by your application.

The following example registers cleanup for a resource opened asynchronously:

``` java
public class LiveDataScreen implements ScreenComponent {
    private final ScreenContextInterface ctx;

    public LiveDataScreen(ScreenContextInterface ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onMount() {
        ctx.scope().run(() -> {
            var connection = openConnection(); // may take a while
            ctx.scope().onCancel(connection::close);
            if (ctx.scope().isCancelled()) return; // screen already gone, don't start listening

            connection.listen(data -> UI.runOnUi(() -> /* update state */ null));
        });
    }
}
```

This pattern is intended to handle a resource that finishes opening after the
screen is destroyed. Its exact cleanup guarantees depend on the implementation
of `Scope` in `megalodonte-base`.

------------------------------------------------------------------------

## Design Philosophy

-   No magic
-   No reflection-heavy frameworks
-   Explicit navigation
-   Desktop-first mindset
-   Inspired by web routing, adapted for JavaFX

Megalodonte Router is meant to **empower**, not abstract away
everything.

------------------------------------------------------------------------

## Status

The Gradle project version is `1.0.0-beta`.

No license file is present in this repository; licensing terms require
confirmation from the project owner.
