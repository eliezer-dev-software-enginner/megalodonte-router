package megalodonte.router.v3;

import javafx.stage.Stage;

public record ScreenContext(
        Stage selfStage,
        Router router
) {

}
