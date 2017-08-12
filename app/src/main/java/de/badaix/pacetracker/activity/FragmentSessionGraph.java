package de.badaix.pacetracker.activity;

import android.graphics.Color;
import android.graphics.Paint.Align;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.TimeChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer.FillOutsideLine;
import org.achartengine.renderer.XYSeriesRenderer.FillOutsideLine.Type;

import java.util.Calendar;
import java.util.Date;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.HxmData;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.DoubleBuffer;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.TextItemPair;

enum GraphType {
    SPEED(0), SPEED_ELEVATION(1), SPEED_PULSE(2);

    private int type;

    GraphType(int type) {
        this.type = type;
    }

    static GraphType fromInt(int type) {
        switch (type) {
            case 0:
                return SPEED;
            case 1:
                return SPEED_ELEVATION;
            case 2:
                return SPEED_PULSE;
        }
        throw new IllegalArgumentException(Integer.toString(type));
    }

    boolean isSpeed() {
        return ((this == SPEED) || (this == SPEED_ELEVATION) || (this == SPEED_PULSE));
    }

    boolean isElevation() {
        return (this == SPEED_ELEVATION);
    }

    boolean isPulse() {
        return (this == SPEED_PULSE);
    }

    int toInt() {
        return type;
    }
}

class Graph {
    XYSeries series;
    XYSeriesRenderer renderer = null;
    int idx;
    String axisCaption;
    String seriesCaption;
    Align yAxisAlign;
    double minX = 0;
    double minY = 0;
    double maxX = 0;
    double maxY = 0;
    double lastX = Double.MIN_VALUE;

    public Graph(String axisCaption, String seriesCaption, Align yAxisAlign, int idx) {
        this.axisCaption = axisCaption;
        this.seriesCaption = seriesCaption;
        this.yAxisAlign = yAxisAlign;
        this.idx = idx;
        renderer = new XYSeriesRenderer();
        series = new XYSeries(seriesCaption, idx);
        lastX = Double.MIN_VALUE;
    }

    public void add(double x, double y) {
        if (lastX == Double.MIN_VALUE) {
            minX = x;
            maxX = x;
            minY = y;
            maxY = y;
        } else {
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        if (x > lastX) {
            lastX = x;
            series.add(x, y);
        }
    }
}

public class FragmentSessionGraph extends Fragment implements SessionGUI, OnItemSelectedListener {
    Graph graphSpeed;
    Graph graphElevation;
    Graph graphPulse;
    private Session session = null;
    private View view;
    private XYMultipleSeriesDataset mDataset;
    private XYMultipleSeriesRenderer mRenderer;
    private GraphicalView mSpeedChartView = null;
    private Spinner spinnerShowGraph;
    private GraphType graphType;
    private Object syncObject = new Object();
    private String fragmentName = "Graph";
    private double speedFactor;
    private ArrayAdapter<TextItemPair<GraphType>> lapAdapter;

