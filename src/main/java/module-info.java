/** Provides navigation and window routing for Megalodonte applications. */
module megalodonte.router {
    requires transitive megalodonte.base;

    requires transitive javafx.graphics;

    requires org.slf4j;

    exports megalodonte.router.v5;
}
