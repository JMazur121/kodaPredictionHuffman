package prediction;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Setter
public class PredictionService extends Service<List<int[]>> {

	private int[] source;
	private int width, height;

	protected Task<List<int[]>> createTask() {
		return new Task<List<int[]>>() {
			@Override
			protected List<int[]> call() throws Exception {
				ArrayList<int[]> predictions = new ArrayList<>();
				int[] leftNeighbour = Prediction.leftNeighbour(source, width, height);
				int[] upperNeighbur = Prediction.upperNeighbour(source, width, height);
				int[] medianNeighbour = Prediction.leftAndUpperNeighbourMedian(source, width, height);
				predictions.add(leftNeighbour);
				predictions.add(upperNeighbur);
				predictions.add(medianNeighbour);
				return predictions;
			}
		};
	}

}
