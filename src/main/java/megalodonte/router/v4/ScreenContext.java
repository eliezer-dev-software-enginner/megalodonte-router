package megalodonte.router.v4;

import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;
import megalodonte.base.components.Component;
import megalodonte.base.route.RouteProps;
import megalodonte.base.route.RouteResult;
import megalodonte.base.route.ScreenContextInterface;
import megalodonte.base.scale.ScaleProvider;

import java.util.Map;
import java.util.function.Consumer;

public class ScreenContext implements ScreenContextInterface {
    private final Stage selfStage;
    private final Router router;
    private Map<String, String> params;

    private static final Duration TRANSITION_DURATION = Duration.millis(200);

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
        applyRouteResult(result, selfStage);
    }

    /**
     * Fecha todas as janelas spawned (inclusive a atual, se for uma spawned)
     * e navega a stage principal para o path informado. Uso: logout/"Sair",
     * garantindo retorno limpo à Auth independente de onde foi clicado.
     */
    public void navigateAndCloseOthers(String path) {
        RouteResult result = router.navigateAndCloseOthers(path);
        applyRouteResult(result, router.mainStage());
    }

    private void applyRouteResult(RouteResult result, Stage targetStage) {
        RouteProps props = result.props();
        Parent newRoot = (Parent) result.view().getJavaFxNode();

        Scene current = targetStage.getScene();
        if (current == null) {
            targetStage.setScene(new Scene(newRoot,
                    ScaleProvider.scale(props.screenWidth()),
                    ScaleProvider.scale(props.screenHeight())));
            applyStageProps(targetStage, props);
            return;
        }

        Parent oldRoot = current.getRoot();

        FadeTransition fadeOut = new FadeTransition(TRANSITION_DURATION, oldRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            newRoot.setOpacity(0.0);
            current.setRoot(newRoot);
            applyStageProps(targetStage, props);

            FadeTransition fadeIn = new FadeTransition(TRANSITION_DURATION, newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void applyStageProps(Stage stage, RouteProps props) {
        stage.setWidth(ScaleProvider.scale(props.screenWidth()));
        stage.setHeight(ScaleProvider.scale(props.screenHeight()));
        if (props.name() != null) {
            stage.setTitle(props.name());
        }
        if (props.iconPath() != null && !props.iconPath().isEmpty()) {
            stage.getIcons().add(new Image(props.iconPath()));
        }
        stage.setResizable(props.screenIsExpandable());
        stage.centerOnScreen();
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