package frequency;

import lombok.Getter;
import java.util.HashMap;
import java.util.Map;

public class FrequencyMap {

	private Map<Integer,Long> frequencyMap;
	@Getter
	private long totalCount;

	public FrequencyMap() {
		frequencyMap = new HashMap<>();
		totalCount = 0;
	}

	public FrequencyMap(int[] values) {
		super();
		for (int val : values)
			increment(val);
	}

	public long getFrequency(Integer key) {
		return frequencyMap.getOrDefault(key, 0L);
	}

	public long incrementAndGet(Integer key) {
		Long counter = frequencyMap.get(key);
		counter = (counter == null) ? 1L : ++counter;
		frequencyMap.put(key, counter);
		++totalCount;
		return counter;
	}

	public void increment(Integer key) {
		Long counter = frequencyMap.get(key);
		if (counter == null)
			frequencyMap.put(key, 1L);
		else
			frequencyMap.put(key, ++counter);
		++totalCount;
	}

	public double entropy() {
		double entropy = 0.0;
		for (long val : frequencyMap.values()) {
			double probability = (double)val / totalCount;
			entropy -= probability * log2(probability);
		}
		return entropy;
	}

	private static double log2(double n) {
		return Math.log(n) / Math.log(2);
	}

}
