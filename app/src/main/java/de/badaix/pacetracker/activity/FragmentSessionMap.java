package de.badaix.pacetracker.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.HxmData;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.BoundingBox;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;
import de.badaix.pacetracker.views.TileDownloadDialog;
import de.badaix.pacetracker.widgets.Compass;

public abstract class FragmentSessionMap extends Fragment implements SensorEventListener, SessionGUI,
        OnClickListener, android.content.DialogInterface.OnClickListener, MenuItem.OnMenuItemClickListener {
    protected Session session = null;
    protected Route route = null;
    protected TextView tvMapPos = null;
    protected TextView tvMapSpeed = null;
    protected ImageView toggleBtnSattelite;
    protected ImageView imageViewCompass;
    protected ImageView btnCenter;
    protected ImageView btnMyLocation;
    protected String sSatCount = "N/A";
    protected String sAlt = "N/A";
    protected String sBearing = "N/A";
    protected String sSpeed = "N/A";
    protected String sLat = "N/A";
    protected String sLon = "N/A";
    protected Compass compass = null;
    protected boolean compassActive;
    protected int lastCompassBearing = -999;
    protected TextView textViewMapCopyright;
    protected TextView textViewRouteCopyright;
    protected boolean offline = false;
    protected LocationManager lm;
    protected SensorManager mSensorManager;
    protected TileDownloadDialog tileDownloadDialog;
    protected FragmentGms mapFragment = null;
    protected View touchFrame;
    private LinearLayout linearLayoutMapGps = null;
    private String fragmentName = "Map";

    public void setTitle(String title) {
        fragmentName = title;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) this.getActivity().getSystemService(Context.SENSOR_SERVICE);
        setCompassActive(true);
        setOffline(offline);
    }

    @Override
    public void onResume() {
        super.onResume();
        Hint.log(this, "onResume");
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        Hint.log(this, "onPause");
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlobalSettings.getInstance(this.getActivity());
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;

        if (linearLayoutMapGps == null)
            return;

        if (offline)
            linearLayoutMapGps.setVisibility(View.GONE);
        else
            linearLayoutMapGps.setVisibility(View.VISIBLE);
    }

    public void setCompassActive(boolean active) {
        compassActive = active;
    }

    abstract public void lock(boolean lock);

    abstract public boolean zoomToRoute(boolean animateTo);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        Hint.log(this, "SessionMapFragment.onCreateView");
        View view = inflater.inflate(R.layout.fragment_session_map, null, false);
        // tvMapLat = (TextView)view.findViewById(R.id.tvMapLat);
        tvMapPos = (TextView) view.findViewById(R.id.tvMapPos);
        tvMapSpeed = (TextView) view.findViewById(R.id.tvMapSpeed);
        // zoomControls = (ZoomControls) view.findViewById(R.id.zoomControls);

        mapFragment = (FragmentGms) getChildFragmentManager().findFragmentById(R.id.myMap);
        Hint.log(this, "SessionMapFragment.onCreateView mapFragment == null: " + Boolean.toString(mapFragment == null));
        // mapParent.addView(zoomButtonsController.getContainer());
        // zoomButtonsController.get

        textViewMapCopyright = (TextView) view.findViewById(R.id.textViewMapCopyright);
        textViewMapCopyright.setMovementMethod(LinkMovementMethod.getInstance());
        textViewRouteCopyright = (TextView) view.findViewById(R.id.textViewRouteCopyright);
        textViewRouteCopyright.setMovementMethod(LinkMovementMethod.getInstance());
        setMapCopyright("");
        setRouteCopyright("");
        btnCenter = (ImageView) view.findViewById(R.id.buttonCenterMap);
        btnCenter.setOnClickListener(this);
        btnMyLocation = (ImageView) view.findViewById(R.id.buttonMyLocation);
        btnMyLocation.setOnClickListener(this);
        toggleBtnSattelite = (ImageView) view.findViewById(R.id.buttonMapMode);
        toggleBtnSattelite.setOnClickListener(this);
        imageViewCompass = (ImageView) view.findViewById(R.id.compass);
        imageViewCompass.setOnClickListener(this);
        compass = new Compass(getActivity());

        linearLayoutMapGps = (LinearLayout) view.findViewById(R.id.linearLayoutMapGps);
        setOffline(offline);

        touchFrame = (View) view.findViewById(R.id.touchFrame);
        return view;
    }

    public void setMapCopyright(String copyright) {
        setMapCopyright(new SpannedString(copyright));
    }

    public void setMapCopyright(Spanned copyright) {
        if (TextUtils.isEmpty(copyright))
            textViewMapCopyright.setVisibility(View.GONE);
        else
            textViewMapCopyright.setVisibility(View.VISIBLE);

        textViewMapCopyright.setText(copyright);
    }

    public void setRouteCopyright(String copyright) {
        setRouteCopyright(new SpannedString(copyright));
    }

    public void setRouteCopyright(Spanned copyright) {
        if (TextUtils.isEmpty(copyright))
            textViewRouteCopyright.setVisibility(View.GONE);
        else
            textViewRouteCopyright.setVisibility(View.VISIBLE);

        textViewRouteCopyright.setText(copyright);
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
    }

    @Override
    public void onSessionCommand(int command) {
    }

    private void updateGpsStatus() {
        if (tvMapPos != null)
            tvMapPos.setText("Lat: " + sLat + "; Lon: " + sLon + "; Alt: " + sAlt);
        if (tvMapSpeed != null)
            tvMapSpeed.setText("Speed: " + sSpeed + "; Bearing: " + sBearing + "; Sat: " + sSatCount);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!this.isResumed())
            return;

        sLat = Distance.GpsDegToString(location.getLatitude());
        sLon = Distance.GpsDegToString(location.getLongitude());

        sAlt = Distance.doubleToString(location.getAltitude(), 1);
        sBearing = Integer.toString((int) location.getBearing());
        sSpeed = Distance.speedToString(location.getSpeed() * 3.6f);

        updateGpsStatus();
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        sSatCount = Integer.toString(fixCount) + "/" + Integer.toString(satCount);
        updateGpsStatus();
    }

    protected BoundingBox getBoundingBox() {
        BoundingBox bb = null;
        if ((session != null) && (route != null)) {
            bb = new BoundingBox(session.getBoundingBox());
            bb.add(new GeoPos(route.getBoundingBox().getMaxLat(), route.getBoundingBox().getMaxLon()));
            bb.add(new GeoPos(route.getBoundingBox().getMinLat(), route.getBoundingBox().getMinLon()));
        } else if (session != null)
            bb = session.getBoundingBox();
        else if (route != null)
            bb = route.getBoundingBox();

        if ((bb == null) || !bb.isInitialized()) {
            Location lastLocation = LocationUtils.getLastKnownLocation();
            if (lastLocation != null) {
                double lat = lastLocation.getLatitude();
                double lon = lastLocation.getLongitude();
                bb = new BoundingBox(lat - 0.0025, lon - 0.0025, lat + 0.0025, lon + 0.0025);
            }
        }

        return bb;
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    @Override
    public void onGuiTimer(boolean resumed) {
        // TODO Auto-generated method stub

    }

    @Override
    public void update() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
    }

    @Override
    public String toString() {
        return fragmentName;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
    }

    class DirectionIndicator {
        private final int border = 10;
        private final int size = 25;
        private Canvas canvas;
        private Bitmap canvasBitmap;
        private Paint paint;

        public DirectionIndicator() {
            canvasBitmap = Bitmap.createBitmap(size + 2 * border, size + 2 * border, Config.ARGB_8888);
            // Create blank bitmap of equal size
            canvasBitmap.eraseColor(0x00000000);

            // Create canvas
            canvas = new Canvas(canvasBitmap);

            paint = new Paint();
            paint.setDither(true);
            paint.setColor(Color.argb(0xFF, 0x33, 0x71, 0xA3));
            paint.setStyle(Paint.Style.STROKE);// .FILL_AND_STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(7);
        }

        public BitmapDrawable getDrawable(float angle) {
            canvasBitmap.eraseColor(0x00000000);

            canvas.save();
            canvas.rotate(angle, canvas.getWidth() / 2, canvas.getHeight() / 2);
            final Path p = new Path();
            p.moveTo(border, size + border);
            p.lineTo(size / 2 + border, border);
            p.lineTo(size + border, size + border);
            canvas.drawPath(p, paint);
            canvas.restore();

            return new BitmapDrawable(canvasBitmap);
        }

        public void draw(Canvas canvas, Point pos, float bearing) {
            BitmapDrawable dirBitmapDrawable = getDrawable(bearing);
            int sizeX = dirBitmapDrawable.getBitmap().getWidth();
            int sizeY = dirBitmapDrawable.getBitmap().getHeight();
            dirBitmapDrawable.setBounds(pos.x - sizeX / 2, pos.y - sizeY / 2, pos.x + sizeX / 2, pos.y + sizeY / 2);
            dirBitmapDrawable.draw(canvas);

        }
    }

}
