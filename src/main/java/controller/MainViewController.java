package controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import prediction.PredictionService;
import utils.WrappedImageView;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import static org.opencv.core.Core.*;

public class MainViewController implements Initializable {

	@FXML
	private TextField codedDataLengthField;
	@FXML
	private TextField dataLengthField;
	@FXML
	private GridPane outputPane;
	@FXML
	private TextField entropyField;
	@FXML
	private GridPane inputPane;

	private WrappedImageView inputImageView;
	private WrappedImageView leftNeighbourView;
	private WrappedImageView upperNeighbourView;
	private WrappedImageView medianView;
	private PredictionService predictionService;
	private Mat image;
	private Mat convertedToInt;

	private int[] imageData;
	private int[] leftPrediction;
	private int[] upperPrediction;
	private int[] medianPrediction;

	public void initialize(URL location, ResourceBundle resources) {
		initializeInputView();
		initializeOutputViews();
		predictionService = new PredictionService();
		predictionService.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				leftPrediction = newValue.get(0);
				upperPrediction = newValue.get(1);
				medianPrediction = newValue.get(2);
				leftNeighbourView.setImage(differentialImage(leftPrediction));
				upperNeighbourView.setImage(differentialImage(upperPrediction));
				medianView.setImage(differentialImage(medianPrediction));
			}
		});
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

	private void configureFileChooser(final FileChooser chooser) {
		chooser.setTitle("Select script");
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

	public void loadImage(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		configureFileChooser(fileChooser);
		File file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
		if (file != null) {
			image = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
			Image loadedImage = matToImage(image);
			inputImageView.setImage(loadedImage);
			dataLengthField.setText(Integer.toString(image.width() * image.height()) + " bytes");
			convertedToInt = new Mat();
			image.convertTo(convertedToInt, CvType.CV_32S);
			imageData = new int[image.width() * image.height()];
			convertedToInt.get(0, 0, imageData);
			predictionService.setSource(imageData);
			predictionService.setWidth(image.width());
			predictionService.setHeight(image.height());
			predictionService.restart();
		}
	}

}
