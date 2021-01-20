package com.keevol.keenotes.splash;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * splash with Preloader
 *
 * @author fq@keevol.com
 */
public class SplashScreenLoader extends Preloader {

    protected Logger logger = LoggerFactory.getLogger(SplashScreenLoader.class);

    private Stage splashScreen;

    @Override
    public void start(Stage stage) throws Exception {
        splashScreen = stage;
        splashScreen.setScene(createScene());
        splashScreen.initStyle(StageStyle.TRANSPARENT);
        splashScreen.show();
    }

    public Scene createScene() {
        StackPane root = new StackPane();
        ImageView imageView = new ImageView(new Image(getClass().getResource("/images/splash.png").toExternalForm()));
        root.getChildren().add(imageView);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add("/css/transparent.css");
        return scene;
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification notification) {
        super.handleStateChangeNotification(notification);
        logger.info("hide splash screen at StateChangeNotification: {}", notification);
        splashScreen.hide();
    }

    @Override
    public void handleProgressNotification(ProgressNotification info) {
        super.handleProgressNotification(info);
    }

    @Override
    public boolean handleErrorNotification(ErrorNotification info) {
        return super.handleErrorNotification(info);
    }
}