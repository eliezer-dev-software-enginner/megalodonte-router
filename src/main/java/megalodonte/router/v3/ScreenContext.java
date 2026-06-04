package megalodonte.router.v3;

import javafx.stage.Stage;
import megalodonte.base.route.ScreenContextInterface;

@Deprecated(forRemoval = true,  since = "04/jun/2026")
public record ScreenContext(
        Stage selfStage,
        Router router
) implements ScreenContextInterface { }
