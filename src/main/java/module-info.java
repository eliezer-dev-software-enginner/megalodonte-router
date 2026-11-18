/** Provides navigation and window routing for Megalodonte applications. */
module megalodonte.router.desktop {
    requires transitive megalodonte.base.desktop;

    requires transitive javafx.graphics;

    requires org.slf4j;

    exports megalodonte.router.v5;
}
