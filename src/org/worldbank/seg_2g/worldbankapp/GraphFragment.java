package org.worldbank.seg_2g.worldbankapp;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Fragment;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class GraphFragment extends Fragment {

	// text to display when the graph contains null values
	private static final String NULL_DATA_TEXT = "No data exists for the selected combination of country, attribute and year range";
	
	// the layout new graphs are added to
	public static RelativeLayout graphLayout;
	
	// instance of the graph activity
	private GraphActivity activityContext;

	// JSON data and country name strings
	private String countryName;
	private String data;
	private String comparisonData;
	
	// set to true if the JSON data contains a null value
	private boolean containsOnlyNullData;

	// JSON arrays for main and comparison line
	private JSONArray dataFeed;
	private JSONArray comparisonDataFeed;
	private JSONArray feedArray;
	private JSONArray comparisonFeedArray;
	
	// JSON object containing the title and number of entries
	private JSONObject titleValues;
	private int totalEntries;
	private int jsonCounter;
	
	// the graph view
	private LineChartView graph;
	
	// graph data information where line list will be added
	private LineChartData chartData;
	
	// point value list for main and comparison line, as well as dummy list to set view of percentage based graph
	private List<PointValue> values;
	private List<PointValue> comparisonValues;
	private List<PointValue> invisiblePercentGraphValues;

	// lists of axis values
	private List<AxisValue> axisValues;
	private List<AxisValue> comparisonAxisValues;
	
	// strings for attribute titles
	private String measureLabel;
	private String comparisonMeasureLabel;

	// fields used for rounding values for axis labels
	private int value;
	private int comparisonValue;
	private float percentValue;
	private int year;
	private int roundedValue;
	private int highestValue;
	private int comparisonHighestValue;
	private int lowestValue;
	private int comparisonLowestValue;
	private int increment;
	private String valueString;
	private int digitCount;
	private int charCounter;
	private String newLabel;
	private int firstThreeDigits;
	
	// the main, comparison and dummy percentage lines and a list of lines these will be added to
	private Line invisiblePercentGraphLine;
	private Line mainGraphLine;
	private Line comparisonGraphLine;
	private List<Line> graphLines;
	
	// the graph axes
	private Axis axisX;
	private Axis axisY;
	private Axis rightAxisY;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View fragmentView = inflater.inflate(
				R.layout.fragment_graph, container, false);
		// the layout graphs will be added to
		graphLayout = (RelativeLayout) fragmentView.findViewById(R.id.main_graph_layout);
		
		return fragmentView;
	}

	// this method will create a single line graph
	protected GraphFragment createGraph(final GraphActivity context, String JSONdata,
			String countryName) {
		this.countryName = countryName;
		this.data = JSONdata;
		this.activityContext = context;
		
		graph = new LineChartView(context);

		new AsyncTask<Void,Void,Void>() {

			// create graph in new thread
			@Override
			protected Void doInBackground(Void... params) {
				createLinearGraph();
				return null;
			}
			
			// after thread runs, if no null data exists, add chart data to chart view and add chart view to layout
			@Override
			protected void onPostExecute(Void v) {
				
				if (!containsOnlyNullData) {
				
					graph.setLineChartData(chartData);
					graph.setPadding(0, 10, 0, 0);
					graph.setZoomEnabled(false);
					graph.setScrollEnabled(false);
					
					graphLayout.removeAllViews();
					graphLayout.addView(graph);
					
				}
				// if null data exists (resulting in null chart view) notify the user
				else {
					graphLayout.removeAllViews();
					Toast.makeText(activityContext, NULL_DATA_TEXT, Toast.LENGTH_LONG).show();
				}
			}
		}.execute();
		
		// return this graph fragment, to be stores in cache array
		return this;
	}

	// same as above method with an extra JSON parameter for a comparison line.
	protected GraphFragment createGraph(GraphActivity context, String JSONdata,
			String comparisonData, String countryName) {
		
		this.countryName = countryName;
		this.data = JSONdata;
		this.comparisonData = comparisonData;
		
		this.activityContext = context;
		
		graph = new LineChartView(context);
		
		new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				createComparisonGraph();
				return null;
			}
			
			@Override
			protected void onPostExecute(Void v) {
				
				if (!containsOnlyNullData) {
					
					graph.setLineChartData(chartData);
					graph.setPadding(0, 10, 0, 0);
					graph.setZoomEnabled(false);
					graph.setScrollEnabled(false);
					
					graphLayout.removeAllViews();					
					graphLayout.addView(graph);
					
				}
				else {
					graphLayout.removeAllViews();
					Toast.makeText(activityContext, NULL_DATA_TEXT, Toast.LENGTH_LONG).show();
				}
			}
		}.execute();
		
		return this;
	}
	
	// called if fragment instance exists in cache array, rebuilds graph from existing data
	public void reloadGraph() {
		if (comparisonData == null) {
			new GraphFragment().createGraph(activityContext, data, countryName);
		} else {
			new GraphFragment().createGraph(activityContext, data, comparisonData, countryName);
		}
	}

	// creates a double line comparison graph, with the higher value normalized to show relative growth compared to lower value
	private void createComparisonGraph() {
		
		containsOnlyNullData = false;
		
		try {
			// create JSON arrays from main and comparison JSON strings
			dataFeed = new JSONArray(data);
			comparisonDataFeed = new JSONArray(comparisonData);
			
			// get title values for and total entries
			titleValues = dataFeed.getJSONObject(0);
			totalEntries = titleValues.getInt("total");
			
			// initialize empty point value lists
			values = new ArrayList<PointValue>();
			comparisonValues = new ArrayList<PointValue>();

			// get JSON arrays to extract values from
			feedArray = dataFeed.getJSONArray(1);
			comparisonFeedArray = comparisonDataFeed.getJSONArray(1);

			// initialize counter to total number of entries
			jsonCounter = totalEntries - 1;
			
			// initialize empty axis value lists
			axisValues = new ArrayList<AxisValue>();
			comparisonAxisValues = new ArrayList<AxisValue>();
			
			// initialize empty lines, set color, line width and point size
			mainGraphLine = new Line().setColor(Color.RED).setCubic(false).setStrokeWidth(2).setPointRadius(3);
			comparisonGraphLine = new Line().setColor(Color.GREEN).setCubic(false).setStrokeWidth(2).setPointRadius(3);
			
			// initialize empty lines list to which graph lines will be added
			graphLines = new ArrayList<Line>();

			measureLabel = comparisonMeasureLabel = null;
			
			// integers to contain highest and lowest values, used to assign labels to graph
			highestValue = comparisonHighestValue = 0;
			lowestValue = comparisonLowestValue = 2000000000;

			// declare floats used for normalizing the higher values and initialize with default values
			float normAddition = 1;
			float scale = 0;
			
			// declare int used to store last value used to determine whether to increase or decrease normalization
			int originalPreviousValue = 0;
			
			// put every entry from both JSON objects into the correct line
			for (int i = 0; i < totalEntries; ++i) {
				
				// initialize new JSON objects from feed arrays
				JSONObject json = feedArray.getJSONObject(jsonCounter);
				JSONObject comparisonJson = comparisonFeedArray.getJSONObject(jsonCounter--);
				
				// if either value field in an object is null, stop creating graph and set null data boolean to true
				if (json.optInt("value", -1) == -1 || comparisonJson.optInt("value", -1) == -1) {
					containsOnlyNullData = true;
					return;
				}
				
				// add measure type label to string to later create axes labels
				if (measureLabel == null) {
					JSONObject indicator = json.getJSONObject("indicator");
					measureLabel = indicator.getString("value");
					indicator = comparisonJson.getJSONObject("indicator");
					comparisonMeasureLabel = indicator.getString("value");
				}
				
				value = json.getInt("value");
				comparisonValue = comparisonJson.getInt("value");
				year = json.getInt("date");
				
				// if current value is bigger than the previous value, increase normalization
				if (value > originalPreviousValue) {
					normAddition += 0.02;
				}
				// else decrease normalization
				else {
					normAddition -= 0.02;
				}
				
				// store this value for comparison to the following value
				originalPreviousValue = value;
				
				// if scale is of default value calculate the normalization factor
				if (scale == 0) {
					scale = (float) comparisonValue / value;
				}
				
				// normalize the value to adjust to the scale of the comparison line
				float normalisedValue = (value * scale) * normAddition;

				// add values to lines and create labels
				values.add(new PointValue(year, normalisedValue).setLabel(String.valueOf(value).toCharArray()));
				comparisonValues.add(new PointValue(year, comparisonValue).setLabel(String.valueOf(comparisonValue).toCharArray()));
				
				// assign highest and lowest comparison value, later used to create axis labels
				if (comparisonValue > comparisonHighestValue) { comparisonHighestValue = comparisonValue; }
				if (comparisonValue < comparisonLowestValue) { comparisonLowestValue = comparisonValue; }

			}
			
			// set value lists to graph lines
			mainGraphLine.setValues(values);
			mainGraphLine.setHasLabelsOnlyForSelected(true);
			comparisonGraphLine.setValues(comparisonValues);
			
			// add both lines to line list
			graphLines.add(mainGraphLine);
			graphLines.add(comparisonGraphLine);
			
			// initialize chart data and set with line list containing both lines
			chartData = new LineChartData(graphLines);
			
			// create labels for both Y axes
			createNumberLabels(comparisonHighestValue, comparisonLowestValue);
			
			// initialize year axis and set text color and size
			axisX = new Axis().setMaxLabelChars(4).setTextColor(Color.BLACK).setTextSize(11);
			
			// initialize left Y axis and set with values, label, text color and text size
			axisY = new Axis(comparisonAxisValues).setName(measureLabel + " (relative growth)").setHasLines(false)
					.setTextColor(Color.RED).setTextSize(11);
			// initialize right Y axis and set with values, label, text color and text size
			rightAxisY = new Axis(axisValues).setName(comparisonMeasureLabel).setHasLines(true)
					.setMaxLabelChars(11).setTextColor(Color.GREEN).setLineColor(Color.LTGRAY).setTextSize(11);
			
			// set chart data object with X, left Y and right Y axes
			chartData.setAxisXBottom(axisX);
			chartData.setAxisYLeft(axisY);
			chartData.setAxisYRight(rightAxisY);
			
		} catch (JSONException e) {}
		
	}

	// creates a single line graph
	private void createLinearGraph() {
			
		containsOnlyNullData = false;
		
		try {
			// extract full JSON array from JSON string
			dataFeed = new JSONArray(data);
			titleValues = dataFeed.getJSONObject(0);
			
			// get total number of entries
			totalEntries = titleValues.getInt("total");
			
			// initialize point value list
			values = new ArrayList<PointValue>();

			// extract values JSON array from previous full
			feedArray = dataFeed.getJSONArray(1);

			// initialize counter with number of entries
			jsonCounter = totalEntries - 1;
			
			// initialize empty axis values list
			axisValues = new ArrayList<AxisValue>();
			//initialize empty graph line, set color to red, set line width and point size
			mainGraphLine = new Line().setColor(Color.RED).setCubic(false).setStrokeWidth(2).setPointRadius(3);
			// initialize empty line list to which the graph line will be added 
			graphLines = new ArrayList<Line>();

			measureLabel = null;
			
			// integers to contain highest and lowest values, used to assign labels to graph
			highestValue = 0;
			lowestValue = 2000000000;

			// put every entry from JSON in graph and create a label
			for (int i = 0; i < totalEntries; ++i) {
				
				// initialize new JSON object from JSON array
				JSONObject json = feedArray.getJSONObject(jsonCounter--);
				
				// if the value field in the object is null, stop creating graph and set null data boolean to true
				if (json.optInt("value", -1) == -1) {
					containsOnlyNullData = true;
					return;
				}
				
				// add measure type label to string, this will be displayed beside Y axis
				if (measureLabel == null) {
					JSONObject indicator = json.getJSONObject("indicator");
					measureLabel = indicator.getString("value");
				}
				
				// extract current entry (int for value graph, float for percent graph)
				if (measureLabel.contains("%")) {
					percentValue = (float) json.getDouble("value");
					year = json.getInt("date");
					// create percent value labels for representation in the graph Y axis (single run)
					// and add invisible dummy 0 and 100 values to show full graph scale
					if (axisValues.size() == 0) {
						createPercentLabel();
						invisiblePercentGraphValues = new ArrayList<PointValue>();
						invisiblePercentGraphValues.add(new PointValue(year, 0));
						invisiblePercentGraphValues.add(new PointValue(year, 100));
						invisiblePercentGraphLine = new Line(invisiblePercentGraphValues).setHasPoints(false).setHasLines(false);
						graphLines.add(invisiblePercentGraphLine);
					}
					// add year and value to the list later used to draw graph
					values.add(new PointValue(year, percentValue));
				}
				else {
					value = json.getInt("value");
					year = json.getInt("date");
					// assign highest and lowest value, later used to create axis labels
					if (value > highestValue) { highestValue = value; }
					if (value < lowestValue) { lowestValue = value; }
					// add year and value to the list later used to draw graph
					values.add(new PointValue(year, value));
				}
			}
			
			if (!measureLabel.contains("%")) {
				// round the entry value for representation in the graph Y axis
				createNumberLabels(highestValue, lowestValue);
			}
				
			// add point values to graph line
			mainGraphLine.setValues(values);
			// add graph line to line list
			graphLines.add(mainGraphLine);
			// initialize empty chart data object
			chartData = new LineChartData();

			// set the axes max label chars, color and size
			axisX = new Axis().setMaxLabelChars(4).setTextColor(Color.BLACK).setTextSize(11);
			axisY = new Axis().setName(measureLabel).setHasLines(true)
					.setMaxLabelChars(11).setTextColor(Color.BLACK).setLineColor(Color.LTGRAY).setTextSize(11);
			
			// set the Y axis with rounded value labels
			axisY.setValues(axisValues);

			// add axes to chart data
			chartData.setAxisXBottom(axisX);
			chartData.setAxisYLeft(axisY);

			// set chart data with graph lines
			chartData.setLines(graphLines);
			
			
		} catch (JSONException e) {}

	}

	// called if the graph contains percent values
	private void createPercentLabel() {
		for (int i = 100; i >= 0; i -= 20) {
			char[] label = String.valueOf(i).toCharArray();
			axisValues.add(new AxisValue(i, label));
		}
	}
	
	// called if the graph contains number values
	private void createNumberLabels(int highestValue, int lowestValue) {

		setRoundedValue(highestValue);
		int highestRoundedValue = roundedValue;
		setRoundedValue(lowestValue);
		int lowestRoundedValue = roundedValue;
		
		// determine value increment to display in graph axis labels
		increment = (highestRoundedValue - lowestRoundedValue) / totalEntries;
		
		for (int i = highestRoundedValue; i >= lowestRoundedValue; i -= increment) {
			
			setRoundedValue(i);
			
			// put first digit char in new label
			newLabel = "" + valueString.charAt(charCounter++);
	
			// add commas to the value string
			while (digitCount > 3) {
				while (digitCount-- % 3 != 0) {
					newLabel += valueString.charAt(charCounter++);
				}
				newLabel += "," + valueString.charAt(charCounter++);
			}
			// finalise new label with last three chars from string
			newLabel += valueString.substring(charCounter);
	
			// convert string to char array
			char[] label = newLabel.toCharArray();
	
			// add label to new list
			axisValues.add(new AxisValue(roundedValue, label));
			
			// if creating labels for a comparison graph, create empty labels for second axis
			if (comparisonData != null) {
				comparisonAxisValues.add(new AxisValue(roundedValue, "".toCharArray()));
			}
		}
	}
	
	private void setRoundedValue(int value) {
		
		// change string to represent rounded value
		valueString = String.valueOf(value);
		digitCount = valueString.length() - 1;
		charCounter = 0;
		// store the first three digits as int for rounded display value
		firstThreeDigits = Integer.parseInt(valueString.substring(0, 3));
		// calculate rounded value for display
		roundedValue = 1;
		switch (digitCount) {
		case 9:
			roundedValue = firstThreeDigits * 10000000;
			break;
		case 8:
			roundedValue = firstThreeDigits * 1000000;
			break;
		case 7:
			roundedValue = firstThreeDigits * 100000;
			break;
		case 6:
			roundedValue = firstThreeDigits * 10000;
			break;
		case 5:
			roundedValue = firstThreeDigits * 1000;
			break;
		case 4:
			roundedValue = firstThreeDigits * 100;
			break;
		case 3:
			roundedValue = firstThreeDigits * 10;
			break;
		case 2:
			roundedValue = firstThreeDigits;
		}
		// store rounded value in value string
		valueString = String.valueOf(roundedValue);
	}

}
