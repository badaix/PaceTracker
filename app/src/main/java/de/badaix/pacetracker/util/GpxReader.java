package de.badaix.pacetracker.util;

import android.text.TextUtils;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringReader;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.session.Segment;
import de.badaix.pacetracker.settings.GlobalSettings;

public class GpxReader {
    public static Route parseGpx(String gpx) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

            XMLReader xr = sp.getXMLReader();
            GpxHandler navSax2Handler = new GpxHandler();
            xr.setContentHandler(navSax2Handler);
            xr.parse(new InputSource(new StringReader(gpx)));

            return navSax2Handler.getRoute();
        } catch (Exception e) {
            Hint.log("parseKml", e);
            return null;
        }
    }

}

class GpxHandler extends DefaultHandler {

    // ===========================================================
    // Fields
    // ===========================================================

    // private boolean in_kmltag = false;
    private boolean in_metadata = false;
    private boolean in_trk = false;
    private boolean in_trkseg = false;
    private boolean in_trkpt = false;
    private boolean in_name = false;
    private boolean in_desc = false;
    private boolean in_trkName = false;
    private boolean in_trkDesc = false;
    @SuppressWarnings("unused")
    private boolean in_ele = false;
    @SuppressWarnings("unused")
    private boolean in_time = false;
    private Segment currentSegment = null;

    private String name = "";
    private String description = "";
    private String trkName = "";
    private String trkDescription = "";
    private GeoPos lastPos = null;
    private Vector<Segment> segments = new Vector<Segment>();

    public Route getRoute() {
        Route route = new Route();
        for (int i = 0; i < segments.size(); ++i) {
            Segment segment = segments.get(i);
            if (segment.positions.isEmpty())
                continue;

            route.addSegment(segment);
        }

        Vector<Segment> segments = route.getSegments();
        if ((segments == null) || segments.isEmpty())
            return null;

        segments.firstElement().setImageUrl("http://content.mapquest.com/mqsite/turnsigns/icon-dirs-start_sm.gif");
        if (TextUtils.isEmpty(name))
            name = segments.firstElement().getName();

        if (TextUtils.isEmpty(segments.firstElement().getName()))
            segments.firstElement().setName(GlobalSettings.getInstance().getContext().getString(R.string.start));

        if (segments.size() == 1) {
            Segment last = new Segment(GlobalSettings.getInstance().getContext().getString(R.string.end));
            GeoPos lastPos = segments.firstElement().getPositions().lastElement();
            last.addPosition(lastPos);
            segments.firstElement().positions.remove(lastPos);
            last.setImageUrl("http://content.mapquest.com/mqsite/turnsigns/icon-dirs-end_sm.gif");
            segments.add(last);
        }

        route.setName(name);
        route.setDescription(description);
        return route;
    }

    // ===========================================================
    // Methods
    // ===========================================================
    @Override
    public void startDocument() throws SAXException {
        // this.navigationDataSet = new NavigationDataSet();
    }

    @Override
    public void endDocument() throws SAXException {
        // Nothing to do
    }

    /**
     * Gets be called on opening tags like: <tag> Can provide attribute(s), when
     * xml was like: <tag attribute="attributeValue">
     */
    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (localName.equalsIgnoreCase("gpx")) {
            currentSegment = null;
            segments = new Vector<Segment>();
        } else if (localName.equalsIgnoreCase("trk")) {
            this.in_trk = true;
            currentSegment = new Segment();
        } else if (localName.equalsIgnoreCase("metadata")) {
            this.in_metadata = true;
        } else if (in_metadata && localName.equalsIgnoreCase("name")) {
            this.in_name = true;
        } else if (in_metadata && localName.equalsIgnoreCase("desc")) {
            this.in_desc = true;
        } else if (in_trk && localName.equalsIgnoreCase("name")) {
            this.in_trkName = true;
        } else if (in_trk && localName.equalsIgnoreCase("desc")) {
            this.in_trkDesc = true;
        } else if (in_trk && localName.equalsIgnoreCase("trkseg")) {
            in_trkseg = true;
        } else if (in_trkseg && localName.equalsIgnoreCase("trkpt")) {
            this.in_trkpt = true;
            Hint.log(this, "Lat: " + atts.getValue("lat") + ", Lon: " + atts.getValue("lon"));
            GeoPos pos = new GeoPos(Double.parseDouble(atts.getValue("lat")), Double.parseDouble(atts.getValue("lon")));
            if (lastPos != null)
                pos.distance = lastPos.distance + Distance.calculateDistance(lastPos, pos);
            else
                pos.distance = 0;
            lastPos = pos;
            currentSegment.addPosition(pos);
        } else if (in_trkpt && localName.equalsIgnoreCase("ele")) {
            this.in_ele = true;
        } else if (in_trkpt && localName.equalsIgnoreCase("time")) {
            this.in_time = true;
        }
    }

    /**
     * Gets be called on closing tags like: </tag>
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

        if (localName.equalsIgnoreCase("trk")) {
            this.in_trk = false;
            segments.add(currentSegment);
        } else if (localName.equalsIgnoreCase("metadata")) {
            this.in_metadata = false;
        } else if (in_trk && localName.equalsIgnoreCase("name")) {
            this.in_trkName = false;
            Hint.log(this, "trkName: " + trkName);
            currentSegment.setName(trkName);
            trkName = "";
        } else if (in_trk && localName.equalsIgnoreCase("desc")) {
            this.in_trkDesc = false;
            Hint.log(this, "trkDesc: " + trkDescription);
        } else if (in_metadata && localName.equalsIgnoreCase("name")) {
            this.in_name = false;
            Hint.log(this, "Name: " + name);
        } else if (in_metadata && localName.equalsIgnoreCase("desc")) {
            this.in_desc = false;
            Hint.log(this, "Desc: " + description);
        } else if (in_trk && localName.equalsIgnoreCase("trkseg")) {
            in_trkseg = false;
        } else if (in_trkseg && localName.equalsIgnoreCase("trkpt")) {
            this.in_trkpt = false;
        } else if (in_trkpt && localName.equalsIgnoreCase("ele")) {
            this.in_ele = false;
        } else if (in_trkpt && localName.equalsIgnoreCase("time")) {
            this.in_time = false;
        }
    }

    /**
     * Gets be called on the following structure: <tag>characters</tag>
     */
    @Override
    public void characters(char ch[], int start, int length) {
        if (this.in_name) {
            name += new String(ch, start, length);
        } else if (this.in_desc) {
            description += new String(ch, start, length);
        } else if (this.in_trkName) {
            trkName += new String(ch, start, length);
        } else if (this.in_trkDesc) {
            trkDescription += new String(ch, start, length);
        }
    }

}