    public void setTitle(String title) {
        fragmentName = title;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Hint.log(this, "onCreate");
        super.onCreate(savedInstanceState);
        lapAdapter = new ArrayAdapter<TextItemPair<GraphType>>(this.getActivity(),
                android.R.layout.simple_spinner_item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Hint.log(this, "onCreateView");
        view = inflater.inflate(R.layout.session_graph, container, false);

        speedFactor = GlobalSettings.getInstance(getActivity()).getDistUnit().getFactor() / 1000.;
        graphType = GraphType.SPEED_ELEVATION;
        spinnerShowGraph = (Spinner) view.findViewById(R.id.spinnerShowGraph);

        mDataset = new XYMultipleSeriesDataset();
        mRenderer = new XYMultipleSeriesRenderer(2);
        mSpeedChartView = null;
        mRenderer.setApplyBackgroundColor(true);
        // mRenderer.setBackgroundColor(getResources().getColor(R.color.item));
        mRenderer.setBackgroundColor(Color.argb(0x00, 0xFF, 0x00, 0x00));
        mRenderer.setGridColor(getResources().getColor(R.color.blue_focused));
        mRenderer.setChartTitleTextSize(20);
        mRenderer.setAxesColor(getResources().getColor(R.color.black));

        mRenderer.setAxisTitleTextSize(17);
        mRenderer.setLabelsTextSize(Helper.dipToPix(this.getActivity(), 12));
        mRenderer.setLegendTextSize(Helper.dipToPix(this.getActivity(), 13));
        mRenderer.setLabelsColor(getResources().getColor(R.color.black));
        mRenderer.setXLabelsColor(getResources().getColor(R.color.text));
        mRenderer.setYLabelsColor(0, getResources().getColor(R.color.text));
        mRenderer.setYLabelsColor(1, getResources().getColor(R.color.text));

        // mRenderer.setMargins(new int[] { 0, 0, 0, 0 });
        mRenderer.setMargins(new int[]{20, 30, 20, 30});
        // mRenderer.setMarginsColor(Color.TRANSPARENT);
        // mRenderer.setBackgroundColor(Color.TRANSPARENT);
        // mRenderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
        mRenderer.setMarginsColor(getResources().getColor(R.color.item));
        mRenderer.setZoomButtonsVisible(true);
        mRenderer.setPointSize(1);
        mRenderer.setShowGrid(true);
        mRenderer.setXLabelsAlign(Align.CENTER);
        mRenderer.setYLabelsAlign(Align.RIGHT);
        mRenderer.setYAxisAlign(Align.CENTER, 1);

        int lastGraph = GlobalSettings.getInstance().getInt("graphType", GraphType.SPEED_ELEVATION.toInt());
        lapAdapter.add(new TextItemPair<GraphType>(getResources().getString(R.string.speed), GraphType.SPEED));
        lapAdapter.add(new TextItemPair<GraphType>(getResources().getString(R.string.speed) + " + "
                + getResources().getString(R.string.elevation), GraphType.SPEED_ELEVATION));
        addHxm();
        lapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerShowGraph.setAdapter(lapAdapter);
        spinnerShowGraph.setOnItemSelectedListener(this);
        spinnerShowGraph.setSelection(Math.min(lastGraph, lapAdapter.getCount() - 1));
        // update();
        return view;
    }

    private void addHxm() {
        if ((lapAdapter == null) || (lapAdapter.getCount() == 3))
            return;

        if ((session != null) && (session.getHxmData() != null) && !session.getHxmData().isEmpty())
            lapAdapter.add(new TextItemPair<GraphType>(getResources().getString(R.string.speed) + " + "
                    + getResources().getString(R.string.heartRate), GraphType.SPEED_PULSE));
    }

    @Override
    public void setSession(Session session) {
        Hint.log(this, "setSession");
        this.session = session;
        addHxm();
    }

    @Override
    public void onLocationChanged(Location arg0) {
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
    }

    @Override
    public void onGuiTimer(boolean resumed) {
    }

    @Override
    public void onResume() {
        Hint.log(this, "onResume");
        super.onResume();
        update();
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
    }

    @Override
    public void onSessionCommand(int command) {
        // TODO Auto-generated method stub
    }

    private void createChartView() {
        Hint.log(this, "createChartView");
        if (mSpeedChartView != null)
            return;

        LinearLayout layout = (LinearLayout) view.findViewById(R.id.session_chart);
        // SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss",
        // Locale.GERMANY);
        // df.setTimeZone(TimeZone.getTimeZone("GMT-0"));

        TimeChart tc = new TimeChart(mDataset, mRenderer);
        tc.setDateFormat("H:mm:ss");
        // tc.setXAxisSmart(false);
        mSpeedChartView = new GraphicalView(this.getActivity(), tc);// mDataset,
        // mRenderer,
        // "HH:mm:ss");
        // mSpeedChartView = ChartFactory.getTimeChartView(this.getActivity(),
        // tc);//mDataset, mRenderer, "HH:mm:ss");
        // mSpeedChartView = ChartFactory.getLineChartView(this.getActivity(),
        // mDataset, mRenderer);

		/*
         * mRenderer.setClickEnabled(true); mRenderer.setSelectableBuffer(100);
		 * mSpeedChartView.setOnClickListener(new View.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { SeriesSelection
		 * seriesSelection = mSpeedChartView .getCurrentSeriesAndPoint(); if
		 * (seriesSelection == null) { } else { } } });
		 * 
		 * mSpeedChartView.addPanListener(new PanListener() { public void
		 * panApplied() { System.out.println("New X range=[" +
		 * mRenderer.getXAxisMin() + ", " + mRenderer.getXAxisMax() +
		 * "], Y range=[" + mRenderer.getYAxisMax() + ", " +
		 * mRenderer.getYAxisMax() + "]"); } });
		 */
        layout.addView(mSpeedChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    }

    private void initSpeedGraph(Graph graph) {
        graph.renderer = new XYSeriesRenderer();
        graph.renderer.setColor(getResources().getColor(R.color.blue));
        graph.renderer.setPointStyle(PointStyle.CIRCLE);
        graph.renderer.setFillPoints(true);
        graph.renderer.setLineWidth(3);
    }

    private void initElevationGraph(Graph graph) {
        graph.renderer = new XYSeriesRenderer();
        graph.renderer.setColor(getResources().getColor(R.color.orange));
        graph.renderer.setPointStyle(PointStyle.CIRCLE);
        graph.renderer.setFillPoints(true);
        graph.renderer.setLineWidth(3);
        FillOutsideLine fillOutsideLine = new FillOutsideLine(Type.BELOW);
        fillOutsideLine.setColor(getResources().getColor(R.color.orange_focused));
        graph.renderer.addFillOutsideLine(fillOutsideLine);
    }

    private void initPulseGraph(Graph graph) {
        graph.renderer = new XYSeriesRenderer();
        graph.renderer.setColor(getResources().getColor(R.color.orange));
        graph.renderer.setPointStyle(PointStyle.CIRCLE);
        graph.renderer.setFillPoints(true);
        graph.renderer.setLineWidth(3);
        FillOutsideLine fillOutsideLine = new FillOutsideLine(Type.BELOW);
        fillOutsideLine.setColor(getResources().getColor(R.color.orange_focused));
        graph.renderer.addFillOutsideLine(fillOutsideLine);
    }

    private void addGraph(Graph graph) {
        Hint.log(this, "addGraph: " + graph.seriesCaption);
        mRenderer.addSeriesRenderer(graph.renderer);
        mRenderer.setYTitle(graph.axisCaption, graph.idx);

        mDataset.addSeries(graph.idx, graph.series);
        mRenderer.setYAxisAlign(graph.yAxisAlign, graph.idx);
        Align labelAlign = Align.LEFT;
        if (graph.yAxisAlign == Align.LEFT)
            labelAlign = Align.RIGHT;
        mRenderer.setYLabelsAlign(labelAlign, graph.idx);
    }

    private void reset() {
        Hint.log(this, "reset");
        createChartView();

        int count = mDataset.getSeriesCount();
        for (int i = 0; i < count; ++i)
            mDataset.removeSeries(0);

        count = mRenderer.getSeriesRenderers().length;
        for (int i = 0; i < count; ++i)
            mRenderer.removeSeriesRenderer(mRenderer.getSeriesRenderers()[0]);

        int idx = 0;
        graphElevation = null;
        if (graphType.isElevation()) {
            graphElevation = new Graph(getResources().getString(R.string.elevation) + " [m]", getResources().getString(
                    R.string.elevation), Align.RIGHT, idx);
            initElevationGraph(graphElevation);
            addGraph(graphElevation);
            idx++;
        }

        graphPulse = null;
        if (graphType.isPulse()) {
            graphPulse = new Graph(getResources().getString(R.string.heartRate) + " [bpm]", getResources().getString(
                    R.string.heartRate), Align.RIGHT, idx);
            initPulseGraph(graphPulse);
            addGraph(graphPulse);
            idx++;
        }

        graphSpeed = null;
        if (graphType.isSpeed()) {
            graphSpeed = new Graph(getResources().getString(R.string.speed) + " ["
                    + GlobalSettings.getInstance().getDistUnit().perHourString() + "]", getResources().getString(
                    R.string.speed), Align.LEFT, idx);
            initSpeedGraph(graphSpeed);
            addGraph(graphSpeed);
            idx++;
        }
    }

    @Override
    public void update() {
        synchronized (syncObject) {
            Hint.log(this, "update");
            if (!isResumed()) {
                Hint.log(this, "update: !isResumed");
                return;
            }

            if (session == null) {
                Hint.log(this, "update: !session");
                return;
            }

            reset();

            int cluster = 150;
            int clusterSize = Math.max(1, session.getGpsPos().size() / cluster);
            int bufferSize = Math.max(10, (int) (3.0 * clusterSize));
            Hint.log(this, "Pos: " + session.getGpsPos().size() + ", clusterSize: " + clusterSize + ", bufferSize: "
                    + bufferSize);
            DoubleBuffer speedBuffer = new DoubleBuffer(bufferSize);
            DoubleBuffer elevationBuffer = new DoubleBuffer(bufferSize);
            int currentCluster = 0;
            double cXTime = 0;
            // double cYSpeed = 0;
            // double cYAlt = 0;
            double minX = -1;
            double maxX = -1;

            Calendar cal = Calendar.getInstance(); // get calendar instance
            cal.setTime(new Date());
            cal.set(Calendar.YEAR, 1970); // set hour to midnight
            cal.set(Calendar.MONTH, Calendar.JANUARY); // set hour to midnight
            cal.set(Calendar.DAY_OF_MONTH, 10); // set hour to midnight
            cal.set(Calendar.HOUR_OF_DAY, 0); // set hour to midnight
            cal.set(Calendar.MINUTE, 0); // set minute in hour
            cal.set(Calendar.SECOND, 0); // set second in minute
            cal.set(Calendar.MILLISECOND, 0); // set millisecond in second
            long offsetFromUtc = cal.getTimeInMillis();
            Hint.log(this, "OffsetFromUTC: " + offsetFromUtc);

            for (int i = 0; i < session.getGpsPos().size(); ++i) {
                currentCluster++;
                double x, y;
                GpsPos gpsPos = session.getGpsPos().get(i);
                x = gpsPos.duration;
                y = gpsPos.speed * 3.6;
                // Log.d("PaceTracker", "y: " + y);
                cXTime += x;
                // cYSpeed += y;
                // cYAlt += gpsPos.altitude;
                speedBuffer.setNext(y);
                elevationBuffer.setNext(gpsPos.altitude);

                if ((currentCluster == clusterSize) || (i == session.getGpsPos().size() - 1)) {
                    cXTime /= (double) currentCluster;
                    cXTime += offsetFromUtc;
                    // cYSpeed /= (double) currentCluster;
                    // cYAlt /= (double) currentCluster;
                    // Hint.log(this, "mean speed: " + cYSpeed + ", cluster: " +
                    // currentCluster);

                    if (graphType.isSpeed())
                        graphSpeed.add(cXTime, speedBuffer.getAverage() / speedFactor);
                    if (graphType.isElevation())
                        graphElevation.add(cXTime, elevationBuffer.getAverage());

                    if (minX == -1)
                        minX = cXTime;
                    maxX = cXTime;
                    cXTime = 0;
                    // cYSpeed = 0;
                    // cYAlt = 0;
                    currentCluster = 0;
                }
            }


            if (graphType.isPulse() && GlobalSettings.getInstance().isPro()) {
                for (int i = 0; i < session.getHxmData().size(); ++i) {
                    HxmData hxmData = session.getHxmData().get(i);
                    double currentX = hxmData.duration + offsetFromUtc;
                    if ((currentX <= minX) || (currentX >= maxX))
                        continue;
                    if (hxmData.heartRate != -1) {
                        graphPulse.add(currentX, hxmData.heartRate);
                    }
                }
                double height = graphPulse.maxY - graphPulse.minY;
                mRenderer.setInitialRange(new double[]{minX, maxX, Math.max(0, graphPulse.minY - height * 0.2),
                        graphPulse.minY + height * 1.2}, graphPulse.idx);
            }

            if (minX == -1)
                minX = 0;
            maxX = Math.max(maxX, minX);

            if (graphType.isSpeed()) {
                double height = graphSpeed.maxY - graphSpeed.minY;
                mRenderer.setInitialRange(new double[]{minX, maxX, Math.max(0, graphSpeed.minY - height * 0.2),
                        graphSpeed.minY + height * 1.2}, graphSpeed.idx);
            }
            if (graphType.isElevation()) {
                double height = graphElevation.maxY - graphElevation.minY;
                mRenderer.setInitialRange(new double[]{minX, maxX, Math.max(0, graphElevation.minY - height * 0.2),
                        graphElevation.minY + height * 1.2}, graphElevation.idx);
            }
            mSpeedChartView.zoomReset();
            mRenderer.setPanEnabled(true, false);
            // [panMinimumX, panMaximumX, panMinimumY, panMaximumY]
            mRenderer.setPanLimits(new double[]{minX, maxX, 0, 0});
            // [zoomMinimumX, zoomMaximumX, zoomMinimumY, zoomMaximumY]
            mRenderer.setZoomEnabled(true, false);
            // mRenderer.setZoomLimits(new double[]{1, 50, 0, 0});
            mSpeedChartView.repaint();
            Hint.log(this, "range: " + mRenderer.getInitialRange()[0] + ", " + mRenderer.getInitialRange()[1]);
            Hint.log(this, "update end");
        }
    }

    @Override
    public String toString() {
        return fragmentName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Hint.log(this, "selected: " + position);
        graphType = ((TextItemPair<GraphType>) spinnerShowGraph.getItemAtPosition(position)).getItem();
        GlobalSettings.getInstance().put("graphType", graphType.toInt());

        update();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }

}
