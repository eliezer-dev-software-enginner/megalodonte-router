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
-   Route parameter extraction
-   Per-route window configuration (size, title)
-   Spawn and close windows programmatically
-   Optional awareness of route parameters via interface
-   Works with plain JavaFX or custom UI layers

------------------------------------------------------------------------

## Defining Routes

Routes are defined in a single place using the `Router.Route` class.

``` java
public class AppRoutes {

    public Router defineRoutes(Stage stage) throws ReflectiveOperationException {

        var routes = Set.of(
            new Router.Route(
                "home",
                router -> new HomeScreen(router),
                new Router.RouteProps(1300, 700, null)
            ),
            new Router.Route(
                "cad-produtos/${id}",
                router -> new ProdutoScreen(router),
                new Router.RouteProps(1500, 900, "Cadastro de produtos")
            ),
            new Router.Route(
                "detail",
                router -> new DetailScreen(router),
                new Router.RouteProps(900, 700, null)
            )
        );

        return new Router(routes, "home", stage);
    }
}
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

To open a new route:

``` java
router.spawnWindow("cad-produtos/123");
```

To close the current spawned window and optionally return to another
route:

``` java
router.closeSpawn("home");
```

This makes navigation explicit and predictable.

------------------------------------------------------------------------

## Receiving Route Parameters

Screens that need access to route parameters simply implement
`RouteParamsAware`.

``` java
public class ProdutoScreen implements RouteParamsAware {

    private String id;

    @Override
    public void onRouteParams(Map<String, String> params) {
        this.id = params.get("id");
    }

    public Component render() {
        System.out.println("Product ID: " + id);
        return new Column(...);
    }
}
```

This keeps constructors clean and avoids tight coupling with the router.

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

A home screen with cards that navigate to different features:

``` java
new Column(...)
    .c_child(
        new Clickable(icon)
            .onClick(() -> router.spawnWindow("cad-produtos/teste"))
    );
```

Each card controls navigation without knowing anything about stages or
scenes.

------------------------------------------------------------------------

## Smart Navigation (New Feature)

The router now supports intelligent navigation that automatically targets the currently active stage:

### Active Stage Management
``` java
// Get current active stage
Stage activeStage = router.getCurrentActiveStage();

// Focus back to main stage
router.focusMainStage();
```

### Smart Navigation Behavior
``` java
// Start with main stage active
Router router = new Router(routes, "home", mainStage);

// Navigate on main window
router.navigateTo("about"); // Uses main stage

// Spawn new window (automatically becomes active)
router.spawnWindow("user/123");

// Navigation now targets spawned window
router.navigateTo("profile"); // Navigates in spawned window, not main!

// Return to main stage
router.focusMainStage();
router.navigateTo("settings"); // Back to main stage
```

### Use Cases

**Multi-Window Applications:**
- Spawn independent windows for different workflows
- Navigation automatically follows user focus
- Easy switching between windows

**Tabbed Interfaces:**
- Each tab can have its own navigation context
- Router tracks which tab is active
- Navigation happens in the correct context

**Dialog-based Flows:**
- Open modal windows for specific tasks
- Navigation stays within the dialog
- Return to parent when dialog closes

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
