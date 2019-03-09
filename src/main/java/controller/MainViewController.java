package controller;

import frequency.FrequencyMap;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import prediction.Prediction;
import utils.WrappedImageView;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
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

	private WrappedImageView inputImageView;
	private WrappedImageView leftNeighbourView;
	private WrappedImageView upperNeighbourView;
	private WrappedImageView medianView;
	private Mat image;
	private Mat convertedToInt;

	private ExecutorService worker;

	private int[] imageData;
	private int[] leftPrediction;
	private int[] upperPrediction;
	private int[] medianPrediction;

	private FrequencyMap imageFrequency;
	private FrequencyMap leftPredictionFrequency;
	private FrequencyMap upperPredictionFreqency;
	private FrequencyMap medianPredictionFrequency;

	public void initialize(URL location, ResourceBundle resources) {
		initializeInputView();
		initializeOutputViews();
		worker = Executors.newSingleThreadExecutor();
	}

	private void initializeInputView() {
		inputImageView = new WrappedImageView();
		inputPane.getColumnConstraints().get(0).setHalignment(HPos.CENTER);
		inputPane.getRowConstraints().get(0).setValignment(VPos.CENTER);
		inputPane.add(inputImageView, 0, 0);
	}

	private void initializeOutputViews() {
		leftNeighbourView = new WrappedImageView();
		upperNeighbourView = new WrappedImageView();
		medianView = new WrappedImageView();
		outputPane.add(leftNeighbourView, 0, 0);
		outputPane.add(upperNeighbourView, 0, 1);
		outputPane.add(medianView, 0, 2);
	}

	private void initializeFrequencyMaps() {
		imageFrequency = new FrequencyMap(imageData);
		leftPredictionFrequency = new FrequencyMap(leftPrediction);
		upperPredictionFreqency = new FrequencyMap(upperPrediction);
		medianPredictionFrequency = new FrequencyMap(medianPrediction);
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
		leftPrediction = Prediction.leftNeighbour(imageData, image.width(), image.height());
		upperPrediction = Prediction.upperNeighbour(imageData, image.width(), image.height());
		medianPrediction = Prediction.leftAndUpperNeighbourMedian(imageData, image.width(), image.height());
		initializeFrequencyMaps();
	}

	private void setEntropy(double img, double left, double upper, double median) {
		entropyField.setText(Double.toString(img));
		leftEntropyField.setText(Double.toString(left));
		upperEntropyField.setText(Double.toString(upper));
		medianEntropyField.setText(Double.toString(median));
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
			image = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
			Image loadedImage = matToImage(image);
			inputImageView.setImage(loadedImage);
			dataLengthField.setText(Integer.toString(image.width() * image.height()));
			convertedToInt = new Mat();
			image.convertTo(convertedToInt, CvType.CV_32S);
			imageData = new int[image.width() * image.height()];
			convertedToInt.get(0, 0, imageData);
			worker.execute(() -> {
				calcPredictions();
				double imgEntropy, leftEntropy, upperEntropy, medianEntropy;
				imgEntropy = imageFrequency.entropy();
				leftEntropy = leftPredictionFrequency.entropy();
				upperEntropy = upperPredictionFreqency.entropy();
				medianEntropy = medianPredictionFrequency.entropy();
				Platform.runLater(() -> {
					leftNeighbourView.setImage(differentialImage(leftPrediction));
					upperNeighbourView.setImage(differentialImage(upperPrediction));
					medianView.setImage(differentialImage(medianPrediction));
					setEntropy(imgEntropy, leftEntropy, upperEntropy, medianEntropy);
				});
			});
		}
	}

}
