# Megalodonte Router

Megalodonte Router is a lightweight routing library designed
specifically for **JavaFX desktop applications**.

It was created to solve a common pain point in JavaFX projects:
**navigation and screen management become messy very quickly** when
everything is handled manually with `Stage`, `Scene`, and imperative
navigation logic.

This Router brings a **route-based navigation model**, inspired by web
frameworks, while staying fully compatible with JavaFX and custom UI
frameworks.

------------------------------------------------------------------------

## Motivation

In traditional JavaFX applications:

-   Navigation logic is spread across multiple classes
-   Opening and closing windows requires repetitive boilerplate
-   Passing parameters between screens is awkward
-   There is no concept of dynamic routes (e.g. `/product/123`)

Megalodonte Router introduces:

-   Centralized route definitions
-   Dynamic route parameters
-   Automatic window spawning and closing
-   Decoupled navigation logic
-   Clean and predictable screen lifecycle

The goal is **simplicity, clarity, and control** --- without introducing
heavy frameworks.

------------------------------------------------------------------------

## Key Features

-   Static and dynamic routes (`/products/${id}`)
-   Route parameter extraction via `ctx.getParams()`
-   Per-route window configuration (size, title)
-   Spawn and close windows programmatically
-   Automatic cancellation of in-flight async work on navigation (`ctx.scope()`)
-   Works with plain JavaFX or custom UI layers

------------------------------------------------------------------------

## Defining Routes

Routes are defined in a single place using `Router.Route`. Each route pairs an identifier with a
`ScreenFactory` (`ctx -> new SomeScreen(ctx)`) and a `RouteProps` (from `megalodonte-base`)
describing the window: `RouteProps(screenWidth, screenHeight, name, screenIsExpandable)`.

``` java
public class AppRoutes {

    public Set<Router.Route> routes() {
        return Set.of(
            new Router.Route(
                "home",
                ctx -> new HomeScreen(ctx),
                new RouteProps(1300, 700, null, false)
            ),
            new Router.Route(
                "cad-produtos/${id}",
                ctx -> new ProdutoScreen(ctx),
                new RouteProps(1500, 900, "Cadastro de produtos", true)
            ),
            new Router.Route(
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
MegalodonteApp.run(AppHost.class, args, context -> {
    Router router = new Router(new AppRoutes().routes(), "home");
    context.useRouter(router).start();
});
```

### Route Pattern

-   Static route: `home`
-   Dynamic route: `cad-produtos/${id}`

When navigating to:

    cad-produtos/123

The router automatically extracts:

    id = "123"

------------------------------------------------------------------------

## Navigating Between Screens

Screens receive a `ScreenContext ctx` in their constructor. Use it to navigate:

``` java
// Navigate within the current stage
ctx.navigate("cad-produtos/123");

// Open a new window for the route
ctx.router().spawnWindow("cad-produtos/123", error -> {
    // called if the route fails to resolve
});

// Close every spawned window and navigate the main stage back to a route
// (e.g. logout)
ctx.navigateAndCloseOthers("home");
```

This makes navigation explicit and predictable — no direct `Stage`/`Scene` handling in screens.

------------------------------------------------------------------------

## Receiving Route Parameters

Dynamic segments (`${id}`) are resolved into a `Map<String, String>` on the `ScreenContext` —
read them with `ctx.getParams()`, typically in the screen's constructor:

``` java
public class ProdutoScreen implements ScreenComponent {

    private final String id;

    public ProdutoScreen(ScreenContext ctx) {
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

If a route cannot be resolved, the router throws:

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
            .onClick(() -> ctx.router().spawnWindow("cad-produtos/teste", e -> {}))
    );
```

Each item controls navigation without knowing anything about stages or scenes.

------------------------------------------------------------------------

## Automatic Cancellation on Navigation (`ctx.scope()`)

Every `ScreenContext` (v4) owns a `Scope` (from `megalodonte-base`), created fresh for that
navigation. The router cancels it automatically at every teardown point — main-stage
navigation, spawned window close, `navigateAndCloseOthers` — right before calling the screen's
`onDestroy()`. Screens don't have to do anything to get this; it's just there.

Use it instead of a raw `Async.Run()` for anything that opens a resource which needs to be
closed if the user navigates away before it finishes setting up:

``` java
public class LiveDataScreen implements ScreenComponent {
    private final ScreenContext ctx;

    public LiveDataScreen(ScreenContext ctx) {
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

Without this, a screen that opens something asynchronously in `onMount` and closes it in
`onDestroy` has a race: if `onDestroy` runs before the resource finishes opening, the
`null`-check guard in `onDestroy` finds nothing to close, and the resource — plus everything its
callback closures keep alive — leaks for as long as the app runs. `ctx.scope()` closes that
window: the cleanup is guaranteed to fire, whether the resource finishes opening before or after
the screen is destroyed.

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

This library is under active development and evolving alongside
real-world desktop applications.

Feedback, ideas, and improvements are welcome.
