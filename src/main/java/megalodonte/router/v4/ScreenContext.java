package megalodonte.router.v4;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import megalodonte.base.route.ScreenContextInterface;

import java.util.function.Consumer;

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

        Parent parent = (Parent) result.view().getJavaFxNode();

        Scene current = selfStage.getScene();
        if (current != null) {
            current.setRoot(parent);
        } else {
            RouteProps props = result.props();
            selfStage.setScene(new Scene(parent, props.screenWidth(), props.screenHeight()));
        }
    }
//    public void navigate(String path) {
//        RouteResult result = router.navigateOnStage(path, selfStage);
//        RouteProps props = result.props();
//
//        Parent parent = (Parent) result.view().getJavaFxNode();
//        selfStage.setScene(new Scene(parent, props.screenWidth(), props.screenHeight()));
//    }

    /**
     * Executa o callback quando a Scene estiver pronta na Stage.
     * Cobre tanto o caso onde ela ainda não existe (aguarda) quanto
     * o caso onde já está disponível (executa imediatamente).
     */
    public void whenReady(Consumer<Scene> callback) {
        Scene current = selfStage.getScene();
        if (current != null) {
            callback.accept(current);
            return;
        }

        selfStage.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) callback.accept(newScene);
        });
    }
}