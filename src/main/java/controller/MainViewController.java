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
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import utils.WrappedImageView;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

	@FXML
	private TextField entropyField;
	@FXML
	private GridPane inputPane;

	private WrappedImageView inputImageView;

	public void initialize(URL location, ResourceBundle resources) {
		initializeInputView();
	}

	private void initializeInputView() {
		inputImageView = new WrappedImageView();
		inputPane.getColumnConstraints().get(0).setHalignment(HPos.CENTER);
		inputPane.getRowConstraints().get(0).setValignment(VPos.CENTER);
		inputPane.add(inputImageView, 0, 0);
	}

	private void configureFileChooser(final FileChooser chooser) {
		chooser.setTitle("Select script");
		chooser.setInitialDirectory(
				new File(System.getProperty("user.home"))
		);
//		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PGM", "*.pgm"));
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
			Mat image = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
			Image loadedImage = matToImage(image);
			inputImageView.setImage(loadedImage);
		}
	}

}
