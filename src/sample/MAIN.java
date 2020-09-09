package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MAIN extends Application {

    public static Stage MainWindow;

    @Override
    public void start(Stage primaryStage) throws Exception{
        MainWindow=primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("init_window.fxml"));
        primaryStage.setTitle("ImageRecognition");
        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
