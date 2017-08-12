package de.badaix.pacetracker.activity;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Date;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.SessionPersistance;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;

public class FragmentHistoryGraph extends Fragment implements FragmentHistory {
    // private XYPlot mySimpleXYPlot;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(2);
    private XYSeries mCurrentSeries;
    private GraphicalView mChartView;
    private View view;
    private Cursor cursor = null;
    private int typeCount = 0;
    private String fragmentName = "Graph";

    public void setTitle(String title) {
        fragmentName = title;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_history_graph, container, false);
        mRenderer.setApplyBackgroundColor(true);
        // mRenderer.setBackgroundColor(getResources().getColor(R.color.item));
        mRenderer.setBackgroundColor(Color.argb(0x00, 0xFF, 0x00, 0x00));
        mRenderer.setAxisTitleTextSize(17);
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
        mRenderer.setPointSize(5);
        mRenderer.setShowGrid(true);
        mRenderer.setXLabelsAlign(Align.CENTER);
        mRenderer.setYLabelsAlign(Align.RIGHT);
        mRenderer.setYAxisAlign(Align.CENTER, 1);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        onUpdateGui();
    }

    @Override
    public void onPause() {
        if ((cursor != null) && !cursor.isClosed())
            cursor.close();
        super.onPause();
    }

    private GraphicalView createChartView() {
        GraphicalView chartView = ChartFactory.getTimeChartView(this.getActivity(), mDataset, mRenderer, "MM/dd/yyyy");
        // mRenderer.setClickEnabled(true);
        // mRenderer.setSelectableBuffer(100);

        return chartView;
    }

    @Override
    public void onUpdateGui() {
        if (view == null)
            return;

        if (mChartView == null) {
            mChartView = createChartView();
            LinearLayout layout = (LinearLayout) view.findViewById(R.id.history_chart);
            layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        }

        int count = mDataset.getSeriesCount();
        for (int i = 0; i < count; ++i)
            mDataset.removeSeries(0);

        count = mRenderer.getSeriesRenderers().length;
        for (int i = 0; i < count; ++i)
            mRenderer.removeSeriesRenderer(mRenderer.getSeriesRenderers()[0]);

        double maxSpeed = 0;
        double minSpeed = 9999.9;
        double[] limits = {-1, 0, minSpeed, maxSpeed};
        double minDistance = Double.MAX_VALUE;
        double maxDistance = Double.MIN_VALUE;

        if ((cursor == null) || !cursor.moveToFirst()) {
            limits[0] = (new Date()).getTime() - 1000 * 60 * 60 * 24;
            limits[1] += limits[0] + 2 * 1000 * 60 * 60 * 24;
            limits[2] = 0;
            limits[3] = 10;
            XYSeries series = new XYSeries("");
            mDataset.addSeries(series);
        } else {
            int typeCol = cursor.getColumnIndex("type");
            int startCol = cursor.getColumnIndex("start");
            int durationCol = cursor.getColumnIndex("duration");
            int distanceCol = cursor.getColumnIndex("distance");
            String lastType = "";
            double speedFactor = GlobalSettings.getInstance(getActivity()).getDistUnit().getFactor() / 1000.;
            int typeIdx = 0;
            XYSeries distanceSeries = null;
            XYSeriesRenderer distanceRenderer = null;
            do {
                if (!cursor.getString(typeCol).equals(lastType)) {
                    lastType = cursor.getString(typeCol);
                    float[] hsv = {(float) typeIdx / (float) typeCount * 360, 1, 1};
                    Hint.log(this, "Color: " + hsv[0]);
                    typeIdx++;

                    String seriesTitle = SessionFactory.getInstance().getSessionNameFromType(lastType);
                    XYSeries series = new XYSeries(seriesTitle, 0);
                    mDataset.addSeries(0, series);
                    mCurrentSeries = series;
                    XYSeriesRenderer renderer = new XYSeriesRenderer();
                    mRenderer.addSeriesRenderer(0, renderer);
                    String axisTitle = getString(R.string.speed) + " ["
                            + GlobalSettings.getInstance(getActivity()).getDistUnit().perHourString() + "]";
                    mRenderer.setYTitle(axisTitle, 0);
                    renderer.setPointStyle(PointStyle.CIRCLE);
                    renderer.setLineWidth(2);
                    renderer.setFillPoints(true);

                    if (typeCount == 1) {
                        renderer.setColor(getResources().getColor(R.color.blue));
                        distanceSeries = new XYSeries(getString(R.string.distance), 1);
                        distanceRenderer = new XYSeriesRenderer();
                        distanceRenderer.setPointStyle(PointStyle.CIRCLE);
                        distanceRenderer.setLineWidth(0);
                        distanceRenderer.setFillPoints(true);
                        distanceRenderer.setColor(getResources().getColor(R.color.orange));
                        mDataset.addSeries(1, distanceSeries);
                        mRenderer.addSeriesRenderer(1, distanceRenderer);
                        axisTitle = getString(R.string.distance) + " ["
                                + GlobalSettings.getInstance(getActivity()).getDistUnit().toShortString() + "]";
                        mRenderer.setYTitle(axisTitle, 1);
                        mRenderer.setYAxisAlign(Align.RIGHT, 1);
                        mRenderer.setYLabelsAlign(Align.LEFT, 1);
                    } else
                        renderer.setColor(Color.HSVToColor(hsv));
                }

                double speed = cursor.getDouble(distanceCol) / cursor.getLong(durationCol) * 3600. / speedFactor;
                long start = cursor.getLong(startCol);

                if (limits[0] < 0)
                    limits[0] = start;
                limits[0] = Math.min(limits[0], start);
                limits[1] = Math.max(limits[1], start);
                if (minSpeed > speed)
                    minSpeed = speed;
                if (maxSpeed < speed)
                    maxSpeed = speed;
                mCurrentSeries.add(start, speed);

                if (typeCount == 1) {
                    double distance = Distance.distanceToDouble(cursor.getLong(distanceCol));
                    minDistance = Math.min(distance, minDistance);
                    maxDistance = Math.max(distance, maxDistance);
                    distanceSeries.add(start, distance);
                }

            } while (cursor.moveToNext());

            limits[0] -= 1000 * 60 * 60 * 24;
            limits[1] += 1000 * 60 * 60 * 24;
            limits[2] = minSpeed * 0.8;
            limits[3] = maxSpeed * 1.2;
        }

        mRenderer.setInitialRange(limits, 0);
        if (typeCount == 1) {
            double range[] = {limits[0], limits[1], 0, maxDistance * 1.2};
            mRenderer.setInitialRange(range, 1);
        }

        mChartView.zoomReset();
        mRenderer.setPanEnabled(true, false);
        // [panMinimumX, panMaximumX, panMinimumY, panMaximumY]
        mRenderer.setPanLimits(new double[]{limits[0], limits[1], 0, 0});
        // [zoomMinimumX, zoomMaximumX, zoomMinimumY, zoomMaximumY]
        mRenderer.setZoomEnabled(true, false);
        // mRenderer.setZoomLimits(new double[]{1, 50, 0, 0});
        // mRenderer.setPanEnabled(true, true);
        // mRenderer.setPanLimits(limits);
        // mRenderer.setZoomEnabled(true, true);

        mChartView.repaint();
    }

    @Override
    public void onUpdate(String filter) {
        SessionPersistance sessionPersistance = SessionPersistance.getInstance(getActivity());
        if ((cursor != null) && !cursor.isClosed())
            cursor.close();

        try {
            String[] fields = {"count(distinct type)"};
            cursor = sessionPersistance.querySessions(fields, filter, null, "", null);
            if ((cursor != null) && cursor.moveToFirst())
                typeCount = cursor.getInt(0);
        } finally {
            if ((cursor != null) && !cursor.isClosed())
                cursor.close();
        }

        String[] fields = {"type, start, duration, distance"};
        cursor = sessionPersistance.querySessions(fields, filter, null, "type asc, start asc", null);
    }

    @Override
    public String toString() {
        return fragmentName;
    }
}
