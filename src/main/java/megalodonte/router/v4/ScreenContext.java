package megalodonte.router.v4;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import megalodonte.base.route.RouteProps;
import megalodonte.base.route.RouteResult;
import megalodonte.base.route.ScreenContextInterface;

import java.util.Map;
import java.util.function.Consumer;

public class ScreenContext implements ScreenContextInterface {
    private final Stage selfStage;
    private final Router router;
    private Map<String, String> params;

    public ScreenContext(Stage selfStage, Router router){
        this.selfStage = selfStage;

        this.router = router;
    }

    /**
     * Navigates to the given route path within this screen's stage.
     *
     * <p>Mutates the existing {@link javafx.scene.Scene} root instead of creating a new Scene,
     * preserving window listeners and state. After navigation, applies the destination
     * route's {@link RouteProps} (dimensions, title, resizability) to the stage.</p>
     *
     * <p>If no Scene exists yet on the stage, a new one is created with the route's dimensions.</p>
     *
     * @param path the route identification to navigate to (e.g. "home", "user/${id}")
     * @throws megalodonte.router.RouteNotFoundException if no route matches the given path
     */
    public void navigate(String path) {
        RouteResult result = router.navigateOnStage(path, selfStage);
        RouteProps props = result.props();
        Parent parent = (Parent) result.view().getJavaFxNode();

        Scene current = selfStage.getScene();
        if (current != null) {
            current.setRoot(parent);
        } else {
            selfStage.setScene(new Scene(parent, props.screenWidth(), props.screenHeight()));
        }

        selfStage.setWidth(props.screenWidth());
        selfStage.setHeight(props.screenHeight());
        if (props.name() != null) {
            selfStage.setTitle(props.name());
        }
        selfStage.setResizable(props.screenIsExpandable());
        selfStage.centerOnScreen();
    }

//    /**
//     * Navega dentro da stage desta tela — nunca afeta a stage principal
//     * nem outras janelas spawned.
//     */
//    public void navigate(String path) {
//        RouteResult result = router.navigateOnStage(path, selfStage);
//
//        Parent parent = (Parent) result.view().getJavaFxNode();
//
//        Scene current = selfStage.getScene();
//        if (current != null) {
//            current.setRoot(parent);
//        } else {
//            RouteProps props = result.props();
//            selfStage.setScene(new Scene(parent, props.screenWidth(), props.screenHeight()));
//        }
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

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Stage selfStage(){
        return this.selfStage;
    }

    public Router router(){
        return this.router;
    }

    public Map<String, String> getParams() {
        return params;
    }
}