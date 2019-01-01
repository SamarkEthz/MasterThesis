package ch.ethz.matsim.students.samark.visualizer;

import ch.ethz.matsim.students.samark.evo.NetworkScoreLog;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;
import org.jfree.ui.VerticalAlignment;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.charts.XYLineChart;

import ReadWriteClone.XMLOps;

public class Visualizer {

	@SuppressWarnings("unchecked")
	public static void writeChartNetworkScore(int lastGeneration, int populationSize, int routesPerNetwork,
			int lastIteration, String inFileName, String outFileName) throws IOException {

		List<Map<String, NetworkScoreLog>> networkScoreMaps = new ArrayList<Map<String, NetworkScoreLog>>();
		networkScoreMaps.addAll(XMLOps.readFromFile(networkScoreMaps.getClass(), inFileName));

		Map<Integer, Double> generationsAverageNetworkScore = new HashMap<Integer, Double>();
		Map<Integer, Double> generationsBestNetworkScore = new HashMap<Integer, Double>();

		int g = 0;
		for (Map<String, NetworkScoreLog> networkScoreMap : networkScoreMaps.subList(0, lastGeneration - 1)) {
			g++;
			double averageNetworkScoreThisGeneration = 0.0;
			double bestNetworkScoreThisGeneration = -Double.MAX_VALUE;
			for (NetworkScoreLog nsl : networkScoreMap.values()) {
				if (nsl.overallScore > bestNetworkScoreThisGeneration) {
					bestNetworkScoreThisGeneration = nsl.overallScore;
				}
				averageNetworkScoreThisGeneration += nsl.overallScore / networkScoreMap.size();
			}
			System.out.println("Best    Network Score This Generation = " + bestNetworkScoreThisGeneration);
			System.out.println("Average Network Score This Generation = " + averageNetworkScoreThisGeneration);

			generationsAverageNetworkScore.put(g, averageNetworkScoreThisGeneration);				
			generationsBestNetworkScore.put(g, bestNetworkScoreThisGeneration);				

		}
		

		JFreeChart lineChart = ChartFactory
				.createXYLineChart(
						"[#Networks=" + populationSize + "];  [#MATSimIter=" + lastIteration
								+ "];  [#InitNetworkRoutes=" + routesPerNetwork + "] \r\n ",
						"Generation", "Total Annual Welfare [Mio CHF]", null); // dataset, PlotOrientation.VERTICAL, true,
																			// true, false
		LegendTitle legend = lineChart.getLegend();
		legend.setPosition(RectangleEdge.TOP); // RectangleEdge.RIGHT
		legend.setItemFont(new Font("Arial", Font.PLAIN, 20));

		XYPlot plot = (XYPlot) lineChart.getPlot();

		final XYSeries sAverage = new XYSeries("Average Network Welfare [Mio CHF]");
		for (Entry<Integer, Double> genAverageScoreEntry : generationsAverageNetworkScore.entrySet()) {
			sAverage.add((double) genAverageScoreEntry.getKey(), genAverageScoreEntry.getValue() / 1.0E6);
		}
		final XYSeries sBest = new XYSeries("Elite Network Welfare [Mio CHF]");
		for (Entry<Integer, Double> genBestScoreEntry : generationsBestNetworkScore.entrySet()) {
			sBest.add((double) genBestScoreEntry.getKey(), genBestScoreEntry.getValue() / 1.0E6);
		}

		XYSeriesCollection dAverage = new XYSeriesCollection();
		XYSeriesCollection dBest = new XYSeriesCollection();
		dAverage.addSeries(sAverage);
		dBest.addSeries(sBest);
		XYDataset dAverageX = (XYDataset) dAverage;
		XYDataset dBestX = (XYDataset) dBest;

		XYLineAndShapeRenderer r1 = new XYLineAndShapeRenderer();
		// r1.setSeriesPaint(0, new Color(0xff, 0xff, 0x00));
		// r1.setSeriesPaint(1, new Color(0x00, 0xff, 0xff));
		r1.setSeriesPaint(0, Color.BLUE);
		r1.setSeriesShapesVisible(0, false);
		r1.setSeriesShapesVisible(1, false);
		r1.setSeriesStroke(0, new BasicStroke(5.0f));

		XYLineAndShapeRenderer r2 = new XYLineAndShapeRenderer();
		// r2.setSeriesPaint(0, new Color(0xff, 0x00, 0x00));
		// r2.setSeriesPaint(1, new Color(0x00, 0xff, 0x00));
		r2.setSeriesPaint(0, Color.RED);
		r2.setSeriesShapesVisible(0, false);
		r2.setSeriesShapesVisible(1, false);
		r2.setSeriesStroke(0, new BasicStroke(5.0f));

		plot.setDataset(0, dAverageX);
		plot.setRenderer(0, r1);
		plot.setDataset(1, dBestX);
		plot.setRenderer(1, r2);

		// NumberAxis numberAxis = new NumberAxis();
		// numberAxis.setRange(-21.0E1, 1.5E1);
		//// numberAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// numberAxis.setTickUnit(new NumberTickUnit(3.0E1));
		// plot.setRangeAxis(numberAxis);

		Font font = new Font("Arial Bold", Font.BOLD, 30);
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setLabelFont(font);
		domainAxis.setTickUnit(new NumberTickUnit(3));
		domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 20));
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setLabelFont(font);
		// rangeAxis.setRange(-21.0E1, 1.5E1);
		rangeAxis.setAutoRange(true);
		rangeAxis.setTickUnit(new NumberTickUnit(3.0E1));
		rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 20));
		//
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		// domainAxis.setVerticalTickLabels(true);
		//
		plot.setDomainAxis(domainAxis);
		plot.setRangeAxis(rangeAxis);

		// plot.mapDatasetToRangeAxis(1, 1); //2nd dataset to 2nd y-axi

		plot.setBackgroundPaint(new Color(0xFF, 0xFF, 0xFF));
		plot.setDomainGridlinePaint(new Color(0x00, 0x00, 0xff));
		plot.setRangeGridlinePaint(new Color(0xff, 0x00, 0x00));

