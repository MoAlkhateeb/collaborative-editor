import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;

    private SceneManager sceneManager;

    @Override
    public void start(Stage primaryStage) {
        this.sceneManager = new SceneManager(primaryStage);

        primaryStage.setTitle("Document Editor");

        sceneManager.showLandingPage();

        primaryStage.setWidth(DEFAULT_WIDTH);
        primaryStage.setHeight(DEFAULT_HEIGHT);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
