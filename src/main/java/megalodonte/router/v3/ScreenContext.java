package megalodonte.router.v3;

import javafx.stage.Stage;
import megalodonte.base.route.ScreenContextInterface;

public record ScreenContext(
        Stage selfStage,
        Router router
) implements ScreenContextInterface { }
