package de.badaix.pacetracker.session;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

public class Exporter {

    private static Vector<GpsPos> decimate(Vector<GpsPos> pos, float tolerance) {
        if (tolerance <= 0.f)
            return pos;

        Vector<GpsPos> vGpsPos = new Vector<GpsPos>();
        LocationUtils.decimate(tolerance, pos, vGpsPos);
        return vGpsPos;
    }

    public static void toGpx(Session session, Writer writer, float tolerance) {
        Vector<GpsPos> vPos = decimate(session.getGpsPos(), tolerance);
        try {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"PaceTracker\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");
            writer.write("\t<trk>\n");
            writer.write("\t\t<name>\n");
            writer.write("\t\t\t<![CDATA[" + session.getSettings().getDescription() + "]]>\n");
            writer.write("\t\t</name>\n");
            writer.write("\t\t<type>\n");
            writer.write("\t\t\t<![CDATA[" + session.getType().replaceAll("Session", "") + "]]>\n");
            writer.write("\t\t</type>\n");
            writer.write("\t\t<trkseg>\n");
            Date ts = new Date();
            for (GpsPos pos : vPos) {
                writer.write("\t\t\t<trkpt lon=\"" + Double.toString(pos.longitude) + "\" lat=\""
                        + Double.toString(pos.latitude) + "\">\n");
                writer.write("\t\t\t\t<ele>" + Double.toString(pos.altitude) + "</ele>\n");
                ts.setTime(pos.time);
                writer.write("\t\t\t\t<time>" + DateUtils.toISOString(ts) + "</time>\n");
                writer.write("\t\t\t</trkpt>\n");
            }
            writer.write("\t\t</trkseg>\n");
            writer.write("\t</trk>\n");
            writer.write("</gpx>");
            writer.flush();
        } catch (IOException e) {
        }
    }

    public static void toCsv(Session session, Writer writer) {
        SessionReader reader = new SessionReader();
        try {
            reader.openSession(session.getFilename());
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Hint.log("Exporter", e.getMessage());
            return;
        }

        try {
            SessionElement element;
            SessionHeader header = new SessionHeader();
            while (reader.getNextHeader(header, false)) {
                element = reader.getNextElement(header);
                if (element == null)
                    break;
                writer.write(element.toString() + "\n");
            }
            writer.flush();
        } catch (IOException e) {
            Hint.log("Exporter", e.getMessage());
        }
    }

    public static void toKmlTour(Context context, Session session, Writer writer, float tolerance) {
        Vector<GpsPos> vPos = decimate(session.getGpsPos(), tolerance);
        for (int i = 0; i < vPos.size() - 1; ++i) {
            vPos.get(i).bearing = (float) de.badaix.pacetracker.util.Distance.bearing(vPos.get(i), vPos.get(i + 1));
        }
        // java.text.DecimalFormat decFormat;
        // decFormat = new java.text.DecimalFormat("#0.0");
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'", Locale.getDefault());
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
            writer.write("	<Document>\n");
            writer.write("		<open>1</open>\n");
            writer.write("		<visibility>1</visibility>\n");
            writer.write("		<name><![CDATA[" + session.getName(context) + "]]></name>\n");
            writer.write("		<atom:author><atom:name><![CDATA[Created with PaceTracker]]></atom:name></atom:author>\n");
            writer.write("		<Style id=\"track\">\n");
            writer.write("		<LineStyle><color>7f0000ff</color><width>4</width></LineStyle>\n");
            writer.write("		<IconStyle>\n");
            writer.write("			<scale>1.3</scale>\n");
            writer.write("			<Icon><href>http://earth.google.com/images/kml-icons/track-directional/track-0.png</href></Icon>\n");
            writer.write("		</IconStyle>\n");
            writer.write("		</Style>\n");
            writer.write("		<Style id=\"start\">\n");
            writer.write("			<IconStyle>\n");
            writer.write("				<scale>1.3</scale>\n");
            writer.write("				<Icon><href>http://maps.google.com/mapfiles/kml/paddle/grn-circle.png</href></Icon>\n");
            writer.write("				<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("			</IconStyle>\n");
            writer.write("		</Style>\n");
            writer.write("		<Style id=\"end\">\n");
            writer.write("			<IconStyle>\n");
            writer.write("				<scale>1.3</scale>\n");
            writer.write("				<Icon><href>http://maps.google.com/mapfiles/kml/paddle/red-circle.png</href></Icon>\n");
            writer.write("				<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("			</IconStyle>\n");
            writer.write("		</Style>\n");
            writer.write("		<Style id=\"statistics\">\n");
            writer.write("			<IconStyle>\n");
            writer.write("				<scale>1.3</scale>\n");
            writer.write("				<Icon><href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href></Icon>\n");
            writer.write("				<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("			</IconStyle>\n");
            writer.write("		</Style>\n");
            writer.write("		<Style id=\"waypoint\">\n");
            writer.write("			<IconStyle>\n");
            writer.write("				<scale>1.3</scale>\n");
            writer.write("				<Icon><href>http://maps.google.com/mapfiles/kml/pushpin/blue-pushpin.png</href></Icon>\n");
            writer.write("				<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("			</IconStyle>\n");
            writer.write("		</Style>\n");
            // writer.write("		<Schema id=\"schema\">\n");
            // writer.write("			<gx:SimpleArrayField name=\"power\" type=\"int\">\n");
            // writer.write("				<displayName><![CDATA[Leistung (W)]]></displayName>\n");
            // writer.write("			</gx:SimpleArrayField>\n");
            // writer.write("			<gx:SimpleArrayField name=\"cadence\" type=\"int\">\n");
            // writer.write("				<displayName><![CDATA[Trittfrequenz (Umdrehungen pro Minute)]]></displayName>\n");
            // writer.write("			</gx:SimpleArrayField>\n");
            // writer.write("			<gx:SimpleArrayField name=\"heart_rate\" type=\"int\">\n");
            // writer.write("				<displayName><![CDATA[Herzfrequenz (Schläge pro Minute)]]></displayName>\n");
            // writer.write("			</gx:SimpleArrayField>\n");
            // writer.write("		</Schema>\n");
            writer.write("		<Placemark>\n");
            writer.write("			<name><![CDATA[" + context.getString(R.string.start) + "]]></name>\n");
            writer.write("			<description><![CDATA[]]></description>\n");
            writer.write("			<TimeStamp>\n");
            String ts = df.format(new Date(vPos.firstElement().time));
            writer.write("				<when>" + ts + "</when>\n");
            writer.write("			</TimeStamp>\n");
            writer.write("			<styleUrl>#start</styleUrl>\n");
            writer.write("			<Point>\n");
            String coord = Double.toString(vPos.firstElement().longitude) + " "
                    + Double.toString(vPos.firstElement().latitude) + " "
                    + Double.toString(vPos.firstElement().altitude);
            writer.write("				<coordinates>" + coord + "</coordinates>\n");
            writer.write("			</Point>\n");
            writer.write("		</Placemark>\n");
            writer.write("		<Placemark id=\"tour\">\n");
            // writer.write("			<name><![CDATA[Süsterfeldstraße 3]]></name>\n");
            writer.write("			<name><![CDATA[]]></name>\n");
            writer.write("			<description><![CDATA[]]></description>\n");
            writer.write("			<styleUrl>#track</styleUrl>\n");
            writer.write("			<gx:MultiTrack>\n");
            writer.write("				<altitudeMode>absolute</altitudeMode>\n");
            writer.write("				<gx:interpolate>1</gx:interpolate>\n");
            writer.write("				<gx:Track>\n");
            // long lastDuration = vPos.firstElement().duration;
            for (GpsPos pos : vPos) {
                // long duration = pos.duration - lastDuration;
                // if (duration < 5000)
                // continue;

                // lastDuration = pos.duration;
                ts = df.format(new Date(pos.time));
                writer.write("					<when>" + ts + "</when>\n");
                coord = Double.toString(pos.longitude) + " " + Double.toString(pos.latitude) + " "
                        + Double.toString(pos.altitude);
                writer.write("					<gx:coord>" + coord + "</gx:coord>\n");
            }
            writer.write("					<ExtendedData>\n");
            writer.write("						<SchemaData schemaUrl=\"#schema\"></SchemaData>\n");
            writer.write("					</ExtendedData>\n");
            writer.write("				</gx:Track>\n");
            writer.write("			</gx:MultiTrack>\n");
            writer.write("		</Placemark>\n");
            writer.write("		<Placemark>\n");
            writer.write("			<name><![CDATA[" + context.getString(R.string.end) + "]]></name>\n");
            writer.write("			<description>\n");
            writer.write("		<![CDATA[Created with PaceTracker\n");
            writer.write("\n");
            // writer.write("		Name: Süsterfeldstraße 5\n");
            // writer.write("		Art der Aktivität: -\n");
            // writer.write("		Beschreibung: -\n");
            // writer.write("		Gesamtstrecke: 1,30 km (0,8 Meile/n)\n");
            // writer.write("		Gesamtzeit: 05:09\n");
            // writer.write("		Zeit in Bewegung: 03:56\n");
            // writer.write("		Durchschnittliche Geschwindigkeit: 15,17 km/h (9,4 Meile/h)\n");
            // writer.write("		Durchschnittliche Geschwindigkeit in Bewegung: 19,81 km/h (12,3 Meile/h)\n");
            // writer.write("		Maximale Geschwindigkeit: 30,60 km/h (19,0 Meile/h)\n");
            // writer.write("		Durchschnittliches Tempo: 3,95 min/km (6,4 min/Meile)\n");
            // writer.write("		Durchschnittliches Tempo in Bewegung: 3,03 min/km (4,9 min/Meile)\n");
            // writer.write("		Schnellstes Tempo: 1,96 min/km (3,2 min/Meile)\n");
            // writer.write("		Maximale Höhe: 228 m (749 Fuß)\n");
            // writer.write("		Minimale Höhe: 202 m (664 Fuß)\n");
            // writer.write("		Höhenunterschied: 9 m (29 Fuß)\n");
            // writer.write("		Maximales Gefälle: 10 %\n");
            // writer.write("		Minimales Gefälle: -7 %\n");
            // writer.write("		Aufgezeichnet: 03.06.2013 11:05\n");
            writer.write("		]]>\n");
            writer.write("			</description>\n");
            writer.write("			<TimeStamp>\n");
            ts = df.format(new Date(vPos.lastElement().time));
            writer.write("				<when>" + ts + "</when>\n");
            writer.write("			</TimeStamp>\n");
            writer.write("			<styleUrl>#end</styleUrl>\n");
            writer.write("			<Point>\n");
            coord = Double.toString(vPos.lastElement().longitude) + " " + Double.toString(vPos.lastElement().latitude)
                    + " " + Double.toString(vPos.lastElement().altitude);
            writer.write("				<coordinates>" + coord + "</coordinates>\n");
            writer.write("			</Point>\n");
            writer.write("		</Placemark>\n");
            writer.write("	</Document>\n");
            writer.write("</kml>\n");
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static void toKml(Session session, Writer writer) {
        java.text.DecimalFormat decFormat;
        decFormat = new java.text.DecimalFormat("#0.0");
        try {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
            writer.write("\t<Document>\n");
            writer.write("\t\t<name>20110731T185959</name>\n");
            writer.write("\t\t<description></description>\n");
            writer.write("\t\t<Style id=\"sn_ylw-pushpin4\">\n");
            writer.write("\t\t\t<IconStyle>\n");
            writer.write("\t\t\t\t<scale>1.1</scale>\n");
            writer.write("\t\t\t\t<Icon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>\n");
            writer.write("\t\t\t\t</Icon>\n");
            writer.write("\t\t\t\t\t<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("\t\t\t</IconStyle>\n");
            writer.write("\t\t\t<LineStyle>\n");
            writer.write("\t\t\t\t<color>ffc68b00</color>\n");
            writer.write("\t\t\t\t<width>6</width>\n");
            writer.write("\t\t\t</LineStyle>\n");
            writer.write("\t\t</Style>\n");
            writer.write("\t\t<Style id=\"sh_ylw-pushpin\">\n");
            writer.write("\t\t\t<IconStyle>\n");
            writer.write("\t\t\t\t<scale>1.3</scale>\n");
            writer.write("\t\t\t\t<Icon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>\n");
            writer.write("\t\t\t\t</Icon>\n");
            writer.write("\t\t\t\t<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("\t\t\t</IconStyle>\n");
            writer.write("\t\t\t<LineStyle>\n");
            writer.write("\t\t\t\t<color>ffc68b00</color>\n");
            writer.write("\t\t\t\t<width>6</width>\n");
            writer.write("\t\t\t</LineStyle>\n");
            writer.write("\t\t</Style>\n");
            writer.write("\t\t<Style id=\"sn_S\">\n");
            writer.write("\t\t\t<IconStyle>\n");
            writer.write("\t\t\t\t<scale>1.1</scale>\n");
            writer.write("\t\t\t\t<Icon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/S.png</href>\n");
            writer.write("\t\t\t\t</Icon>\n");
            writer.write("\t\t\t\t<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("\t\t\t</IconStyle>\n");
            writer.write("\t\t\t<ListStyle>\n");
            writer.write("\t\t\t\t<ItemIcon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/S-lv.png</href>\n");
            writer.write("\t\t\t\t</ItemIcon>\n");
            writer.write("\t\t\t</ListStyle>\n");
            writer.write("\t\t</Style>\n");
            writer.write("\t\t<Style id=\"sh_S\">\n");
            writer.write("\t\t\t<IconStyle>\n");
            writer.write("\t\t\t\t<scale>1.3</scale>\n");
            writer.write("\t\t\t\t<Icon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/S.png</href>\n");
            writer.write("\t\t\t\t</Icon>\n");
            writer.write("\t\t\t\t<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("\t\t\t</IconStyle>\n");
            writer.write("\t\t\t<ListStyle>\n");
            writer.write("\t\t\t\t<ItemIcon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/S-lv.png</href>\n");
            writer.write("\t\t\t\t</ItemIcon>\n");
            writer.write("\t\t\t</ListStyle>\n");
            writer.write("\t\t</Style>\n");
            writer.write("\t\t<Style id=\"sn_Z\">\n");
            writer.write("\t\t\t<IconStyle>\n");
            writer.write("\t\t\t\t<scale>1.1</scale>\n");
            writer.write("\t\t\t\t<Icon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/Z.png</href>\n");
            writer.write("\t\t\t\t</Icon>\n");
            writer.write("\t\t\t\t<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("\t\t\t</IconStyle>\n");
            writer.write("\t\t\t<ListStyle>\n");
            writer.write("\t\t\t\t<ItemIcon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/Z-lv.png</href>\n");
            writer.write("\t\t\t\t</ItemIcon>\n");
            writer.write("\t\t\t</ListStyle>\n");
            writer.write("\t\t</Style>\n");
            writer.write("\t\t<Style id=\"sh_Z\">\n");
            writer.write("\t\t\t<IconStyle>\n");
            writer.write("\t\t\t\t<scale>1.3</scale>\n");
            writer.write("\t\t\t\t<Icon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/Z.png</href>\n");
            writer.write("\t\t\t\t</Icon>\n");
            writer.write("\t\t\t\t<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            writer.write("\t\t\t</IconStyle>\n");
            writer.write("\t\t\t<ListStyle>\n");
            writer.write("\t\t\t\t<ItemIcon>\n");
            writer.write("\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/Z-lv.png</href>\n");
            writer.write("\t\t\t\t</ItemIcon>\n");
            writer.write("\t\t\t</ListStyle>\n");
            writer.write("\t\t</Style>\n");
            writer.write("\t\t<StyleMap id=\"msn_S\">\n");
            writer.write("\t\t\t<Pair>\n");
            writer.write("\t\t\t\t<key>normal</key>\n");
            writer.write("\t\t\t\t<styleUrl>#sn_S</styleUrl>\n");
            writer.write("\t\t\t</Pair>\n");
            writer.write("\t\t\t<Pair>\n");
            writer.write("\t\t\t\t<key>highlight</key>\n");
            writer.write("\t\t\t\t<styleUrl>#sh_S</styleUrl>\n");
            writer.write("\t\t\t</Pair>\n");
            writer.write("\t\t</StyleMap>\n");
            writer.write("\t\t<StyleMap id=\"msn_Z\">\n");
            writer.write("\t\t\t<Pair>\n");
            writer.write("\t\t\t\t<key>normal</key>\n");
            writer.write("\t\t\t\t<styleUrl>#sn_Z</styleUrl>\n");
            writer.write("\t\t\t</Pair>\n");
            writer.write("\t\t\t<Pair>\n");
            writer.write("\t\t\t\t<key>highlight</key>\n");
            writer.write("\t\t\t\t<styleUrl>#sh_Z</styleUrl>\n");
            writer.write("\t\t\t</Pair>\n");
            writer.write("\t\t</StyleMap>\n");
            writer.write("\t\t<StyleMap id=\"msn_ylw-pushpin\">\n");
            writer.write("\t\t\t<Pair>\n");
            writer.write("\t\t\t\t<key>normal</key>\n");
            writer.write("\t\t\t\t<styleUrl>#sn_ylw-pushpin4</styleUrl>\n");
            writer.write("\t\t\t</Pair>\n");
            writer.write("\t\t\t<Pair>\n");
            writer.write("\t\t\t\t<key>highlight</key>\n");
            writer.write("\t\t\t\t<styleUrl>#sh_ylw-pushpin</styleUrl>\n");
            writer.write("\t\t\t</Pair>\n");
            writer.write("\t\t</StyleMap>\n");
            writer.write("\t\t<Style id=\"yellowLineGreenPoly\">\n");
            writer.write("\t\t\t<LineStyle>\n");
            writer.write("\t\t\t\t<color>7f00ffff</color>\n");
            writer.write("\t\t\t\t<width>4</width>\n");
            writer.write("\t\t\t</LineStyle>\n");
            writer.write("\t\t\t<PolyStyle>\n");
            writer.write("\t\t\t\t<color>7f00ff00</color>\n");
            writer.write("\t\t\t</PolyStyle>\n");
            writer.write("\t\t</Style>\n");
            writer.write("\t\t<Folder>\n");
            writer.write("\t\t\t<name>kilometer</name>\n");
            writer.write("\t\t\t<open>1</open>\n");
            int iKilometer = 0;
            int idx = 0;
            int iLastKilometerIdx = 0;
            int iLastKilometerDistance = 0;
            Vector<GpsPos> vGpsPos = session.getGpsPos();
            for (GpsPos pos : vGpsPos) {
                int iDistance = (int) pos.distance;
                if ((iDistance / 1000) > iKilometer) {
                    iKilometer++;
                    writer.write("\t\t\t<Placemark>\n");
                    writer.write("\t\t\t\t<name>Kilometer " + iKilometer + "</name>\n");
                    writer.write("\t\t\t\t<description>");
                    long lDuration = session.getGpsPos().get(idx).duration
                            - session.getGpsPos().get(iLastKilometerIdx).duration;
                    writer.write("<![CDATA[");
                    writer.write("Pace: ");
                    writer.write(DateUtils.secondsToMMSSString(lDuration / (iDistance - iLastKilometerDistance)));
                    writer.write("<br />");
                    writer.write("Speed: ");
                    writer.write(decFormat.format(3600. / (double) (lDuration / (iDistance - iLastKilometerDistance))));
                    writer.write("]]>\n");
                    writer.write("\t\t\t\t</description>\n");
                    writer.write("\t\t\t\t<styleUrl>#msn_ylw-pushpin</styleUrl>\n");
                    writer.write("\t\t\t\t<Point>\n");
                    writer.write("\t\t\t\t\t<coordinates>");
                    writer.write(Double.toString(pos.longitude));
                    writer.write(",");
                    writer.write(Double.toString(pos.latitude));
                    writer.write(",0</coordinates>\n");
                    writer.write("\t\t\t\t</Point>\n");
                    writer.write("\t\t\t</Placemark>\n");
                    iLastKilometerIdx = idx;
                    iLastKilometerDistance = iDistance;
                }
                idx++;
            }
            writer.write("\t\t</Folder>\n");
            writer.write("\t\t<Folder>\n");
            writer.write("\t\t\t<name>events</name>\n");
            writer.write("\t\t\t<open>1</open>\n");
            writer.write("\t\t\t<Placemark>\n");
            writer.write("\t\t\t\t<name>Start</name>\n");
            writer.write("\t\t\t\t<styleUrl>#msn_S</styleUrl>\n");
            writer.write("\t\t\t\t<Point>\n");
            writer.write("\t\t\t\t\t<coordinates>" + Double.toString(session.getGpsPos().firstElement().longitude)
                    + "," + Double.toString(session.getGpsPos().firstElement().latitude) + ",0</coordinates>\n");
            writer.write("\t\t\t\t</Point>\n");
            writer.write("\t\t\t</Placemark>\n");
            writer.write("\t\t\t<Placemark>\n");
            writer.write("\t\t\t\t<name>Ziel</name>\n");
            writer.write("\t\t\t\t<description>");
            long lDuration = session.getGpsPos().lastElement().duration
                    - session.getGpsPos().get(iLastKilometerIdx).duration;
            int iDistance = (int) session.getGpsPos().lastElement().distance - iLastKilometerDistance;
            writer.write("<![CDATA[");
            writer.write("Pace: " + DateUtils.secondsToMMSSString(lDuration / iDistance) + "<br />");
            writer.write("Speed: " + decFormat.format(3600. / (double) (lDuration / iDistance)));
            writer.write("]]>\n");
            writer.write("\t\t\t\t</description>\n");
            writer.write("\t\t\t\t<styleUrl>#msn_Z</styleUrl>\n");
            writer.write("\t\t\t\t<Point>\n");
            writer.write("\t\t\t\t\t<coordinates>" + Double.toString(session.getGpsPos().lastElement().longitude) + ","
                    + Double.toString(session.getGpsPos().lastElement().latitude) + ",0</coordinates>\n");
            writer.write("\t\t\t\t</Point>\n");
            writer.write("\t\t\t</Placemark>\n");
            writer.write("\t\t</Folder>\n");
            writer.write("\t\t<Placemark>\n");
            writer.write("\t\t\t<name>Route</name>\n");
            writer.write("\t\t\t<description>" + session.getSettings().getDescription() + "</description>\n");
            writer.write("\t\t\t<styleUrl>#msn_ylw-pushpin</styleUrl>\n");
            writer.write("\t\t\t<LineString>\n");
            writer.write("\t\t\t\t<tessellate>1</tessellate>\n");
            writer.write("\t\t\t\t<altitudeMode>relativeToGround</altitudeMode>\n");
            writer.write("\t\t\t\t<coordinates>\n");
            for (GpsPos pos : session.getGpsPos()) {
                writer.write("\t\t\t\t\t");
                writer.write(Double.toString(pos.longitude));
                writer.write(",");
                writer.write(Double.toString(pos.latitude) + ",0\n");
            }
            writer.write("\t\t\t\t</coordinates>\n");
            writer.write("\t\t\t</LineString>\n");
            writer.write("\t\t</Placemark>\n");
            writer.write("\t</Document>\n");
            writer.write("</kml>\n");

            writer.flush();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
}
