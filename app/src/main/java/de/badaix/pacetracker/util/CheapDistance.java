package de.badaix.pacetracker.util;

import de.badaix.pacetracker.session.GeoPos;

//http://calgary.rasc.ca/latlong.htm
//
//The Formula for Longitude Distance at a Given Latitude (theta) in Km:
//	1� of Longitude = 111.41288 * cos(theta) - 0.09350 * cos(3 * theta) + 0.00012 * cos(5 * theta)
//
//The Formula for Latitude Distance at a Given Latitude (theta) in Km:
//	1� of Latitude = 111.13295 - 0.55982 * cos(2 * theta) + 0.00117 * cos(4 * theta)

public class CheapDistance {
    double oneLatitude = -1.;
    double oneLongitude = -1.;

    public CheapDistance(GeoPos pos) {
        init(pos);
    }

    public double getOneLatitude() {
        return oneLatitude;
    }

    public double getOneLongitude() {
        return oneLongitude;
    }

    public double longitudeOffset(double meters) {
        return meters / oneLongitude;
    }

    public double latitudeOffset(double meters) {
        return meters / oneLatitude;
    }

    public void init(GeoPos pos) {
        double lat = pos.latitude / Distance.DEGREES_TO_RADIANS;
        double lon = pos.longitude / Distance.DEGREES_TO_RADIANS;
        oneLongitude = 111412.88 * Math.cos(lat) - 0.09350 * Math.cos(3 * lat) + 0.00012 * Math.cos(5 * lat);
        oneLatitude = 111132.95 - 0.55982 * Math.cos(2 * lon) + 0.00117 * Math.cos(4 * lon);
    }

    public double distance(GeoPos posA, GeoPos posB) {
        double latDist = (posA.latitude - posB.latitude) * oneLatitude;
        double lonDist = (posA.longitude - posB.longitude) * oneLongitude;
        return Math.sqrt(latDist * latDist + lonDist * lonDist);
    }
}
