import frequency.FrequencyData;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import prediction.Prediction;
import tree.HuffmanTree;
import java.util.Map;
import java.util.StringJoiner;

public class CmdApp {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) {
		System.out.println("Argumentów : " + args.length);
		if (args.length < 1) {
			System.out.println("Za mało argumentów");
			System.exit(1);
		}
		for (String arg : args) {
			Mat image = Imgcodecs.imread(arg, Imgcodecs.IMREAD_GRAYSCALE);
			if (image.empty())
				System.out.println("Nie udało się odczytać obrazu \"" + arg + "\"");
			else {
				System.out.println(String.format("Odczytano obraz [%s] o wymiarach %dx%d", arg, image.width(), image.height()));
				Mat convertedToInt = new Mat();
				image.convertTo(convertedToInt, CvType.CV_32S);
				int[] imageData = new int[image.width() * image.height()];
				convertedToInt.get(0, 0, imageData);
				FrequencyData frequencyData = new FrequencyData();
				calcPredictions(frequencyData, imageData, image);
				int[] codedSizes = new int[4];
				calcHuffmanCodes(frequencyData, codedSizes);
				StringJoiner joiner = new StringJoiner("\t", "[", "]");
				joiner.add(arg)
						.add(Integer.toString(imageData.length))
						.add(String.format("%.3f", frequencyData.getImageFrequency().entropy()))
						.add(Integer.toString(codedSizes[0]))
						.add(Integer.toString(100 * codedSizes[0] / imageData.length))
						.add(String.format("%.3f", frequencyData.getLeftPredictionFrequency().entropy()))
						.add(String.format("%.2f",(float)(codedSizes[1] * 8) / imageData.length))
						.add(Integer.toString(codedSizes[1]))
						.add(Integer.toString(100 * codedSizes[1] / imageData.length))
						.add(String.format("%.3f", frequencyData.getUpperPredictionFreqency().entropy()))
						.add(String.format("%.2f",(float)(codedSizes[2] * 8) / imageData.length))
						.add(Integer.toString(codedSizes[2]))
						.add(Integer.toString(100 * codedSizes[2] / imageData.length))
						.add(String.format("%.3f", frequencyData.getMedianPredictionFrequency().entropy()))
						.add(String.format("%.2f",(float)(codedSizes[3] * 8) / imageData.length))
						.add(Integer.toString(codedSizes[3]))
						.add(Integer.toString(100 * codedSizes[3] / imageData.length));
				System.out.println(joiner.toString());
			}
		}
	}

	private static void calcPredictions(FrequencyData frequencyData, int[] imageData, Mat image) {
		int[] leftPrediction = Prediction.leftNeighbour(imageData, image.width(), image.height());
		int[] upperPrediction = Prediction.upperNeighbour(imageData, image.width(), image.height());
		int[] medianPrediction = Prediction.leftAndUpperNeighbourMedian(imageData, image.width(), image.height());
		frequencyData.setAll(imageData, leftPrediction, upperPrediction, medianPrediction);
	}

	private static void calcLength(Map<Integer, String> codeBook, Map<Integer, Long> data, int[] length, int pos) {
		data.forEach((key, value) -> {
			String code = codeBook.get(key);
			length[pos] += code.length() * value;
		});
		length[pos] /= 8;
	}

	private static void calcHuffmanCodes(FrequencyData frequencyData, int[] codedSizes) {
		HuffmanTree originalTree = new HuffmanTree(frequencyData.getImageFrequency());
		HuffmanTree leftTree = new HuffmanTree(frequencyData.getLeftPredictionFrequency());
		HuffmanTree upperTree = new HuffmanTree(frequencyData.getUpperPredictionFreqency());
		HuffmanTree medianTree = new HuffmanTree(frequencyData.getMedianPredictionFrequency());
		calcLength(originalTree.getCodeBook(), frequencyData.getImageFrequency().getFrequencyMap(), codedSizes, 0);
		calcLength(leftTree.getCodeBook(), frequencyData.getLeftPredictionFrequency().getFrequencyMap(), codedSizes, 1);
		calcLength(upperTree.getCodeBook(), frequencyData.getUpperPredictionFreqency().getFrequencyMap(), codedSizes, 2);
		calcLength(medianTree.getCodeBook(), frequencyData.getMedianPredictionFrequency().getFrequencyMap(), codedSizes, 3);
	}

}
