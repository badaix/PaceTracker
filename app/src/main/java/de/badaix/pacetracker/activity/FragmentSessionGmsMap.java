package de.badaix.pacetracker.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ZoomControls;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPosition.Builder;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.maps.BitmapMarker;
import de.badaix.pacetracker.maps.CachedTileProvider;
import de.badaix.pacetracker.maps.GpsPolyline;
import de.badaix.pacetracker.maps.MarkerInfo;
import de.badaix.pacetracker.maps.MarkerInfo.MarkerType;
import de.badaix.pacetracker.maps.OfflineDownloader;
import de.badaix.pacetracker.maps.OfflineDownloader.LocalBinder;
import de.badaix.pacetracker.maps.TileSourceFactory;
import de.badaix.pacetracker.maps.TileSourceFactory.TileSource;
import de.badaix.pacetracker.maps.UrlMarker;
import de.badaix.pacetracker.posprovider.GpsPositionProvider;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.session.Segment;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.BoundingBox;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;
import de.badaix.pacetracker.views.MapTypeDialog;
import de.badaix.pacetracker.views.TileDownloadDialog;

public class FragmentSessionGmsMap extends FragmentSessionMap implements OnCameraChangeListener, OnMapClickListener, OnMapReadyCallback,
        LocationSource, View.OnTouchListener {
    private final long ROTATE_TIMEOUT = 3000;
    // private Polyline routePolyLine = null;
    float zoom = 1;
    double nextMarker = 0;
    private CachedTileProvider tileBaseProvider = null;
    private CachedTileProvider tileOverlayProvider = null;
    private Vector<TileOverlay> tileOverlays = new Vector<TileOverlay>();
    private GpsPolyline gpsPolyline = null;
    private Vector<MarkerInfo> sessionMarker;
    private Vector<MarkerInfo> routeMarker;
    // private Vector<com.google.android.gms.maps.model.Marker> routeMarker;
    private ZoomControls zoomControls;
    private HideZoomControls hideZoomControls = null;
    private MapTypeDialog mapTypeDialog;
    private boolean rotate = false;
    private boolean follow = false;
    private long lastLocationChange = 0;
    private int lastLocationBearing = -999;
    // private Bundle savedInstanceState = null;
    private GeoPos currentLocation;
    private Polyline routePolyLine = null;
    private boolean shouldZoomToRoute = false;
    //private MenuItem downloadItem;
    private LocationSource.OnLocationChangedListener locationChangedListener;
    private GpsPositionProvider gpsProvider = null;
    private boolean trackMyLocation = false;
    private boolean locked = false;
    private GoogleMap map = null;

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        tileDownloadDialog = new TileDownloadDialog();
        if (session != null)
            tileDownloadDialog.setRoute(session.getGpsPos());
        else if (route != null)
            tileDownloadDialog.setRoute(route.getPositions());
        else {
            Vector<GeoPos> v = new Vector<GeoPos>();
            v.add(currentLocation);
            tileDownloadDialog.setRoute(v);
        }

        tileDownloadDialog.setOnClickListener(this);
        tileDownloadDialog.setTileSources(
                TileSource.fromEnumString(GlobalSettings.getInstance().getString("MapBase", "GOOGLETERRAIN")),
                TileSource.fromEnumString("NONE"));//GlobalSettings.getInstance().getString("MapOverlay", "NONE")));
        tileDownloadDialog.show(this.getActivity().getFragmentManager(), "fragment_tile_download");

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Hint.log(this, "onCreateOptionsMenu");
        //downloadItem = menu.add(getString(R.string.downloadMap));
        //downloadItem.setIcon(R.drawable.content_save).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        //downloadItem.setOnMenuItemClickListener(this);
        //downloadItem.setVisible(tileBaseProvider != null);

        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public void lock(boolean lock) {
        Hint.log(this, "Lock: " + lock);
        locked = lock;
    }

    public void getMyLocation(boolean trackMyLocation) {
        gpsProvider = GpsPositionProvider.getInstance(GlobalSettings.getInstance().getContext(), false);
        try {
            gpsProvider.start(this);
            this.trackMyLocation = trackMyLocation;
        } catch (Exception e) {
            Hint.log(this, e);
            if (gpsProvider != null)
                gpsProvider.stop(this);
        }
    }

    @Override
    public boolean zoomToRoute(final boolean animateTo) {
        boolean oldFollow = follow;
        try {
            follow = false;
            if ((getBoundingBox() == null) || !getBoundingBox().isInitialized() || (map == null)) {
                shouldZoomToRoute = true;
                return false;
            }
            shouldZoomToRoute = false;

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds(getBoundingBox()),
                    Helper.dipToPix(getActivity(), 20));

            try {
                if (animateTo)
                    map.animateCamera(cameraUpdate);
                else
                    map.moveCamera(cameraUpdate);
            } catch (IllegalStateException e) {
                // layout not yet initialized
                final View mapView = mapFragment.getView();
                if (mapView.getViewTreeObserver().isAlive()) {
                    mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            zoomToRoute(animateTo);
                        }
                    });
                }
            }

            return true;
        } finally {
            follow = oldFollow;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Hint.log(this, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Hint.log(this, "onMapReady");
        this.map = googleMap;
        setMapType(TileSource.fromEnumString(GlobalSettings.getInstance(getActivity()).getString("MapBase", "GOOGLETERRAIN")),
                TileSource.fromEnumString("NONE"));//GlobalSettings.getInstance().getString("MapOverlay", "NONE")));

        if ((session != null) && session.isStarted() && session.hasGpsInfo())
            currentLocation = session.getGpsPos().lastElement();
        else {
            Location location = LocationUtils.getLastKnownLocation();
            if (location != null)
                currentLocation = new GeoPos(location.getLatitude(), location.getLongitude());
        }

        if (currentLocation != null) {
            Bundle bundle = null;
            if ((session != null) && (session.getSettings() != null))
                bundle = session.getSettings().getBundle();
            if (!restoreCameraPosition(bundle)) {
                map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.latitude,
                                currentLocation.longitude), 16));
                // zoomToRoute(false);
                setFollow(true);
            }
        }

        initRoutePolyLine();
        initSessionPolyLine();
        updateMarkers();
        map.setOnCameraChangeListener(this);
        map.setOnMapClickListener(this);
        map.setIndoorEnabled(false);
        map.setTrafficEnabled(false);
        // if ((session != null) && session.isStarted())
        map.setLocationSource(this);
        map.setMyLocationEnabled(true);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
