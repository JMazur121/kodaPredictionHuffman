package prediction;

import java.util.Arrays;

public class Prediction {

	public static final int OUT_OF_BOUND_CONST = 255;

	public static int[] leftNeighbour(int[] source, int width, int height) {
		int[] result = new int[source.length];
		int index = 0;
		for (int y = 0; y < height; ++y) {
			result[index++] = source[y * width] - OUT_OF_BOUND_CONST;
			for (int x = 1; x < width; ++x)
				result[index++] = source[y * width + x] - source[y * width + x - 1];
		}
		return result;
	}

	public static int[] upperNeighbour(int[] source, int width, int height) {
		int[] result = new int[source.length];
		int index = 0;
		for (int x = 0; x < width; ++x)
			result[index++] = source[x] - OUT_OF_BOUND_CONST;
		for (int y = 1; y < height; ++y) {
			for (int x = 0; x < width; ++x)
				result[index++] = source[y * width + x] - source[(y - 1) * width + x];
		}
		return result;
	}

	public static int[] leftAndUpperNeighbourMedian(int[] source, int width, int height) {
		int[] result = new int[source.length];
		int[] medianTab = new int[3];
		int index = 0;
		//upper neighbours are CONST, thus median is equal to OUT_OF_BOUND_CONST too.
		for (int x = 0; x < width; ++x)
			result[index++] = source[x] - OUT_OF_BOUND_CONST;
		for (int y = 1; y < height; ++y) {
			//left neighbours are CONST, thus median is equal to OUT_OF_BOUND_CONST too.
			result[index++] = source[y * width] - OUT_OF_BOUND_CONST;
			for (int x = 1; x < width; ++x) {
				medianTab[0] = source[y * width + x - 1];
				medianTab[1] = source[(y - 1) * width + x];
				medianTab[2] = source[(y - 1) * width + x - 1];
				Arrays.sort(medianTab);
				result[index++] = source[y * width + x] - medianTab[1];
			}
		}
		return result;
	}

}
