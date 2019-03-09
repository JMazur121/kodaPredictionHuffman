package frequency;

import lombok.Getter;
import utils.HistogramMatrix;
import java.util.ArrayList;
import java.util.List;

@Getter
public class FrequencyData {

	private int[] imageData;
	private int[] leftPrediction;
	private int[] upperPrediction;
	private int[] medianPrediction;

	private FrequencyMap imageFrequency;
	private FrequencyMap leftPredictionFrequency;
	private FrequencyMap upperPredictionFreqency;
	private FrequencyMap medianPredictionFrequency;

	private List<double[]> doubles;

	public FrequencyData() {
		imageFrequency = new FrequencyMap();
		leftPredictionFrequency = new FrequencyMap();
		upperPredictionFreqency = new FrequencyMap();
		medianPredictionFrequency = new FrequencyMap();
		doubles = new ArrayList<>();
	}

	public void setAll(int[] imageData, int[] leftPrediction, int[] upperPrediction, int[] medianPrediction) {
		this.imageData = imageData;
		this.leftPrediction = leftPrediction;
		this.upperPrediction = upperPrediction;
		this.medianPrediction = medianPrediction;
		imageFrequency.addAll(imageData);
		leftPredictionFrequency.addAll(leftPrediction);
		upperPredictionFreqency.addAll(upperPrediction);
		medianPredictionFrequency.addAll(medianPrediction);
		doubles.add(HistogramMatrix.convertToDoubleArray(imageData, 255.0));
		doubles.add(HistogramMatrix.convertToDoubleArray(leftPrediction, 255.0));
		doubles.add(HistogramMatrix.convertToDoubleArray(upperPrediction, 255.0));
		doubles.add(HistogramMatrix.convertToDoubleArray(medianPrediction, 255.0));
	}

}
