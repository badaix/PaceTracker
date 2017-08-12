package de.badaix.pacetracker.session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.BoundingBox;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

public class Route extends RouteInfo {
    private Vector<Segment> segments = new Vector<Segment>();
    private Vector<GeoPos> positions = new Vector<GeoPos>();
    private BoundingBox boundingBox = new BoundingBox();
    private JSONObject json = null;

    public Route(File filename) throws IOException, JSONException {
        DataInputStream dis = new DataInputStream(new FileInputStream(filename));
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.read(bytes);
        fromJson(new JSONObject(new String(bytes, "utf-8")));
        this.setFilename(filename.getAbsolutePath());
    }

    public Route() {
    }

    public Route(JSONObject json) throws JSONException {
        fromJson(json);
    }

    public JSONObject toJson() {
        try {
            json = new JSONObject();
            JSONObject shape = new JSONObject();
            JSONArray maneuverIndexes = new JSONArray();
            shape.put("maneuverIndexes", maneuverIndexes);
            JSONArray legIndexes = new JSONArray();
            legIndexes.put(0);
            shape.put("legIndexes", legIndexes);
            shape.put("shapePoints", LocationUtils.compress(this.getPositions(), 5));
            JSONArray legs = new JSONArray();
            JSONObject leg = new JSONObject();
            JSONArray maneuvers = new JSONArray();
            legs.put(leg);
            leg.put("index", 0);
            leg.put("distance", this.getDistance());
            leg.put("maneuvers", maneuvers);

            int idx = 0;
            for (Segment segment : this.getSegments()) {
                maneuverIndexes.put(idx);
                idx += segment.getPositions().size();
                JSONObject maneuver = new JSONObject();
                maneuver.put("distance", segment.getLength());
                maneuver.put("narrative", segment.getInstruction());
                maneuver.put("iconUrl", segment.getImageUrl());
                JSONArray streets = new JSONArray();
                streets.put(segment.getName());
                maneuver.put("streets", streets);
                maneuvers.put(maneuver);
            }

            JSONObject options = new JSONObject();
            options.put("routeType", this.getType());

            JSONObject meta = new JSONObject();
            meta.put("to", this.getTo());
            meta.put("from", this.getFrom());
            meta.put("source", this.getSource());
            meta.put("copyright", this.getCopyright());
            meta.put("name", this.getName());
            meta.put("description", this.getDescription());

            json.put("shape", shape);
            json.put("meta", meta);
            json.put("distance", this.getDistance());
            json.put("legs", legs);
            json.put("options", options);

            return json;
        } catch (JSONException e) {
            Hint.log(this, e);
            return null;
        }
    }

    public void saveToFile(File filename) throws IOException {
        DataOutputStream dos;
        dos = new DataOutputStream(new FileOutputStream(filename));
        byte[] b = toJson().toString().getBytes("utf-8");
        dos.writeInt(b.length);
        dos.write(b);
    }

    protected void fromJson(JSONObject json) throws JSONException {
        this.json = json;

        // Get the leg, only one leg as we don't support
        // waypoints
        final String polyline = json.getJSONObject("shape").getString("shapePoints");
        // Get the steps for this leg
        final JSONArray steps = json.getJSONArray("legs").getJSONObject(0).getJSONArray("maneuvers");
        // Number of steps for use in for loop
        final int numSteps = steps.length();
        // Index of start points in the route shape for each
        // step
        final JSONArray stepIndexes = json.getJSONObject("shape").getJSONArray("maneuverIndexes");

        // Get the total length of the route in meters.
        Hint.log(this, "distance: " + json.getDouble("distance") * 1000);

        Vector<GeoPos> points = LocationUtils.decodePolyLine(polyline, 5);
        if (!points.isEmpty()) {
            points.firstElement().distance = 0.;
            for (int i = 1; i < points.size(); ++i)
                points.get(i).distance = points.get(i - 1).distance
                        + Distance.calculateDistance(points.get(i - 1), points.get(i));
        }
        // route.addPoints(points);

		/*
         * Loop through the steps, creating a segment for each one. Ignore the
		 * last step.
		 */

        for (int i = 0; i < numSteps; i++) {
            Segment segment = new Segment();

            // Get the individual step
            final JSONObject step = steps.getJSONObject(i);
            // Set the length of this segment in metres
            segment.setInstruction(step.getString("narrative"));
            if (step.has("iconUrl"))
                segment.setImageUrl(step.getString("iconUrl"));

            segment.setName(step.getJSONArray("streets").optString(0, ""));

            // Step through point list, using maneuver indexes
            // as start/stop points for each segment
            int stepIndex = stepIndexes.getInt(i);
            int nextStepIndex = i < stepIndexes.length() - 1 ? stepIndexes.getInt(i + 1) : points.size();

            for (int j = stepIndex; j < nextStepIndex; j++) {
                // Retrieve & decode this segment's polyline and
                // add it to the route & segment.
                segment.positions.add(points.get(j));
            }

            // Push a copy of the segment to the route
            addSegment(segment);
        }

        setStartPos(segments.firstElement().positions.firstElement());
        setEndPos(segments.lastElement().positions.lastElement());
        JSONObject meta = json.getJSONObject("meta");
        setCopyright(meta.getString("copyright"));
        setSource(meta.getString("source"));
        setFrom(meta.getString("from"));
        setTo(meta.getString("to"));
        setType(json.getJSONObject("options").getString("routeType").toLowerCase(Locale.getDefault()));

        String description = getFrom() + " "
                + GlobalSettings.getInstance().getContext().getResources().getString(R.string.to) + " " + getTo();
        String name = description;
        if (meta.has("name"))
            name = meta.getString("name");
        if (meta.has("description"))
            description = meta.getString("description");
        setName(name);
        setDescription(description);
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public Vector<GeoPos> getPositions() {
        return positions;
    }

    public void addSegment(Segment segment) {
        if (segment == null)
            return;

        segments.add(segment);
        // Hint.log(this, "new segment (" + segment.getName() + "): " +
        // segment.getInstruction());
        for (GeoPos pos : segment.positions) {
            positions.add(pos);
            boundingBox.add(pos);
        }
    }

    public Vector<Segment> getSegments() {
        return segments;
    }

    @Override
    public double getDistance() {
        if (segments.isEmpty())
            return super.getDistance();

        return segments.lastElement().getDistance() + segments.lastElement().getLength();
    }

    @Override
    public GeoPos getStartPos() {
        if (segments.isEmpty())
            return super.getStartPos();

        return segments.firstElement().positions.firstElement();
    }

    @Override
    public GeoPos getEndPos() {
        if (segments.isEmpty())
            return super.getEndPos();

        return segments.lastElement().positions.lastElement();
    }
}
