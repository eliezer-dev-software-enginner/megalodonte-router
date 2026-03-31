package megalodonte.router.v4;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import megalodonte.base.route.ScreenContextInterface;

public record ScreenContext(
        Stage selfStage,
        Router router
) implements ScreenContextInterface {

    /**
     * Navega dentro da stage desta tela — nunca afeta a stage principal
     * nem outras janelas spawned.
     */
    public void navigate(String path) {
        RouteResult result = router.navigateOnStage(path, selfStage);
        RouteProps props = result.props();

        Parent parent = (Parent) result.view().getJavaFxNode();
        selfStage.setScene(new Scene(parent, props.screenWidth(), props.screenHeight()));
    }
}