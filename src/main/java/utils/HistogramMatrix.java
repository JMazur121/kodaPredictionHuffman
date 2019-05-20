package utils;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.data.statistics.HistogramDataset;
import java.awt.*;

public class HistogramMatrix extends GridPane {

	private ChartViewer upperLeft;
	private ChartViewer upperRight;
	private ChartViewer lowerLeft;
	private ChartViewer lowerRight;

	public static final int BINS = 50;
	public static final String X_LABEL = "Wartosci probek";
	public static final String Y_LABEL = "Czestosc";

	private HistogramMatrix() {
		upperLeft = new ChartViewer();
		upperRight = new ChartViewer();
		lowerLeft = new ChartViewer();
		lowerRight = new ChartViewer();
		ColumnConstraints column1 = new ColumnConstraints();
		ColumnConstraints column2 = new ColumnConstraints();
		column1.setPercentWidth(50);
		column2.setPercentWidth(50);
		this.getColumnConstraints().addAll(column1, column2);
		RowConstraints row1 = new RowConstraints();
		RowConstraints row2 = new RowConstraints();
		row1.setPercentHeight(50);
		row2.setPercentHeight(50);
		this.getRowConstraints().addAll(row1, row2);
		this.add(upperLeft, 0, 0);
		this.add(upperRight, 1, 0);
		this.add(lowerLeft, 0, 1);
		this.add(lowerRight, 1, 1);
	}

	public static HistogramMatrix newHistogramMatrix() {
		return new HistogramMatrix();
	}

	public static double[] convertToDoubleArray(int[] table, double norm) {
		double[] vals = new double[table.length];
		int idx = 0;
		for (int i : table)
			vals[idx++] = i / norm;
		return vals;
	}

	public HistogramMatrix withULChart(String title, double[] values, double min, double max, Color col) {
		upperLeft.setChart(buildChart(title, values, min, max, col));
		return this;
	}

	public HistogramMatrix withURChart(String title, double[] values, double min, double max, Color col) {
		upperRight.setChart(buildChart(title, values, min, max, col));
		return this;
	}

	public HistogramMatrix withLLChart(String title, double[] values, double min, double max, Color col) {
		lowerLeft.setChart(buildChart(title, values, min, max, col));
		return this;
	}

	public HistogramMatrix withLRChart(String title, double[] values, double min, double max, Color col) {
		lowerRight.setChart(buildChart(title, values, min, max,col));
		return this;
	}

	public static JFreeChart buildChart(String title, double[] values, double min, double max, Color col) {
		HistogramDataset dataset = new HistogramDataset();
		dataset.addSeries("", values, BINS, min, max);
		JFreeChart histogram = ChartFactory.createHistogram(title, X_LABEL, Y_LABEL, dataset);
		histogram.getXYPlot().getRenderer().setSeriesPaint(0, col);
		return histogram;
	}

}
