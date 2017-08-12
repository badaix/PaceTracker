package de.badaix.pacetracker.activity;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.HxmData;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.TextItemPair;

public class FragmentSessionPaceTable extends Fragment implements
        SessionGUI, OnItemSelectedListener {
    private Vector<TextView> column;
    private TableLayout paceTable;
    private long lLastKilometerDuration = -1;
    private int iLastKilometerIdx = 0;
    private double dNextLap = 0;
    private Session session = null;
    private Spinner spinnerLap;
    private double lapDistance;
    private String fragmentName = "Pace table";

    public void setTitle(String title) {
        fragmentName = title;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater
                .inflate(R.layout.session_pace_table, container, false);
        paceTable = (TableLayout) v.findViewById(R.id.tlPace);
        spinnerLap = (Spinner) v.findViewById(R.id.spinnerLap);
        column = new Vector<TextView>();
        ((TextView) v.findViewById(R.id.colDistance)).setText(GlobalSettings
                .getInstance(getActivity()).getDistUnit().toShortString());
        ((TextView) v.findViewById(R.id.colPace)).setText(R.string.colPace);
        ((TextView) v.findViewById(R.id.colTime)).setText(R.string.colTime);
        ((TextView) v.findViewById(R.id.colSpeed)).setText(GlobalSettings
                .getInstance().getDistUnit().perHourString());
        ((TextView) v.findViewById(R.id.colUp)).setText(R.string.colUp);
        ((TextView) v.findViewById(R.id.colDown)).setText(R.string.colDown);
        addColumn(0, -1, 0, 0, 0, 0);

        int lastPosition = GlobalSettings.getInstance()
                .getInt("lapDistance", 1);
        ArrayAdapter<TextItemPair<Double>> lapAdapter = new ArrayAdapter<TextItemPair<Double>>(
                this.getActivity(), android.R.layout.simple_spinner_item);
        lapAdapter.add(new TextItemPair<Double>("0.5 "
                + GlobalSettings.getInstance().getDistUnit().toShortString(),
                0.5));
        lapAdapter.add(new TextItemPair<Double>("1 "
                + GlobalSettings.getInstance().getDistUnit().toShortString(),
                1.0));
        lapAdapter.add(new TextItemPair<Double>("2 "
                + GlobalSettings.getInstance().getDistUnit().toShortString(),
                2.0));
        lapAdapter.add(new TextItemPair<Double>("5 "
                + GlobalSettings.getInstance().getDistUnit().toShortString(),
                5.0));
        lapAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLap.setAdapter(lapAdapter);
        spinnerLap.setOnItemSelectedListener(this);
        spinnerLap.setSelection(lastPosition);
        // lapDistance = ((TextItemPair<Double>)
        // spinnerLap.getItemAtPosition(lastPosition)).getItem().doubleValue();
        // lapDistance *= GlobalSettings.getInstance().getDistFactor();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        int lastPosition = GlobalSettings.getInstance()
                .getInt("lapDistance", 1);
        spinnerLap.setSelection(lastPosition);
        // update();
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onLocationChanged(Location arg0) {
    }

    public TextView createColumnTv(String text, int column) {
        TextView tv = new TextView(getActivity());
        tv.setText(text);
        tv.setTextAppearance(getActivity(), R.style.SmallFont);
        tv.setPadding(Helper.dipToPix(getActivity(), 4),
                Helper.dipToPix(getActivity(), 2),
                Helper.dipToPix(getActivity(), 4),
                Helper.dipToPix(getActivity(), 3));
        TableRow.LayoutParams params = new TableRow.LayoutParams(column);
        if (column == 1)
            params.setMargins(Helper.dipToPix(getActivity(), 1), 0,
                    Helper.dipToPix(getActivity(), 1),
                    Helper.dipToPix(getActivity(), 1));
        else
            params.setMargins(0, 0, Helper.dipToPix(getActivity(), 1),
                    Helper.dipToPix(getActivity(), 1));

        int color = R.color.item_dark;
        if (paceTable.getChildCount() % 2 == 0)
            color = R.color.item;

        tv.setBackgroundResource(color);
        tv.setLayoutParams(params);
        return tv;
    }

    public void addColumn(double distance, int pace, int time, double speed,
                          int up, int down) {
        column = new Vector<TextView>(0);
        column.add(createColumnTv(Distance.distanceToString(distance, 1), 1));
        if (pace >= 0)
            column.add(createColumnTv(DateUtils.secondsToMMSSString(pace), 2));
        else
            column.add(createColumnTv("N/A", 2));
        column.add(createColumnTv(DateUtils.secondsToMMSSString(time), 3));
        column.add(createColumnTv(Distance.speedToString(speed), 4));
        column.add(createColumnTv(Integer.toString(up), 5));
        column.add(createColumnTv(Integer.toString(down), 6));

        addTableColumn(column);
    }

    public void setColumn(double distance, int pace, int time, double speed,
                          int up, int down) {
        column.get(0).setText(Distance.distanceToString(distance, 1));
        if (pace >= 0)
            column.get(1).setText(DateUtils.secondsToMMSSString(pace));
        else
            column.get(1).setText("N/A");
        column.get(2).setText(DateUtils.secondsToMMSSString(time));
        column.get(3).setText(Distance.speedToString(speed));
        column.get(4).setText(Integer.toString(up));
        column.get(5).setText(Integer.toString(down));
    }

    public void copyColumn() {
        Vector<TextView> newCol = new Vector<TextView>();
        newCol.add(createColumnTv(column.get(0).getText().toString(), 1));
        newCol.add(createColumnTv(column.get(1).getText().toString(), 2));
        newCol.add(createColumnTv(column.get(1).getText().toString(), 3));
        newCol.add(createColumnTv(column.get(2).getText().toString(), 4));
        newCol.add(createColumnTv(column.get(3).getText().toString(), 5));
        newCol.add(createColumnTv(column.get(4).getText().toString(), 6));
        addTableColumn(newCol);
    }

    private void addTableColumn(Vector<TextView> column) {
        TableRow tr = new TableRow(getActivity());
        tr.addView(column.get(0));
        tr.addView(column.get(1));
        tr.addView(column.get(2));
        tr.addView(column.get(3));
        tr.addView(column.get(4));
        tr.addView(column.get(5));
        TableLayout.LayoutParams params = new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        paceTable.addView(tr, /* 1, */params);
        this.column = column;
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
    }

    @Override
    public void onGuiTimer(boolean resumed) {
        if (!resumed)
            return;

        if (session == null)
            return;

        if (session.getGpsPos().size() > 1) {
            if (session.getGpsPos().lastElement().distance > dNextLap)
                update();
            else {
                lLastKilometerDuration = session.getGpsPos().lastElement().duration
                        - session.getGpsPos().get(iLastKilometerIdx).duration;
                double dLastKilometerDistance = session.getGpsPos()
                        .lastElement().distance
                        - session.getGpsPos().get(iLastKilometerIdx).distance;
                if (iLastKilometerIdx == 0)
                    lLastKilometerDuration = session.getDuration();
                if (dLastKilometerDistance > 0)
                    setColumn(
                            session.getDistance(),
                            (int) ((lLastKilometerDuration / dLastKilometerDistance) * (lapDistance / GlobalSettings
                                    .getInstance().getDistUnit().getFactor())),
                            (int) (session.getGpsPos().lastElement().duration / 1000),
                            (float) (dLastKilometerDistance / (lLastKilometerDuration / 1000.)) * 3.6f,
                            0, 0);
            }
        }
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix,
                                   int fixCount, int satCount) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active,
                                     SensorState sensorState) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
    }

    @Override
    public void onSessionCommand(int command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
        Hint.log(this, "selected: " + position);
        @SuppressWarnings("unchecked")
        Double value = ((TextItemPair<Double>) spinnerLap
                .getItemAtPosition(position)).getItem();
        lapDistance = value
                * GlobalSettings.getInstance().getDistUnit().getFactor();
        GlobalSettings.getInstance().put("lapDistance", position);
        update();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public void update() {
        if (!isResumed())
            return;

        if ((session == null) || (lapDistance == 0))
            return;

        Hint.log(this, "update begin");
        while (paceTable.getChildCount() > 1) {
            paceTable.removeViewAt(paceTable.getChildCount() - 1);
        }
        addColumn(0, -1, 0, 0, 0, 0);
        iLastKilometerIdx = 0;
        lLastKilometerDuration = 0;
        int minHeight = 99999;
        int maxHeight = -99999;
        int startHeight = 0;
        int endHeight = 0;

        if (session.getGpsPos().size() > 0) {
            minHeight = (int) session.getGpsPos().firstElement().altitude;
            maxHeight = minHeight;
        }

        dNextLap = lapDistance;
        for (int i = 0; i < session.getGpsPos().size(); ++i) {
            GpsPos gpsPos = session.getGpsPos().get(i);
            if (i + 1 < session.getGpsPos().size()) {
                minHeight = (int) Math.min(minHeight, Math.max(gpsPos.altitude,
                        session.getGpsPos().get(i + 1).altitude));
                maxHeight = (int) Math.max(maxHeight, Math.min(gpsPos.altitude,
                        session.getGpsPos().get(i + 1).altitude));
            } else {
                minHeight = (int) Math.min(minHeight, gpsPos.altitude);
                maxHeight = (int) Math.max(maxHeight, gpsPos.altitude);
            }

            if (gpsPos.distance > dNextLap) {
                // Hint.log(this, "gpsPos.distance > dNextLap (" +
                // gpsPos.distance
                // + " > " + dNextLap + ")");
                startHeight = (int) session.getGpsPos().get(iLastKilometerIdx).altitude;
                endHeight = (int) session.getGpsPos().get(i - 1).altitude;
                int up = Math.max(0, maxHeight - startHeight)
                        + Math.max(0, endHeight - minHeight);
                int down = Math.max(0, startHeight - minHeight)
                        + Math.max(0, maxHeight - endHeight);
                minHeight = endHeight;
                maxHeight = minHeight;
                dNextLap = (1 + (long) gpsPos.distance / (long) lapDistance)
                        * lapDistance;
                lLastKilometerDuration = gpsPos.duration
                        - session.getGpsPos().get(iLastKilometerIdx).duration;
                double dLastKilometerDistance = gpsPos.distance
                        - session.getGpsPos().get(iLastKilometerIdx).distance;
                iLastKilometerIdx = i;
                setColumn(
                        gpsPos.distance,
                        (int) (lLastKilometerDuration / 1000),
                        (int) (gpsPos.duration / 1000),
                        3600. / ((double) lLastKilometerDuration / dLastKilometerDistance),
                        up, down);
                copyColumn();
            }
        }
        if (session.getGpsPos().size() > iLastKilometerIdx + 1) {
            lLastKilometerDuration = session.getGpsPos().lastElement().duration
                    - session.getGpsPos().get(iLastKilometerIdx).duration;
            double dLastKilometerDistance = session.getGpsPos().lastElement().distance
                    - session.getGpsPos().get(iLastKilometerIdx).distance;
            startHeight = (int) session.getGpsPos().get(iLastKilometerIdx).altitude;
            endHeight = (int) session.getGpsPos().lastElement().altitude;
            int up = Math.max(0, maxHeight - startHeight)
                    + Math.max(0, endHeight - minHeight);
            int down = Math.max(0, startHeight - minHeight)
                    + Math.max(0, maxHeight - endHeight);
            setColumn(
                    session.getDistance(),
                    (int) ((lLastKilometerDuration / dLastKilometerDistance) * (lapDistance / GlobalSettings
                            .getInstance().getDistUnit().getFactor())),
                    (int) (session.getGpsPos().lastElement().duration / 1000),
                    3600. / (((double) lLastKilometerDuration / dLastKilometerDistance)),
                    up, down);
        }
        Hint.log(this, "update end");
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
    }

    @Override
    public String toString() {
        return fragmentName;
    }
}
