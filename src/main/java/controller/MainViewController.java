package controller;

import frequency.FrequencyData;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import prediction.Prediction;
import utils.HistogramMatrix;
import utils.WrappedImageView;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.opencv.core.Core.*;

public class MainViewController implements Initializable {

	public TextField codedDataLengthField;
	public TextField dataLengthField;
	public GridPane outputPane;
	public GridPane inputPane;
	public TextField entropyField;
	public TextField leftEntropyField;
	public TextField upperEntropyField;
	public TextField medianEntropyField;
	public Button histogramsButton;

	private WrappedImageView inputImageView;
	private WrappedImageView leftNeighbourView;
	private WrappedImageView upperNeighbourView;
	private WrappedImageView medianView;
	private Mat image;
	private Mat convertedToInt;
	private int[] imageData;

	private ExecutorService worker;
	private FrequencyData frequencyData;

	public void initialize(URL location, ResourceBundle resources) {
		initializeInputView();
		initializeOutputViews();
		worker = Executors.newSingleThreadExecutor();
		histogramsButton.setDisable(true);
	}

	private void initializeInputView() {
		inputImageView = new WrappedImageView();
		inputPane.getColumnConstraints().get(0).setHalignment(HPos.CENTER);
		inputPane.getRowConstraints().get(0).setValignment(VPos.CENTER);
		inputPane.add(inputImageView, 0, 0);
		ContextMenu contextMenu = new ContextMenu();
		MenuItem menuItem = new MenuItem("Zapisz dane");
		contextMenu.getItems().addAll(menuItem);
		inputImageView.setOnContextMenuRequested(event -> contextMenu.show(inputImageView, event.getScreenX(), event.getScreenY()));
	}

	private void initializeOutputViews() {
		leftNeighbourView = new WrappedImageView();
		upperNeighbourView = new WrappedImageView();
		medianView = new WrappedImageView();
		outputPane.add(leftNeighbourView, 0, 0);
		outputPane.add(upperNeighbourView, 0, 1);
		outputPane.add(medianView, 0, 2);
		configureImageSavingMenu(leftNeighbourView);
		configureImageSavingMenu(upperNeighbourView);
		configureImageSavingMenu(medianView);
	}

	private void configureImageSavingMenu(WrappedImageView imageView) {
		ContextMenu saveMenu = new ContextMenu();
		MenuItem item = new MenuItem("Zapisz jako PNG");
		item.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save Image");
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PNG", "*.png"));
			File file = fileChooser.showSaveDialog(imageView.getScene().getWindow());
			if (file != null) {
				try {
					ImageIO.write(SwingFXUtils.fromFXImage(imageView.getImage(), null), "png", file);
				} catch (IOException e) {
					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Błąd zapisu");
					alert.setHeaderText("Nieudana próba zapisu");
					alert.setContentText(String.format("Nie udało się zapisać do pliku [%s]", file.getAbsolutePath()));
					alert.showAndWait();
				}
			}
		});
		saveMenu.getItems().addAll(item);
		imageView.setOnContextMenuRequested(event -> saveMenu.show(imageView, event.getScreenX(), event.getScreenY()));
	}

	private void configureFileChooser(final FileChooser chooser) {
		chooser.setTitle("Select image");
		chooser.setInitialDirectory(
				new File(System.getProperty("user.home"))
		);
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PGM", "*.pgm"));
	}

	private Image differentialImage(int[] source) {
		Mat img = new Mat(image.cols(), image.rows(), CvType.CV_32S);
		img.put(0, 0, source);
		MinMaxLocResult res = Core.minMaxLoc(img);
		double maxVal = res.maxVal, minVal = res.minVal;
		img.convertTo(img, CvType.CV_8UC1, 255.0 / (maxVal - minVal), -minVal * 255.0 / (maxVal - minVal));
		return matToImage(img);
	}

	private Image matToImage(Mat original) {
		return SwingFXUtils.toFXImage(matToBufferedImage(original), null);
	}

	private BufferedImage matToBufferedImage(Mat original) {
		BufferedImage image;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		if (original.channels() > 1) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		return image;
	}

	private void calcPredictions() {
		frequencyData = new FrequencyData();
		int[] leftPrediction = Prediction.leftNeighbour(imageData, image.width(), image.height());
		int[] upperPrediction = Prediction.upperNeighbour(imageData, image.width(), image.height());
		int[] medianPrediction = Prediction.leftAndUpperNeighbourMedian(imageData, image.width(), image.height());
		frequencyData.setAll(imageData, leftPrediction, upperPrediction, medianPrediction);
	}

	private void setEntropy() {
		entropyField.setText(Double.toString(frequencyData.getImageFrequency().entropy()));
		leftEntropyField.setText(Double.toString(frequencyData.getLeftPredictionFrequency().entropy()));
		upperEntropyField.setText(Double.toString(frequencyData.getUpperPredictionFreqency().entropy()));
		medianEntropyField.setText(Double.toString(frequencyData.getMedianPredictionFrequency().entropy()));
	}

	private void readImage(File file) {
		histogramsButton.setDisable(true);
		image = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
		Image loadedImage = matToImage(image);
		inputImageView.setImage(loadedImage);
		dataLengthField.setText(Integer.toString(image.width() * image.height()));
		convertedToInt = new Mat();
		image.convertTo(convertedToInt, CvType.CV_32S);
		imageData = new int[image.width() * image.height()];
		convertedToInt.get(0, 0, imageData);
	}

	public void showHistograms(ActionEvent event) {
		Stage stage = new Stage();
		List<double[]> values = frequencyData.getDoubles();
		HistogramMatrix matrix = HistogramMatrix.newHistogramMatrix()
				.withULChart("Oryginalny", values.get(0), 0.0, 1.0, Color.BLUE)
				.withURChart("Lewy-sasiad", values.get(1), -1.0, 1.0, Color.ORANGE)
				.withLLChart("Gorny-sasiad", values.get(2), -1.0, 1.0, Color.CYAN)
				.withLRChart("Mediana", values.get(3), -1.0, 1.0, Color.GREEN);
		stage.setScene(new Scene(matrix));
		stage.setTitle("Histogramy");
		stage.setWidth(800);
		stage.setHeight(600);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(((Node) event.getSource()).getScene().getWindow());
		stage.show();
	}

	public void setClosingHandler() {
		entropyField.getScene().getWindow().setOnCloseRequest(event -> {
			worker.shutdown();
			try {
				if (!worker.awaitTermination(5000, TimeUnit.MILLISECONDS))
					worker.shutdownNow();
			} catch (InterruptedException e) {
				worker.shutdownNow();
			}
		});
	}

	public void loadImage(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		configureFileChooser(fileChooser);
		File file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
		if (file != null) {
			readImage(file);
			worker.execute(() -> {
				calcPredictions();
				Platform.runLater(() -> {
					leftNeighbourView.setImage(differentialImage(frequencyData.getLeftPrediction()));
					upperNeighbourView.setImage(differentialImage(frequencyData.getUpperPrediction()));
					medianView.setImage(differentialImage(frequencyData.getMedianPrediction()));
					setEntropy();
					histogramsButton.setDisable(false);
				});
			});
		}
	}

}