//		Log.write("should be making png here!");
		
		File file = new File(outFileName);
		ChartUtilities.saveChartAsPNG(file, lineChart, 1280, 960);

	}

	@SuppressWarnings("unchecked")
	public static void writeChartAverageTravelTimes(int lastGeneration, int populationSize, int routesPerNetwork,
			int lastIteration, String inFileName, String outFileName) throws FileNotFoundException {

		List<Map<String, NetworkScoreLog>> networkScoreMaps = new ArrayList<Map<String, NetworkScoreLog>>();
		networkScoreMaps.addAll(XMLOps.readFromFile(networkScoreMaps.getClass(), inFileName));

		Map<Integer, Double> generationsAverageTravelTime = new HashMap<Integer, Double>();
		Map<Integer, Double> generationsAverageTravelTimeStdDev = new HashMap<Integer, Double>();
		Map<Integer, Double> generationsBestTravelTime = new HashMap<Integer, Double>();

		int g = 0;
		for (Map<String, NetworkScoreLog> networkScoreMap : networkScoreMaps.subList(0, lastGeneration - 1)) {
			g++;
			double averageTravelTimeThisGeneration = 0.0;
			double averageTravelTimeStdDevThisGeneration = 0.0;
			double bestAverageTravelTimeThisGeneration = Double.MAX_VALUE;
			for (NetworkScoreLog nsl : networkScoreMap.values()) {
				if (nsl.averageTravelTime < bestAverageTravelTimeThisGeneration) {
					bestAverageTravelTimeThisGeneration = nsl.averageTravelTime;
				}
				averageTravelTimeThisGeneration += nsl.averageTravelTime / networkScoreMap.size();
				averageTravelTimeStdDevThisGeneration += nsl.stdDeviationTravelTime / networkScoreMap.size();
			}
			System.out.println("bestAverageTravelTimeThisGeneration = " + bestAverageTravelTimeThisGeneration);
			System.out.println("Average AverageTravelTime This Generation = " + averageTravelTimeThisGeneration);
			generationsAverageTravelTime.put(g, averageTravelTimeThisGeneration);
			generationsAverageTravelTimeStdDev.put(g, averageTravelTimeStdDevThisGeneration);
			generationsBestTravelTime.put(g, bestAverageTravelTimeThisGeneration);
		}
		XYLineChart chart = new XYLineChart("Perform. Evol. [nNetw=" + populationSize + "], [nSimIter=" + lastIteration
				+ "], [nInitRoutes/Netw=" + routesPerNetwork + "]", "Generation", "Score");
		chart.addSeries("Average Travel Time [min]", generationsAverageTravelTime);
		chart.addSeries("Average Travel Time - Std Deviation [min]", generationsAverageTravelTimeStdDev);
		chart.addSeries("Best Average Travel Time [min]", generationsBestTravelTime);
		chart.saveAsPng(outFileName, 800, 600);
	}

	// default plotting method with adaptable layout instead of standard java plotting
	public static void plot2D(String title,  String subtitle, String xAxisName, String yAxisName, List<Map<Integer, Double>> inputSeries,
			List<String> inputSeriesName, Double tickUnitX, Double tickUnitY, Range yRange, String outFileName)
			throws IOException {

//		 old version charts
//		 XYLineChart chart = new XYLineChart(title, xAxisName, yAxisName);
//		 for (Integer seriesNr=0; seriesNr<inputSeries.size(); seriesNr++) {
//		 chart.addSeries(inputSeriesName.get(seriesNr), inputSeries.get(seriesNr));
//		 }
//		 chart.saveAsPng("x"+outFileName, 800, 600);

		// new version
		JFreeChart lineChart = ChartFactory.createXYLineChart(title, xAxisName, yAxisName, null);
		TextTitle plotTitle = lineChart.getTitle();
		plotTitle.setFont(new Font("Arial Bold", Font.BOLD, 35));
		lineChart.addSubtitle(new TextTitle(subtitle,
				new Font("Arial", Font.PLAIN, 20), Color.black,
				RectangleEdge.TOP, HorizontalAlignment.CENTER,
				VerticalAlignment.CENTER, RectangleInsets.ZERO_INSETS));
		LegendTitle legend = lineChart.getLegend();
		legend.setPosition(RectangleEdge.TOP); // RectangleEdge.RIGHT
		legend.setItemFont(new Font("Arial", Font.PLAIN, 30));
		
		XYPlot plot = (XYPlot) lineChart.getPlot();
		List<XYDataset> dataSets = new ArrayList<XYDataset>();
		for (Integer seriesNr = 0; seriesNr < inputSeries.size(); seriesNr++) {
			final XYSeries thisSeries = new XYSeries(inputSeriesName.get(seriesNr));
			for (Entry<Integer, Double> inputSeriesEntry : inputSeries.get(seriesNr).entrySet()) {
				thisSeries.add((double) inputSeriesEntry.getKey(), inputSeriesEntry.getValue());
			}
			 XYSeriesCollection thisSeriesCollection = new XYSeriesCollection();
			 thisSeriesCollection.addSeries(thisSeries);
			 dataSets.add((XYDataset) thisSeriesCollection);
		}

		List<Color> defaultColors = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA,
				Color.BLACK, Color.ORANGE, Color.GRAY, Color.YELLOW);

		for (Integer dataSetNr = 0; dataSetNr < dataSets.size(); dataSetNr++) {
			XYLineAndShapeRenderer r = new XYLineAndShapeRenderer();
			r.setSeriesPaint(0, defaultColors.get(dataSetNr));
			r.setSeriesShapesVisible(0, false);
			r.setSeriesShapesVisible(1, false);
			r.setSeriesStroke(0, new BasicStroke(4.0f));
			plot.setDataset(dataSetNr, dataSets.get(dataSetNr));
			plot.setRenderer(dataSetNr, r);
		}

		Font font = new Font("Arial Bold", Font.BOLD, 40);
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setLabelFont(font);
		if (tickUnitX > 0.0) {
			domainAxis.setTickUnit(new NumberTickUnit(tickUnitX));
		}
		domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 40));
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setLabelFont(font);
		if (yRange != null) {
			rangeAxis.setRange(yRange);
		} else {
			rangeAxis.setAutoRange(true);
		}
		if (!tickUnitY.equals(null) && tickUnitY > 0.0) {
			rangeAxis.setTickUnit(new NumberTickUnit(tickUnitY));
		}
		rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 40));

		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDomainAxis(domainAxis);
		plot.setRangeAxis(rangeAxis);
		plot.setBackgroundPaint(new Color(0xFF, 0xFF, 0xFF));
		plot.setDomainGridlinePaint(new Color(0x00, 0x00, 0xff));
		plot.setRangeGridlinePaint(new Color(0xff, 0x00, 0x00));

		File file = new File(outFileName);
		ChartUtilities.saveChartAsPNG(file, lineChart, 1280, 960);
	}
	
	// plots as in default plotting method but with confidence interval bands (90th percentile)
	public static void plot2DConfIntervals(String title, String subtitle, String xAxisName, String yAxisName, List<Map<Integer, Double>> inputSeries,
			List<String> inputSeriesName, Double tickUnitX, Double tickUnitY, Range yRange, String outFileName, List<List<Double>> confIntervals)
			throws IOException {

//		 old version charts
//		 XYLineChart chart = new XYLineChart(title, xAxisName, yAxisName);
//		 for (Integer seriesNr=0; seriesNr<inputSeries.size(); seriesNr++) {
//		 chart.addSeries(inputSeriesName.get(seriesNr), inputSeries.get(seriesNr));
//		 }
//		 chart.saveAsPng("x"+outFileName, 800, 600);

		// new version
		JFreeChart lineChart = ChartFactory.createXYLineChart(title, xAxisName, yAxisName, null);
		LegendTitle legend = lineChart.getLegend();
		legend.setPosition(RectangleEdge.TOP); // RectangleEdge.RIGHT
		legend.setItemFont(new Font("Arial", Font.PLAIN, 30));
		TextTitle plotTitle = lineChart.getTitle();
		plotTitle.setFont(new Font("Arial Bold", Font.BOLD, 35));
		lineChart.addSubtitle(new TextTitle(subtitle,
				new Font("Arial", Font.PLAIN, 20), Color.black,
			    RectangleEdge.TOP, HorizontalAlignment.CENTER,
			    VerticalAlignment.CENTER, RectangleInsets.ZERO_INSETS));
		
		XYPlot plot = (XYPlot) lineChart.getPlot();
		List<XYDataset> dataSets = new ArrayList<XYDataset>();
		for (Integer seriesNr = 0; seriesNr < inputSeries.size(); seriesNr++) {
			final XYSeries thisSeries = new XYSeries(inputSeriesName.get(seriesNr));
			for (Entry<Integer, Double> inputSeriesEntry : inputSeries.get(seriesNr).entrySet()) {
				thisSeries.add((double) inputSeriesEntry.getKey(), inputSeriesEntry.getValue());
			}
			 XYSeriesCollection thisSeriesCollection = new XYSeriesCollection();
			 thisSeriesCollection.addSeries(thisSeries);
			 dataSets.add((XYDataset) thisSeriesCollection);
//			dataSets.add((XYDataset) thisSeries);
		}

		List<Color> defaultColors = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA,
				Color.BLACK, Color.ORANGE, Color.GRAY, Color.YELLOW);

		
		for (Integer confIntNr = 0; confIntNr < confIntervals.size(); confIntNr++) {
			final IntervalMarker confInterval = new IntervalMarker(confIntervals.get(confIntNr).get(0), confIntervals.get(confIntNr).get(1));
			confInterval.setLabel("90th Percentile");
			confInterval.setLabelFont(new Font("SansSerif", Font.ITALIC, 20));
			if (confIntNr%2==0) {
				confInterval.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
				confInterval.setLabelTextAnchor(TextAnchor.TOP_RIGHT);				
			}
			else {
				confInterval.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
				confInterval.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);				
			}
			confInterval.setPaint(
					new Color(defaultColors.get(confIntNr).getRed(), defaultColors.get(confIntNr).getGreen(), defaultColors.get(confIntNr).getBlue(), 90));
			plot.addRangeMarker(confInterval, Layer.BACKGROUND);
		}

	    
		for (Integer dataSetNr = 0; dataSetNr < dataSets.size(); dataSetNr++) {
			XYLineAndShapeRenderer r = new XYLineAndShapeRenderer();
			r.setSeriesPaint(0, defaultColors.get(dataSetNr));
			r.setSeriesShapesVisible(0, false);
			r.setSeriesShapesVisible(1, false);
			r.setSeriesStroke(0, new BasicStroke(4.0f));
			plot.setDataset(dataSetNr, dataSets.get(dataSetNr));
			plot.setRenderer(dataSetNr, r);
		}

		Font font = new Font("Arial Bold", Font.BOLD, 40);
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setLabelFont(font);
		if (tickUnitX > 0.0) {
			domainAxis.setTickUnit(new NumberTickUnit(tickUnitX));
		}
		domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 40));
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setLabelFont(font);
		if (yRange != null) {
			rangeAxis.setRange(yRange);
		} else {
			rangeAxis.setAutoRange(true);
		}
		if (!tickUnitY.equals(null) && tickUnitY > 0.0) {
			rangeAxis.setTickUnit(new NumberTickUnit(tickUnitY));
		}
		rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 40));

		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDomainAxis(domainAxis);
		plot.setRangeAxis(rangeAxis);
		plot.setBackgroundPaint(new Color(0xFF, 0xFF, 0xFF));
		plot.setDomainGridlinePaint(new Color(0x00, 0x00, 0xff));
		plot.setRangeGridlinePaint(new Color(0xff, 0x00, 0x00));

		File file = new File(outFileName);
		ChartUtilities.saveChartAsPNG(file, lineChart, 1280, 960);
	}

	public static Coord zh2img(Coord zhCoord, Double xScalingFactor, Double yScalingFactor, Double x0, Double y0) {
		Double xImg = (zhCoord.getX() - x0) / xScalingFactor;
		Double yImg = (zhCoord.getY() - y0) / yScalingFactor;
		return new Coord(xImg, yImg);
	}

}
