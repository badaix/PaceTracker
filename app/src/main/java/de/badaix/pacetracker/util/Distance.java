package de.badaix.pacetracker.util;

import android.content.Context;
import android.location.Address;
import android.location.Location;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.settings.GlobalSettings;

/**
 * Class to calculate the distance between two points in arbitrary units
 */
public class Distance {
    public final static double mile = 1609.344;
    public final static double kilometer = 1000.0;
    public final static double nmi = 1852.014;
    public final static double yard = 0.9144;
    public final static double meter = 1.0;
    /**
     * Names for the units to use
     */
    public final static int KILOMETERS = 0;
    public final static int STATUTE_MILES = 1;
    public final static int NAUTICAL_MILES = 2;
    public final static int METERS = 3;
    /**
     * Conversion factor to convert from degrees to radians
     */
    public static final double DEGREES_TO_RADIANS = (double) (180.0 / Math.PI);
    /**
     * Radius of the Earth in the units above
     */
    private final static double EARTHS_RADIUS[] = {
            // 6356.7523142,
            6378.1, // Kilometers
            3963.1676, // Statue miles
            3443.89849, // Nautical miles
            6378100 // meters
    };
    private static java.text.DecimalFormat decFormat3 = null;
    private static java.text.DecimalFormat decFormatNeutral = null;
    private static java.text.DecimalFormat decFormat1 = null;

    /**
     * Calculates the "length" of an arc between two points on a sphere given
     * the latitude & longitude of those points.
     *
     * @param aLat  Latitude of point A
     * @param aLong Longitude of point A
     * @param bLat  Latitude of point B
     * @param bLong Longitude of point B
     * @return
     */
    private static double calclateArc(double aLat, double aLong, double bLat, double bLong) {
        /*
		 * Convert location a and b's lattitude and longitude from degrees to
		 * radians
		 */
        double aLatRad = aLat / DEGREES_TO_RADIANS;
        double aLongRad = aLong / DEGREES_TO_RADIANS;
        double bLatRad = bLat / DEGREES_TO_RADIANS;
        double bLongRad = bLong / DEGREES_TO_RADIANS;

        double cosBLatRadxcosBLatRad = Math.cos(bLatRad) * Math.cos(aLatRad);

        // Calculate the length of the arc that subtends point a and b
        double t1 = cosBLatRadxcosBLatRad * Math.cos(aLongRad) * Math.cos(bLongRad);
        double t2 = cosBLatRadxcosBLatRad * Math.sin(aLongRad) * Math.sin(bLongRad);
        double t3 = Math.sin(aLatRad) * Math.sin(bLatRad);
        double tt = Math.acos(t1 + t2 + t3);

        // Return a "naked" length for the calculated arc
        return tt;
    }

    /**
     * Calculates the distance between two addresses
     *
     * @param pointA Address of point A
     * @param pointB Address of point B
     * @param units  Desired units
     * @return Distance between the two points in the desired units
     */
    public static double calculateDistance(Address pointA, Address pointB, int units) {
        return calculateDistance(pointA.getLatitude(), pointA.getLongitude(), pointB.getLatitude(),
                pointB.getLongitude(), units);
    }

    /**
     * Calculates the distance between two locations
     *
     * @param pointA Location of point A
     * @param pointB Location of point B
     * @param units  Desired units
     * @return Distance between the two points in the desired units
     */
    public static double calculateDistance(Location pointA, Location pointB, int units) {
        return calculateDistance(pointA.getLatitude(), pointA.getLongitude(), pointB.getLatitude(),
                pointB.getLongitude(), units);
    }

    public static double calculateDistance(GeoPos posA, GeoPos posB) {
        return calculateDistance(posA.latitude, posA.longitude, posB.latitude, posB.longitude, Distance.METERS);
    }

    public static double calculateDistance(double latA, double lonA, double latB, double lonB, int units) {
        double result = calclateArc(latA, lonA, latB, lonB) * EARTHS_RADIUS[units];
        if (Double.isNaN(result))
            return 0.;

        return result;
    }

    public static double distanceToDouble(double distanceMeters, Unit unit) {
        return distanceMeters / unit.getFactor();
    }

    public static double distanceToDouble(double distanceMeters) {
        return distanceToDouble(distanceMeters, GlobalSettings.getInstance().getDistUnit());
    }

	/*
	 * public static double calculateDistance(double lat_a, double lng_a, double
	 * lat_b, double lng_b) { float a1 = (float)(lat_a / DEGREES_TO_RADIANS);
	 * float a2 = (float)(lng_a / DEGREES_TO_RADIANS); float b1 = (float)(lat_b
	 * / DEGREES_TO_RADIANS); float b2 = (float)(lng_b / DEGREES_TO_RADIANS);
	 * 
	 * float t1 =
	 * FloatMath.cos(a1)*FloatMath.cos(a2)*FloatMath.cos(b1)*FloatMath.cos(b2);
	 * float t2 =
	 * FloatMath.cos(a1)*FloatMath.sin(a2)*FloatMath.cos(b1)*FloatMath.sin(b2);
	 * float t3 = FloatMath.sin(a1)*FloatMath.sin(b1); double tt = Math.acos(t1
	 * + t2 + t3);
	 * 
	 * return 6366000*tt; }
	 */

