package megalodonte.router.v5;

import megalodonte.base.components.ScreenComponent;

@FunctionalInterface
public interface ScreenFactory {
    ScreenComponent create(ScreenContext ctx) throws Exception;
}