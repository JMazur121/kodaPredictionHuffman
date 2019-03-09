package controller;

import frequency.FrequencyMap;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.data.statistics.HistogramDataset;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import prediction.Prediction;
import utils.WrappedImageView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.opencv.core.Core.*;

public class MainViewController implements Initializable {

	public static final int BINS = 50;
	public static final String X_LABEL = "Wartosci probek";
	public static final String Y_LABEL = "Czestosc";

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

	private ExecutorService worker;

	private int[] imageData;
	private int[] leftPrediction;
	private int[] upperPrediction;
	private int[] medianPrediction;

	private FrequencyMap imageFrequency;
	private FrequencyMap leftPredictionFrequency;
	private FrequencyMap upperPredictionFreqency;
	private FrequencyMap medianPredictionFrequency;

	private List<JFreeChart> histograms;

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

	private void buildHistogram(String title, int[] values, boolean pred, Color color) {
		HistogramDataset dataset = new HistogramDataset();
		double[] vals = new double[values.length];
		int idx = 0;
		for (int i : values) {
			vals[idx++] = i / 255.0;
		}
		if (pred)
			dataset.addSeries("", vals, BINS, -1.0, 1.0);
		else
			dataset.addSeries("", vals, BINS, 0.0, 1.0);
		JFreeChart histogram = ChartFactory.createHistogram(title, X_LABEL, Y_LABEL, dataset);
		histogram.getXYPlot().getRenderer().setSeriesPaint(0, color);
		histograms.add(histogram);
	}

	private void buildAllHistograms() {
		buildHistogram("Oryginalny", imageData, false, Color.BLUE);
		buildHistogram("Lewy-sasiad", leftPrediction, true, Color.ORANGE);
		buildHistogram("Gorny-sasiad", upperPrediction, true, Color.DARK_GRAY);
		buildHistogram("Mediana", medianPrediction, true, Color.LIGHT_GRAY);
	}

	public void showHistograms(ActionEvent event) {
		ChartViewer image, left, upper, median;
		image = new ChartViewer(histograms.get(0));
		left = new ChartViewer(histograms.get(1));
		upper = new ChartViewer(histograms.get(2));
		median = new ChartViewer(histograms.get(3));
		Stage stage = new Stage();
		GridPane gridpane = new GridPane();
		ColumnConstraints column1 = new ColumnConstraints();
		ColumnConstraints column2 = new ColumnConstraints();
		column1.setPercentWidth(50);
		column2.setPercentWidth(50);
		gridpane.getColumnConstraints().addAll(column1, column2);
		RowConstraints row1 = new RowConstraints();
		RowConstraints row2 = new RowConstraints();
		row1.setPercentHeight(50);
		row2.setPercentHeight(50);
		gridpane.getRowConstraints().addAll(row1, row2);
		gridpane.add(image, 0, 0);
		gridpane.add(left, 1, 0);
		gridpane.add(upper, 0, 1);
		gridpane.add(median, 1, 1);
		stage.setScene(new Scene(gridpane));
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
			histogramsButton.setDisable(true);
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
				histograms = new ArrayList<>();
				buildAllHistograms();
				Platform.runLater(() -> {
					leftNeighbourView.setImage(differentialImage(leftPrediction));
					upperNeighbourView.setImage(differentialImage(upperPrediction));
					medianView.setImage(differentialImage(medianPrediction));
					setEntropy(imgEntropy, leftEntropy, upperEntropy, medianEntropy);
					histogramsButton.setDisable(false);
				});
			});
		}
	}

}