    public static String doubleToString(double value, int precision) {
        if (precision == 0)
            return Integer.toString((int) Math.round(value));
        if (decFormat3 == null)
            decFormat3 = new java.text.DecimalFormat("#0.000");
        String s = decFormat3.format(value);
        return s.substring(0, s.length() - (3 - precision));
    }

    public static String doubleToStringNeutral(double value, int precision) {
        if (decFormatNeutral == null) {
            DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
            otherSymbols.setDecimalSeparator('.');
            otherSymbols.setGroupingSeparator(',');
            decFormatNeutral = new java.text.DecimalFormat("#0.000", otherSymbols);
        }

        String s = decFormatNeutral.format(value);
        return s.substring(0, s.length() - (3 - precision));
    }

    public static String distanceToString(double distanceMeters, Unit unit, int precision) {
        return doubleToString(distanceToDouble(distanceMeters, unit), precision);
    }

    public static String distanceToString(double distanceMeters, int precision) {
        return doubleToString(distanceToDouble(distanceMeters), precision);
    }

    public static String GpsDegToString(double deg) {
        String result = Location.convert(deg, Location.FORMAT_MINUTES);
        int pos = result.lastIndexOf(".");
        if (pos == -1)
            pos = result.lastIndexOf(",");
        if ((pos != -1) && (result.length() - pos > 3))
            result = result.substring(0, pos + 4);
        return result;
    }

    public static String speedToString(double speedKmh) {
        if (decFormat1 == null)
            decFormat1 = new java.text.DecimalFormat("#0.0");
        String s = decFormat1.format(speedKmh / (GlobalSettings.getInstance().getDistUnit().getFactor() / 1000.));
        return s;
    }

    public static double bearing(GeoPos p1, GeoPos p2) {
        // Convert input values to radians
        double aLatRad = p1.latitude / DEGREES_TO_RADIANS;
        double aLongRad = p1.longitude / DEGREES_TO_RADIANS;
        double bLatRad = p2.latitude / DEGREES_TO_RADIANS;
        double bLongRad = p2.longitude / DEGREES_TO_RADIANS;

        double deltaLong = bLongRad - aLongRad;

        double y = Math.sin(deltaLong) * Math.cos(bLatRad);
        double x = Math.cos(aLatRad) * Math.sin(bLatRad) - Math.sin(aLatRad) * Math.cos(bLatRad) * Math.cos(deltaLong);
        return Math.atan2(y, x) * DEGREES_TO_RADIANS;
    }

    public enum System {
        METRIC, IMPERIAL;

        public static System fromString(String system) {
            if (system.equalsIgnoreCase(IMPERIAL.toString()))
                return IMPERIAL;
            return METRIC;
        }
    }

    public enum Unit {
        KILOMETERS, MILES, NAUTIC_MILES, YARDS, METER;

        public String toLocaleString(Context context, boolean singular) {
            if (this == KILOMETERS) {
                if (singular)
                    return context.getResources().getString(R.string.kilometer);
                return context.getResources().getString(R.string.kilometers);
            } else if (this == MILES) {
                if (singular)
                    return context.getResources().getString(R.string.mile);
                return context.getResources().getString(R.string.miles);
            } else if (this == NAUTIC_MILES) {
                if (singular)
                    return context.getResources().getString(R.string.nauticMile);
                return context.getResources().getString(R.string.nauticMiles);
            } else if (this == YARDS) {
                if (singular)
                    return context.getResources().getString(R.string.yard);
                return context.getResources().getString(R.string.yards);
            } else if (this == METER) {
                if (singular)
                    return context.getResources().getString(R.string.meter);
                return context.getResources().getString(R.string.meters);
            } else
                return "";
        }

        @Override
        public String toString() {
            if (this == KILOMETERS)
                return "kilometers";
            else if (this == MILES)
                return "miles";
            else if (this == NAUTIC_MILES)
                return "nmi";
            else if (this == YARDS)
                return "yards";
            else if (this == METER)
                return "meters";
            else
                return "";
        }

        public String toShortString() {
            if (this == KILOMETERS)
                return "km";
            else if (this == MILES)
                return "mi";
            else if (this == NAUTIC_MILES)
                return "nmi";
            else if (this == YARDS)
                return "yd";
            else if (this == METER)
                return "m";
            else
                return "";
        }

        public String perHourString() {
            if (this == KILOMETERS)
                return "km/h";
            else if (this == MILES)
                return "mph";
            else if (this == NAUTIC_MILES)
                return "nmi/h";
            else if (this == YARDS)
                return "yd/h";
            else if (this == METER)
                return "m/h";
            else
                return "";
        }

        public System getSystem() {
            if (this == KILOMETERS)
                return System.METRIC;
            else if (this == MILES)
                return System.IMPERIAL;
            else if (this == NAUTIC_MILES)
                return System.IMPERIAL;
            else if (this == YARDS)
                return System.IMPERIAL;
            else if (this == METER)
                return System.METRIC;
            else
                return System.METRIC;
        }

        public double getFactor() {
            if (this == KILOMETERS)
                return kilometer;
            else if (this == MILES)
                return mile;
            else if (this == NAUTIC_MILES)
                return nmi;
            else if (this == YARDS)
                return yard;
            else if (this == METER)
                return meter;
            else
                return kilometer;
        }
    }
}
