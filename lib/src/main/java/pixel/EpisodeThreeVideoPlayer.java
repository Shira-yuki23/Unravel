package pixel;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Standalone JavaFX window used by EpisodeThreeVideo. */
public final class EpisodeThreeVideoPlayer extends Application {

    private MediaPlayer player;
    private final AtomicBoolean closing = new AtomicBoolean();

    @Override
    public void start(Stage stage) {
        Parameters parameters = getParameters();
        if (parameters.getRaw().size() < 2) {
            System.err.println("Video source and number are required.");
            Platform.exit();
            return;
        }

        String source = parameters.getRaw().get(0);
        String number = parameters.getRaw().get(1);

        try {
            Media media = new Media(source);
            player = new MediaPlayer(media);
            MediaView view = new MediaView(player);
            view.setPreserveRatio(true);

            StackPane root = new StackPane(view);
            root.setStyle("-fx-background-color: black;");
            Scene scene = new Scene(root, 1280, 720, Color.BLACK);
            view.fitWidthProperty().bind(scene.widthProperty());
            view.fitHeightProperty().bind(scene.heightProperty());

            stage.setTitle("end".equals(number) ? "UNRAVEL - Ending" : "Unravel - Memory " + number);
            stage.setScene(scene);
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setFullScreenExitHint("ESC: skip video");

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    event.consume();
                    finish(0);
                }
            });
            stage.setOnCloseRequest(event -> {
                event.consume();
                finish(0);
            });
            media.setOnError(() -> {
                System.err.println(media.getError());
                finish(2);
            });
            player.setOnError(() -> {
                System.err.println(player.getError());
                finish(2);
            });
            player.setOnEndOfMedia(() -> finish(0));

            stage.show();
            stage.setFullScreen(true);
            stage.toFront();
            stage.requestFocus();
            player.play();
        } catch (Exception error) {
            error.printStackTrace();
            finish(2);
        }
    }

    private void finish(int exitCode) {
        if (!closing.compareAndSet(false, true)) {
            return;
        }
        if (player != null) {
            player.stop();
            player.dispose();
        }
        Platform.exit();
        System.exit(exitCode);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
