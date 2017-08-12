package de.badaix.pacetracker.util;

/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.settings.GlobalSettings;

/**
 * Utility class for decimating tracks at a given level of precision.
 *
 * @author Leif Hendrik Wilden
 */
public class LocationUtils {
    public static Address lastKnownAddress = null;
    private static Location lastKnownLocation = null;

    /**
     * This is a utility class w/ only static members.
     */
    private LocationUtils() {
    }

    /**
     * Computes the distance on the two sphere between the point c0 and the line
     * segment c1 to c2.
     *
     * @param c0 the first coordinate
     * @param c1 the beginning of the line segment
     * @param c2 the end of the lone segment
     * @return the distance in m (assuming spherical earth)
     */
    public static double distance(final GeoPos c0, final GeoPos c1, final GeoPos c2) {
        if (c1.equals(c2)) {
            return Distance.calculateDistance(c2.latitude, c2.longitude, c0.latitude, c0.longitude, Distance.METERS);
        }

        final double s0lat = c0.latitude / Distance.DEGREES_TO_RADIANS;
        final double s0lng = c0.longitude / Distance.DEGREES_TO_RADIANS;
        final double s1lat = c1.latitude / Distance.DEGREES_TO_RADIANS;
        final double s1lng = c1.longitude / Distance.DEGREES_TO_RADIANS;
        final double s2lat = c2.latitude / Distance.DEGREES_TO_RADIANS;
        final double s2lng = c2.longitude / Distance.DEGREES_TO_RADIANS;

        double s2s1lat = s2lat - s1lat;
        double s2s1lng = s2lng - s1lng;
        final double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
        if (u <= 0) {
            return Distance.calculateDistance(c0.latitude, c0.longitude, c1.latitude, c1.longitude, Distance.METERS);
        }
        if (u >= 1) {
            return Distance.calculateDistance(c0.latitude, c0.longitude, c2.latitude, c2.longitude, Distance.METERS);
        }
        double saLat = c0.latitude - c1.latitude;
        double saLon = c0.longitude - c1.longitude;
        double sbLat = u * (c2.latitude - c1.latitude);
        double sbLon = u * (c2.longitude - c1.longitude);
        double distance = Distance.calculateDistance(saLat, saLon, sbLat, sbLon, Distance.METERS);
        return distance;
    }

    /**
     * Decimates the given locations for a given zoom level. This uses a
     * Douglas-Peucker decimation algorithm.
     *
     * @param <T>
     * @param tolerance in meters
     * @param locations input
     * @param decimated output
     */
    @SuppressWarnings("unchecked")
    public static <T extends GeoPos> void decimate(double tolerance, List<? extends GeoPos> locations, List<T> decimated) {
        final int n = locations.size();
        if (n < 1) {
            return;
        }
        int idx;
        int maxIdx = 0;
        Stack<int[]> stack = new Stack<int[]>();
        double[] dists = new double[n];
        dists[0] = 1;
        dists[n - 1] = 1;
        double maxDist;
        double dist = 0.0;
        int[] current;

        if (n > 2) {
            int[] stackVal = new int[]{0, (n - 1)};
            stack.push(stackVal);
            while (stack.size() > 0) {
                current = stack.pop();
                maxDist = 0;
                for (idx = current[0] + 1; idx < current[1]; ++idx) {
                    dist = LocationUtils.distance(locations.get(idx), locations.get(current[0]),
                            locations.get(current[1]));
                    if (dist > maxDist) {
                        maxDist = dist;
                        maxIdx = idx;
                    }
                }
                if (maxDist > tolerance) {
                    dists[maxIdx] = maxDist;
                    int[] stackValCurMax = {current[0], maxIdx};
                    stack.push(stackValCurMax);
                    int[] stackValMaxCur = {maxIdx, current[1]};
                    stack.push(stackValMaxCur);
                }
            }
        }

        int i = 0;
        idx = 0;
        decimated.clear();
        for (GeoPos l : locations) {
            if (dists[idx] != 0) {
                decimated.add((T) l);
                i++;
            }
            idx++;
        }
        Hint.log("LocationUtils", "Decimating " + n + " points to " + i + " w/ tolerance = " + tolerance);
    }

    /**
     * Checks if a given location is a valid (i.e. physically possible) location
     * on Earth. Note: The special separator locations (which have latitude =
     * 100) will not qualify as valid. Neither will locations with lat=0 and
     * lng=0 as these are most likely "bad" measurements which often cause
     * trouble.
     *
     * @param location the location to test
     * @return true if the location is a valid location.
     */
    public static boolean isValidLocation(Location location) {
        return location != null && Math.abs(location.getLatitude()) <= 90 && Math.abs(location.getLongitude()) <= 180;
    }

    public static String asString(final Address address) {
        final StringBuffer sBuffer = new StringBuffer();
        final int top = address.getMaxAddressLineIndex() + 1;
        for (int i = 0; i < top; i++) {
            sBuffer.append(address.getAddressLine(i));
            if (i != top - 1) {
                sBuffer.append(", ");
            }
        }
        return sBuffer.toString();
    }

