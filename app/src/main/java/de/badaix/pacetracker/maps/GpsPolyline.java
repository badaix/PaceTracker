package de.badaix.pacetracker.maps;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;
import java.util.Vector;

import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

public class GpsPolyline {
    private final GoogleMap googleMap;
    private final int MAX_POINTS = 30;
    private final int MAX_LINES = 20;
    private final PolylineOptions origOptions;
    private PolylineOptions polylineOptions;
    private Vector<Polyline> polyLines = null;
    private Polyline polyLine = null;
    private Vector<GpsPos> positions = null;

    // private Random random;

    public GpsPolyline(final Context context, final GoogleMap googleMap, final PolylineOptions polylineOptions,
                       Vector<GpsPos> gpsPos) {
        // random = new Random();
        this.googleMap = googleMap;
        this.origOptions = polylineOptions;
        this.polylineOptions = createPolylineOptions(null);
        this.polyLines = new Vector<Polyline>();
        this.positions = new Vector<GpsPos>();
        if (gpsPos != null) {
            List<GpsPos> decimated = new Vector<GpsPos>();
            LocationUtils.decimate(2.0, gpsPos, decimated);
            for (GpsPos pos : decimated) {
                addGpsPos(pos);
            }
            if (!decimated.isEmpty())
                positions.add(decimated.get(decimated.size() - 1));

            polyLine = googleMap.addPolyline(this.polylineOptions);
            polyLines.add(polyLine);
        }

        polyLine = addNewPolyLine();
    }

    public void clear() {
        for (Polyline line : polyLines)
            line.remove();
        polyLines.clear();
        polyLines = null;
        polyLine.remove();
        polyLine = null;
    }

    private Polyline addNewPolyLine() {
        if (polyLines.size() > MAX_LINES) {
            Hint.log(this, "more than " + MAX_LINES + " lines, combining...");
            polylineOptions = createPolylineOptions(null);
            for (Polyline line : polyLines) {
                polylineOptions.addAll(line.getPoints());
                line.remove();
            }
            polyLines.clear();
            polyLines.add(googleMap.addPolyline(polylineOptions));
            Hint.log(this, "...done");
        }

        polylineOptions = createPolylineOptions(null);
        if (!polyLines.isEmpty()) {
            List<LatLng> list = polyLines.lastElement().getPoints();
            if ((list != null) && !list.isEmpty())
                polylineOptions.add(list.get(list.size() - 1));
        }
        final Polyline polyLine = googleMap.addPolyline(this.polylineOptions);
        polyLines.add(polyLine);
        Hint.log(this, "addNewPolyLine: " + polyLines.size());
        return polyLine;
    }

    private PolylineOptions createPolylineOptions(PolylineOptions polylineOptions) {
        if (polylineOptions == null)
            polylineOptions = origOptions;
        PolylineOptions options = new PolylineOptions();
        options.color(polylineOptions.getColor()).width(polylineOptions.getWidth()).zIndex(polylineOptions.getZIndex());
        // options.color(Color.rgb(random.nextInt(256), random.nextInt(256),
        // random.nextInt(256)));
        return options;
    }

    public void addGpsPos(GpsPos gpsPos) {
        LatLng latLng = new LatLng(gpsPos.latitude, gpsPos.longitude);
        if (polyLine != null) {
            positions.add(gpsPos);
            if (positions.size() >= MAX_POINTS) {
                List<GpsPos> decimated = new Vector<GpsPos>();
                LocationUtils.decimate(2.0, positions, decimated);
                Hint.log(this, "positions.size() > " + MAX_POINTS + ": decimate " + positions.size() + " to "
                        + decimated.size());
                List<LatLng> points = new Vector<LatLng>();
                for (GpsPos pos : decimated)
                    points.add(new LatLng(pos.latitude, pos.longitude));
                polyLine.setPoints(points);
                positions.clear();
                positions.add(gpsPos);
                polyLine = addNewPolyLine();
            } else {
                List<LatLng> points = polyLine.getPoints();
                points.add(latLng);
                polyLine.setPoints(points);
            }
        } else
            polylineOptions.add(latLng);
    }

    public Polyline getPolyline() {
        return polyLine;
    }

}