//		this.getActivity().getSupportActionBar()
//				.setTitle(getActivity().getSupportActionBar().getTitle());
        if (trackMyLocation)
            getMyLocation(trackMyLocation);

        if (!offline && (session != null) && session.hasGpsInfo())
            onFilteredLocationChanged(session.getGpsPos().lastElement());
        if (offline)
            zoomToRoute(false);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Hint.log(this, "onCreateView");

        // mapFragment = new FragmentGms();
        // this.savedInstanceState = savedInstanceState;

        View v = super.onCreateView(inflater, container, savedInstanceState);
        mapFragment.getMapAsync(this);

        zoomControls = (ZoomControls) v.findViewById(R.id.zoomControls);
        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomIn();
            }
        });
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomOut();
            }
        });
        // mapParent = (FrameLayout) view.findViewById(R.id.mapParent);

        sessionMarker = new Vector<MarkerInfo>();
        routeMarker = new Vector<MarkerInfo>();
        // routeMarker = new Vector<com.google.android.gms.maps.model.Marker>();
        Hint.log(this, "onCreateView");
        MapsInitializer.initialize(v.getContext());
        if (shouldZoomToRoute)
            zoomToRoute(false);

        touchFrame.setOnTouchListener(this);

        return v;
    }

    private void storeCameraPosition(Bundle bundle) {
        Hint.log(this, "storeCameraPosition");
        if (map == null)
            return;
        CameraPosition currentPos = map.getCameraPosition();
        bundle.putFloat("bearing", currentPos.bearing);
        bundle.putFloat("tilt", currentPos.tilt);
        bundle.putFloat("zoom", currentPos.zoom);
        bundle.putDouble("latitude", currentPos.target.latitude);
        bundle.putDouble("longitude", currentPos.target.longitude);
        bundle.putBoolean("follow", isFollow());
        bundle.putBoolean("rotate", isRotate());
    }

    // @Override
    // public void onSaveInstanceState(Bundle savedInstanceState) {
    // Hint.log(this, "onSaveInstanceState");
    // super.onSaveInstanceState(savedInstanceState);
    // storeCameraPosition(savedInstanceState);
    // }

    private boolean restoreCameraPosition(final Bundle bundle) {
        Hint.log(this, "restoreCameraPosition");
        if ((map == null) || (bundle == null) || !bundle.containsKey("bearing")) {
            return false;
        }
        Hint.log(this, "restoreCameraPosition not null");
        Builder builder = CameraPosition.builder(map.getCameraPosition());
        builder.bearing(bundle.getFloat("bearing"));
        builder.tilt(bundle.getFloat("tilt"));
        builder.zoom(bundle.getFloat("zoom"));
        builder.target(new LatLng(bundle.getDouble("latitude"), bundle.getDouble("longitude")));
        CameraPosition newPlace = builder.build();
        map.moveCamera(CameraUpdateFactory.newCameraPosition(newPlace));
        setFollow(bundle.getBoolean("follow"));
        setRotate(bundle.getBoolean("rotate"));
        return true;
    }

    @Override
    public void onPause() {
        Hint.log(this, "onPause");
        if (map == null) {
            super.onPause();
            return;
        }
        for (TileOverlay overlay : tileOverlays) {
            if (overlay != null) {
                overlay.remove();
                overlay.clearTileCache();
            }
        }

        tileBaseProvider = null;
        tileOverlayProvider = null;
        tileOverlays.clear();
        if (gpsPolyline != null)
            gpsPolyline.clear();
        gpsPolyline = null;
        for (MarkerInfo markerInfo : sessionMarker)
            if (markerInfo != null)
                markerInfo.clear();
        sessionMarker.clear();
        for (MarkerInfo markerInfo : routeMarker)
            if (markerInfo != null)
                markerInfo.clear();
        routeMarker.clear();
        if (routePolyLine != null)
            routePolyLine.remove();
        routePolyLine = null;
        UrlMarker.clearDescriptors();
        map.setMapType(GoogleMap.MAP_TYPE_NONE);
        map.clear();
        try {
            storeCameraPosition(session.getSettings().getBundle());
        } catch (Exception e) {
        }
        if (gpsProvider != null)
            gpsProvider.stop(this);
        System.gc();
        super.onPause();
    }

    @Override
    public void onResume() {
        Hint.log(this, "onResume");
        super.onResume();
        mapFragment.getMapAsync(this);
    }

    protected LatLngBounds latLngBounds(BoundingBox boundingBox) {
        return new LatLngBounds(new LatLng(boundingBox.getMinLat(), boundingBox.getMinLon()), new LatLng(
                boundingBox.getMaxLat(), boundingBox.getMaxLon()));
    }

    protected BoundingBox latLngBounds(LatLngBounds boundingBox) {
        return new BoundingBox(boundingBox.southwest.latitude, boundingBox.southwest.longitude,
                boundingBox.northeast.latitude, boundingBox.northeast.longitude);
    }

    protected LatLng latLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    private void setMapType(TileSource baseTileSource, TileSource overlayTileSource) {
        if (map == null)
            return;
        GlobalSettings.getInstance().put("MapBase", baseTileSource.toEnumString());
        GlobalSettings.getInstance().put("MapOverlay", "NONE");//overlayTileSource.toEnumString());
        map.setMapType(GoogleMap.MAP_TYPE_NONE);

        tileBaseProvider = null;
        tileOverlayProvider = null;
        for (TileOverlay overlay : tileOverlays) {
            if (overlay != null) {
                overlay.remove();
                overlay.clearTileCache();
            }
        }

        setMapCopyright("");

        if (TileSource.GOOGLE.equals(baseTileSource))
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        else if (TileSource.GOOGLESATELLITE.equals(baseTileSource))
            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        else if (TileSource.GOOGLETERRAIN.equals(baseTileSource))
            map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        //if (downloadItem != null)
        //    downloadItem.setVisible(tileBaseProvider != null);
        tileOverlayProvider = TileSourceFactory.getTileProvider(getActivity(), overlayTileSource);

        if (tileBaseProvider != null) {
            tileOverlays.add(map
                    .addTileOverlay(new TileOverlayOptions().zIndex(-2).tileProvider(tileBaseProvider)));
        }
        if (tileOverlayProvider != null) {
            tileOverlays.add(map.addTileOverlay(
                    new TileOverlayOptions().zIndex(-1).tileProvider(tileOverlayProvider)));
        }
        validateZoom();
    }

    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton == DialogInterface.BUTTON_NEGATIVE)
            return;

        if ((tileDownloadDialog != null) && (dialog.equals(tileDownloadDialog.getDialog()))) {
            Intent intent = new Intent(this.getActivity(), OfflineDownloader.class);
            this.getActivity().bindService(intent, new ServiceConnection() {

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    LocalBinder binder = (LocalBinder) service;
                    OfflineDownloader mService = binder.getService();
                    mService.download(tileDownloadDialog.getJobs());
                    getActivity().unbindService(this);
                }
            }, Context.BIND_AUTO_CREATE);
            this.getActivity().startService(intent);

        } else if ((mapTypeDialog != null) && (dialog.equals(mapTypeDialog.getDialog()))) {
            setMapType(mapTypeDialog.getBaseTileSource(), mapTypeDialog.getOverlayTileSource());
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnCenter) {
            setFollow(false);
            btnMyLocation.setImageResource(R.drawable.ic_menu_mylocation);
            zoomToRoute(true);
        } else if (v == btnMyLocation) {
            if ((map == null) || (currentLocation == null))
                return;

            GeoPos cameraPos = new GeoPos(map.getCameraPosition().target.latitude,
                    map.getCameraPosition().target.longitude);

            if (isFollow())
                setFollow(false);
            else if (Distance.calculateDistance(currentLocation, cameraPos) <= 50)
                setFollow(!isFollow());

            if (map.getCameraPosition().zoom < 14) {
                map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.latitude,
                                currentLocation.longitude), 16));
            } else {
                map.animateCamera(
                        CameraUpdateFactory.newLatLng(new LatLng(currentLocation.latitude, currentLocation.longitude)));
            }
        } else if (v == toggleBtnSattelite) {
            mapTypeDialog = new MapTypeDialog();
            mapTypeDialog.setOnClickListener(this);
            mapTypeDialog.setBaseTileSource(TileSource.fromEnumString(GlobalSettings.getInstance().getString("MapBase", "GOOGLETERRAIN")));
            mapTypeDialog.setOverlayTileSource(TileSource.fromEnumString("NONE"));//GlobalSettings.getInstance().getString("MapOverlay", "NONE")));
            mapTypeDialog.show(this.getActivity().getFragmentManager(), "fragment_map_type");
        } else if (v == imageViewCompass) {
            setRotate(!isRotate());
            updateCamera(isRotate() ? lastCompassBearing : 0, null);
        }
    }

    private Marker addMarker(MarkerInfo markerInfo) {
        GeoPos pos = markerInfo.pos;
        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(pos.latitude, pos.longitude)).snippet(
                markerInfo.snipped);
        if (markerInfo.markerType != MarkerType.url) {
            String title = Distance.distanceToString(pos.distance, 1) + " "
                    + GlobalSettings.getInstance().getDistUnit().toShortString();
            markerOptions = markerOptions.title(title);
            BitmapMarker bitmapMarker = new BitmapMarker(getActivity(), map, markerOptions, markerInfo,
                    14, markerInfo.text);
            markerInfo.marker = bitmapMarker;
            return markerInfo.marker.getMarker();
        } else {
            markerOptions = markerOptions.title(markerInfo.text);
            UrlMarker urlMarker = new UrlMarker(getActivity(), map, markerOptions, markerInfo);
            urlMarker.getMarker();
            markerInfo.marker = urlMarker;
            return markerInfo.marker.getMarker();
        }
    }

    private void initRouteMarker() {
        if ((route == null) || (map == null))
            return;

        for (Segment segment : route.getSegments()) {
            String title = Distance.distanceToString(segment.getDistance(), 1) + " "
                    + GlobalSettings.getInstance().getDistUnit().toShortString();
            if (segment.getName().length() > 0)
                title = title + ": " + segment.getName();

            GeoPos pos = new GeoPos(segment.positions.firstElement().latitude,
                    segment.positions.firstElement().longitude, segment.positions.firstElement().distance);
            // UrlMarker urlMarker =
            routeMarker.add(new MarkerInfo(segment.getImageUrl(), pos, title, segment.getInstruction()));
        }
    }

    private void initSessionMarker() {
        if ((session == null) || (map == null))
            return;

        nextMarker = 0;
        String snipped = "";
        for (GpsPos pos : session.getGpsPos()) {
            if ((pos.distance >= nextMarker) && (map != null)) {
                MarkerType markerType = MarkerType.normal;
                if (nextMarker == 0)
                    markerType = MarkerType.start;

                snipped = DateUtils.secondsToHHMMSSString(pos.duration / 1000);
                sessionMarker.add(new MarkerInfo(markerType, pos, Distance.distanceToString(nextMarker, 0), snipped));
                nextMarker += GlobalSettings.getInstance().getDistUnit().getFactor();
            }
        }
        if (session.isStopped() && session.hasGpsInfo()) {
            GpsPos pos = session.getGpsPos().lastElement();
            snipped = DateUtils.secondsToHHMMSSString(pos.duration / 1000);
            sessionMarker.add(new MarkerInfo(MarkerType.finish, session.getGpsPos().lastElement(), Distance
                    .distanceToString(pos.distance, 1), snipped));
        }
    }

    private void initSessionPolyLine() {
        if ((session == null) || (map == null))
            return;
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(getResources().getColor(R.color.blue)).zIndex(1.0f);
        gpsPolyline = new GpsPolyline(getActivity(), map, polylineOptions, session.getGpsPos());
        initSessionMarker();
    }

    private void initRoutePolyLine() {
        if ((route == null) || (map == null) || (routePolyLine != null))
            return;
        setRouteCopyright(Html.fromHtml(route.getCopyright()));
        PolylineOptions routeOptions = new PolylineOptions();
        routeOptions.color(Color.RED);
        for (GeoPos pos : route.getPositions()) {
            LatLng latLng = new LatLng(pos.latitude, pos.longitude);
            routeOptions.add(latLng);
        }
        initRouteMarker();
        routePolyLine = map.addPolyline(routeOptions);
    }

    @Override
    public void setSession(Session session) {
        Hint.log(this, "setSession");
        super.setSession(session);
        if (session.getSettings().getGoal() != null) {
            session.getSettings().getGoal().addSessionUI(this);
        }
        // if (!isOffline() && (map != null))
        // map.setLocationSource(this);
        initSessionPolyLine();
    }

    @Override
    public void setRoute(Route route) {
        Hint.log(this, "setRoute");
        super.setRoute(route);
        initRoutePolyLine();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Hint.log(this, "onLocationChanged: " + location.getLatitude() + ", "
        // + location.getLongitude());
        if (gpsProvider != null) {
            Hint.log(this, "Accuracy: " + location.getAccuracy() + ", Speed: " + location.getSpeed());
            if (((location.getSpeed() == 0.0f) && (location.getAccuracy() <= 32.0f))
                    || (location.getAccuracy() <= 16.0f)) {
                Location loc = new Location("");
                loc.setLatitude(location.getLatitude());
                loc.setLongitude(location.getLongitude());
                loc.setAccuracy(location.getAccuracy());
                if (locationChangedListener != null)
                    locationChangedListener.onLocationChanged(loc);
                currentLocation = new GeoPos(loc.getLatitude(), loc.getLongitude());
                onFilteredLocationChanged(new GpsPos(location.getLatitude(), location.getLongitude(),
                        location.getAltitude(), location.getTime(), location.getSpeed(), location.getBearing(), 0, 0));
                if (!trackMyLocation) {
                    gpsProvider.stop(this);
                    gpsProvider = null;
                }
            }
        } else if (locationChangedListener != null)
            locationChangedListener.onLocationChanged(location);

        super.onLocationChanged(location);
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
        Hint.log(this, "onFilteredLocationChanged: " + location.latitude + ", " + location.longitude);
        currentLocation = location;
        if (map == null)
            return;

        int bearing = (int) location.bearing;
        lastLocationChange = SystemClock.elapsedRealtime();
        boolean doCameraUpdate = isFollow();
        if (Math.abs(bearing - lastLocationBearing) >= 5) {
            lastLocationBearing = bearing;
            doCameraUpdate = isRotate() || doCameraUpdate;
        }
        LatLng latLng = null;
        if (isFollow())
            latLng = new LatLng(location.latitude, location.longitude);

        if (doCameraUpdate)
            updateCamera(isRotate() ? (int) location.bearing : -1, latLng);

        // List<LatLng> points = sessionPolyLine.getPoints();
        // points.add(new LatLng(location.latitude, location.longitude));
        // sessionPolyLine.setPoints(points);
        // Hint.log(this, "new location: " + location.latitude + ", "
        // + location.longitude);
        if (gpsPolyline != null) {
            gpsPolyline.addGpsPos(location);

            if ((location.distance >= nextMarker) && (map != null)) {
                MarkerType markerType = MarkerType.normal;
                if (nextMarker == 0)
                    markerType = MarkerType.start;

                String snipped = DateUtils.secondsToHHMMSSString(location.duration / 1000);
                sessionMarker.add(new MarkerInfo(markerType, location, Distance.distanceToString(nextMarker, 0),
                        snipped));
                addMarker(sessionMarker.lastElement());
                nextMarker += GlobalSettings.getInstance().getDistUnit().getFactor();
            }
        } else {
            initSessionPolyLine();
            initSessionMarker();
            initRoutePolyLine();
        }
    }

    private void updateMarkers() {
        if (map == null)
            return;

        long nextN = 0;
        int everyN = 1;

        float zoom = map.getCameraPosition().zoom;
        int power = Math.max(0, 22 - (int) zoom);
        everyN = (int) Math.pow(2, power);
        if (everyN > 1000)
            everyN = everyN - (everyN % 1000);
        Hint.log(this, "OnCameraChange zoom: " + zoom + ", power: " + power + ", everyN: " + everyN);

        int everyRouteN = zoom >= 16 ? 1 : everyN / 3;
        for (int i = 0; i < routeMarker.size(); ++i) {
            boolean visible = (i >= routeMarker.size() - 1);
            if (routeMarker.get(i).pos.distance >= nextN) {
                visible = true;
                nextN = (long) (1 + Math.ceil(routeMarker.get(i).pos.distance / everyRouteN)) * everyRouteN;
                Hint.log(this, "Visible: " + routeMarker.get(i).pos.distance + ", " + nextN);
            }

            if (visible && (routeMarker.get(i).marker == null)) {
                Hint.log(this, "RouteMarker (" + i + ", " + routeMarker.get(i).pos.distance + ") == null. Creating.");
                addMarker(routeMarker.get(i));
            }

            if (routeMarker.get(i).marker != null)
                routeMarker.get(i).marker.setVisible(visible);
        }

        nextN = 0;
        for (int i = 0; i < sessionMarker.size(); ++i) {
            boolean visible = false;
            if (isOffline() && (i == sessionMarker.size() - 1))
                visible = true;

            if (sessionMarker.get(i).pos.distance >= nextN) {
                visible = true;
                // nextN = (long)(1 +
                // Math.ceil(sessionMarker.get(i).pos.distance / everyN)) *
                // everyN;
                nextN = nextN + everyN;
            }

            // VisibleRegion region =
            // map.getProjection().getVisibleRegion();
            // LatLng pos = new LatLng(sessionMarker.get(i).pos.latitude,
            // sessionMarker.get(i).pos.longitude);
            // Hint.log(this, "Contains (" + i + "): " +
            // region.latLngBounds.contains(pos));

            if (!visible && (sessionMarker.get(i).marker != null)) {
                Hint.log(this, "Marker (" + i + ") not visible. Deleting.");
                sessionMarker.get(i).clear();
            }

            if (visible && (sessionMarker.get(i).marker == null)) {
                Hint.log(this, "Marker (" + i + ") == null. Creating.");
                addMarker(sessionMarker.get(i));
            }

            if (sessionMarker.get(i).marker != null)
                sessionMarker.get(i).marker.setVisible(visible);
        }
    }

    @Override
    public void onCameraChange(CameraPosition position) {
        if (position.zoom != zoom) {
            showZoomControls();
            zoom = position.zoom;
            Hint.log(this, "OnCameraChange zoom: " + position.zoom);
            validateZoom();
            updateMarkers();
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        showZoomControls();
    }

    private float getMaxZoomLevel() {
        if (map == null)
            return 16;
        float max = map.getMaxZoomLevel();
        if (tileBaseProvider != null)
            max = Math.min(max, tileBaseProvider.getTileSource().getZoomMaxLevel());
        if (tileOverlayProvider != null)
            max = Math.min(max, tileOverlayProvider.getTileSource().getZoomMaxLevel());
        return max;
    }

    private float getMinZoomLevel() {
        if (map == null)
            return 0;
        float min = map.getMinZoomLevel();
        if (tileBaseProvider != null)
            min = Math.max(min, tileBaseProvider.getTileSource().getZoomMinLevel());
        if (tileOverlayProvider != null)
            min = Math.max(min, tileOverlayProvider.getTileSource().getZoomMinLevel());
        return min;
    }

    private void validateZoom() {
        if (map == null)
            return;

        if (map.getCameraPosition().zoom >= getMaxZoomLevel()) {
            zoomControls.setIsZoomInEnabled(false);
            map.animateCamera(CameraUpdateFactory.zoomTo(getMaxZoomLevel()));
        } else
            zoomControls.setIsZoomInEnabled(true);

        if (map.getCameraPosition().zoom <= getMinZoomLevel()) {
            zoomControls.setIsZoomOutEnabled(false);
            map.animateCamera(CameraUpdateFactory.zoomTo(getMinZoomLevel()));
        } else
            zoomControls.setIsZoomOutEnabled(true);
    }

    private boolean zoomIn() {
        if (map == null)
            return false;
        float currentZoom = map.getCameraPosition().zoom;
        float zoomTo = (float) Math.ceil(currentZoom);
        if (zoomTo == currentZoom)
            ++zoomTo;
        zoomTo = Math.min(zoomTo, getMaxZoomLevel());

        map.animateCamera(CameraUpdateFactory.zoomTo(zoomTo));
        showZoomControls();
        zoomControls.setIsZoomInEnabled(zoomTo < getMaxZoomLevel());
        zoomControls.setIsZoomOutEnabled(zoomTo > getMinZoomLevel());
        return (zoomTo >= getMaxZoomLevel());
    }

    private boolean zoomOut() {
        if (map == null)
            return false;
        float currentZoom = map.getCameraPosition().zoom;
        float zoomTo = (float) Math.floor(currentZoom);
        if (zoomTo == currentZoom)
            --zoomTo;
        zoomTo = Math.max(zoomTo, getMinZoomLevel());

        map.animateCamera(CameraUpdateFactory.zoomTo(zoomTo));
        showZoomControls();
        zoomControls.setIsZoomInEnabled(zoomTo < getMaxZoomLevel());
        zoomControls.setIsZoomOutEnabled(zoomTo > getMinZoomLevel());
        return (zoomTo <= getMinZoomLevel());
    }

    private void showZoomControls() {
        if (this.getView() == null)
            return;
        zoomControls.setVisibility(View.VISIBLE);
        this.getView().removeCallbacks(hideZoomControls);
        hideZoomControls = new HideZoomControls();
        this.getView().postDelayed(hideZoomControls, ViewConfiguration.getZoomControlsTimeout());
    }

    public void updateCamera(int bearing, LatLng latLng) {
        if (map == null)
            return;
        CameraPosition currentPos = map.getCameraPosition();
        Builder builder = CameraPosition.builder(currentPos);
        if (bearing >= 0)
            builder.bearing(bearing);
        if (latLng != null)
            builder.target(latLng);
        CameraPosition newPlace = builder.build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(newPlace));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            if ((event.values != null) && compassActive) {
                int azimuth = (int) event.values[0];
                if (Math.abs(azimuth - lastCompassBearing) >= 5) {
                    // Hint.log(this, Integer.toString(azimuth) + " " +
                    // this.isVisible());
                    if (this.isVisible())
                        compass.drawCompass(imageViewCompass, azimuth);
                    lastCompassBearing = azimuth;
                    if (isRotate()) {
                        if (SystemClock.elapsedRealtime() - lastLocationChange > ROTATE_TIMEOUT)
                            updateCamera(lastCompassBearing + compass.getDisplayOrientation(), null);
                    }
                }
            }
        }
    }

    private boolean isRotate() {
        return rotate;
    }

    private void setRotate(boolean rotate) {
        this.rotate = rotate;
    }

    private boolean isFollow() {
        return follow;
    }

    private void setFollow(boolean follow) {
        if (isOffline())
            follow = false;
        this.follow = follow;
        if (follow)
            btnMyLocation.setImageResource(R.drawable.ic_menu_mylocation_lock);
        else
            btnMyLocation.setImageResource(R.drawable.ic_menu_mylocation);
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        locationChangedListener = listener;
        if (currentLocation != null) {
            Location loc = new Location("");
            loc.setLatitude(currentLocation.latitude);
            loc.setLongitude(currentLocation.longitude);
            loc.setAccuracy(5);
            locationChangedListener.onLocationChanged(loc);
        }
    }

    @Override
    public void deactivate() {
        locationChangedListener = null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (map == null)
            return false;

        if (locked) {
            map.getUiSettings().setScrollGesturesEnabled(true);
            return false;
        }

//		Hint.log(this, "onTouch: " + event.getAction() + " " + event.getX() + ", " + event.getY() + ", " + touchFrame.getWidth());
        int left = touchFrame.getWidth() / 5;
        int right = touchFrame.getWidth() - touchFrame.getWidth() / 5;
        // TODO Auto-generated method stub
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if ((event.getX() < left) || (event.getX() > right)) {
                map.getUiSettings().setScrollGesturesEnabled(false);
                return false;
            }
        }

        map.getUiSettings().setScrollGesturesEnabled(true);
        return false;
    }

    private class HideZoomControls implements Runnable {
        @Override
        public void run() {
            zoomControls.setVisibility(View.INVISIBLE);
        }
    }

}
