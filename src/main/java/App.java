import controller.MainViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencv.core.Core;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class App extends Application {

	public static final String MAIN_VIEW_URL = "/views/MainView.fxml";
	public static final String WINDOW_TITLE = "Kodowanie predykcyjne";

	static{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_VIEW_URL));
		Parent root = loader.load();
		primaryStage.setTitle(WINDOW_TITLE);
		primaryStage.setScene(new Scene(root));
		primaryStage.show();
		MainViewController controller = loader.getController();
		controller.setClosingHandler();
	}

	public static void main(String[] args) {
		for (String arg : args)
			System.out.println(arg);
		launch(args);
	}

}