    private static Location getLastCachedLocation() {
        float lat = GlobalSettings.getInstance().getFloat("lastKnownLat", Float.MAX_VALUE);
        float lon = GlobalSettings.getInstance().getFloat("lastKnownLon", Float.MAX_VALUE);
        if ((lat != Float.MAX_VALUE) && (lon != Float.MAX_VALUE)) {
            lastKnownLocation = new Location("cache");
            lastKnownLocation.setLatitude(lat);
            lastKnownLocation.setLongitude(lon);
            return lastKnownLocation;
        }
        return null;
    }

    public static Location getLastKnownLocation() {
        if (lastKnownLocation == null)
            lastKnownLocation = getLastCachedLocation();
        if (lastKnownLocation == null)
            lastKnownLocation = getLastKnownLocation(GlobalSettings.getInstance().getContext(), false);
        return lastKnownLocation;
    }

    public static void setLastKnownLocation(Location location) {
        if (location == null)
            return;

        lastKnownLocation = location;
        GlobalSettings.getInstance().put("lastKnownLat", (float) location.getLatitude());
        GlobalSettings.getInstance().put("lastKnownLon", (float) location.getLongitude());
    }

    public static Location getLastKnownLocation(Context context, boolean enabledProvidersOnly) {
        float accuracy = 9999.9f;
        Location result = null;
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        List<String> providers = manager.getProviders(enabledProvidersOnly);

        for (String provider : providers) {
            location = manager.getLastKnownLocation(provider);
            if (location != null) {

                Hint.log("LocationUtils", "provider: " + provider + ", accuracy: " + location.getAccuracy()
                        + ", time: " + location.getTime());
                Date now = new Date();
                if (location.hasAccuracy() && (location.getAccuracy() < accuracy)
                        && (now.getTime() - location.getTime() < 1000 * 60 * 30)) {
                    accuracy = location.getAccuracy();
                    result = location;
                }
                if (result == null)
                    result = location;
            }
        }

        if (result == null)
            result = getLastCachedLocation();

        return result;
    }

    public static Address getAddressFromLocation(Context context, final Location location) throws IOException {
        if (!Helper.isOnline(context))
            return null;

        Geocoder geocoder = new Geocoder(context);
        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        if (addresses == null || addresses.size() == 0) {
            return null;
        }
        return addresses.get(0);
    }

    public static Address getAddressFromName(Context context, final String name) throws IOException {
        Geocoder geocoder = new Geocoder(context);
        List<Address> addresses = geocoder.getFromLocationName(name, 1);
        if (addresses == null || addresses.size() == 0) {
            return null;
        }
        return addresses.get(0);
    }

    /**
     * Decode a mapquest polyline string into a list of PGeoPoints.
     *
     * @param poly      polyline encoded string to decode.
     * @param precision the level of precision the polyline was encoded to
     * @return the list of PGeoPoints represented by this polystring.
     */

    public static Vector<GeoPos> decodePolyLine(final String encoded, double precision) {
        Hint.log("LocationUtils", "Decode polyLine of size: " + encoded.length());
        precision = Math.pow(10, -precision);
        int len = encoded.length();
        int index = 0;
        double lat = 0;
        double lng = 0;
        Vector<GeoPos> array = new Vector<GeoPos>();
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) > 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) > 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            array.add(new GeoPos(lat * precision, lng * precision));
        }
        Hint.log("LocationUtils", "Decode polyLine done");
        return array;
    }

    public static String compress(Vector<? extends GeoPos> points, double precision) {
        long oldLat = 0;
        long oldLng = 0;
        StringBuilder encoded = new StringBuilder();
        precision = Math.pow(10, precision);
        for (GeoPos pos : points) {
            // Round to N decimal places
            long lat = Math.round(pos.latitude * precision);
            long lng = Math.round(pos.longitude * precision);

            // Encode the differences between the points
            encoded.append(encodeNumber(lat - oldLat));
            encoded.append(encodeNumber(lng - oldLng));

            oldLat = lat;
            oldLng = lng;
        }
        return encoded.toString();
    }

    public static String encodeNumber(long num) {
        num = num << 1;
        if (num < 0) {
            num = ~(num);
        }
        StringBuilder encoded = new StringBuilder();
        while (num >= 0x20) {
            encoded.append((char) ((0x20 | (num & 0x1f)) + 63));
            num >>= 5;
        }
        encoded.append((char) (num + 63));
        return encoded.toString();
    }

    public static JSONObject geoPosToJson(GeoPos geoPos) {
        JSONObject json = new JSONObject();
        if (geoPos == null)
            return json;
        try {
            json.put("lat", geoPos.latitude);
            json.put("lon", geoPos.longitude);
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

    public static GeoPos geoPosFromJson(JSONObject json) {
        try {
            return new GeoPos(json.getDouble("lat"), json.getDouble("lon"));
        } catch (JSONException e) {
            return null;
        }
    }
}
