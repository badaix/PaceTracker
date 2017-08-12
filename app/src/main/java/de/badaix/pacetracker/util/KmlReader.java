package de.badaix.pacetracker.util;

import android.text.TextUtils;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.session.Segment;
import de.badaix.pacetracker.settings.GlobalSettings;

public class KmlReader {

    public static Route parseKml(String kml) {
        try {
            /* Get a SAXParser from the SAXPArserFactory. */
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

			/* Get the XMLReader of the SAXParser we created. */
            XMLReader xr = sp.getXMLReader();

			/* Create a new ContentHandler and apply it to the XML-Reader */
            KmlHandler navSax2Handler = new KmlHandler();
            xr.setContentHandler(navSax2Handler);

			/* Parse the xml-data from our URL. */
            xr.parse(new InputSource(new StringReader(kml)));

			/* Our NavigationSaxHandler now provides the parsed data to us. */
            // navigationDataSet =
            // navSax2Handler.getParsedData();
            return navSax2Handler.getRoute();
        } catch (Exception e) {
            Hint.log("parseKml", e);
            return null;
        }
    }

}

class KmlHandler extends DefaultHandler {

    // ===========================================================
    // Fields
    // ===========================================================

    // private boolean in_kmltag = false;
    private boolean in_placemarktag = false;
    private boolean in_nametag = false;
    private boolean in_descriptiontag = false;
    // private boolean in_geometrycollectiontag = false;
    private boolean in_stylemaptag = false;
    // private boolean in_pairtag = false;
    private boolean in_styleurl = false;
    private boolean in_styletag = false;
    private boolean in_iconstyletag = false;
    private boolean in_icontag = false;
    private boolean in_href = false;
    private boolean in_linestringtag = false;
    private boolean in_pointtag = false;
    private boolean in_coordinatestag = false;
    private Segment currentSegment = null;

    private String docName = "";
    private String coordinate = "";
    private String currentStyle = null;
    private HashMap<String, String> styleMap = null;
    private StringBuffer buffer = new StringBuffer();
    private Vector<Segment> segments = new Vector<Segment>();

    // private NavigationDataSet navigationDataSet = new NavigationDataSet();

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    private GeoPos coordStringToGeoPos(String coords) {
        // Hint.log(this, "Coords: " + coords);
        String[] coord = coords.split(",");
        if (coord.length == 1)
            coord = coords.split(" ");
        // Hint.log(this, "lat: " + coord[0].trim() +", lon: " +
        // coord[1].trim());
        return new GeoPos(Double.parseDouble(coord[1].trim()), Double.parseDouble(coord[0].trim()));
    }

    private void resolveImage(Segment segment) {
        String style = segment.getImageUrl();
        if (TextUtils.isEmpty(style) || !style.startsWith("#"))
            return;

        if (style.startsWith("#")) {
            style = style.substring(1);

            if (styleMap.containsKey(style))
                segment.setImageUrl(styleMap.get(style));
            else
                segment.setImageUrl("");
        }

        if (segment.getImageUrl().startsWith("#"))
            resolveImage(segment);
    }

    public Route getRoute() {
        try {
            Vector<GeoPos> coordinates = new Vector<GeoPos>();
            String[] coords = buffer.toString().trim().split("\\s+");
            for (int i = 0; i < coords.length; ++i) {
                try {
                    coordinates.add(coordStringToGeoPos(coords[i]));
                } catch (Exception e) {
                    Hint.log(this, e);
                }
            }
            if (coordinates.isEmpty() || segments.isEmpty())
                return null;

            Route route = new Route();
            for (int i = 0; i < segments.size(); ++i) {
                Segment segment = segments.get(i);
                if (segment.positions.isEmpty()) {
                    segments.set(i, null);
                    continue;
                }

                resolveImage(segment);

                GeoPos segmentPos = segment.positions.firstElement();
                segment.positions.setSize(1);
                int closestIdx = 0;

                double dist = Double.MAX_VALUE;
                for (int j = 0; j < coordinates.size() - 1; ++j) {
                    if (coordinates.get(j).equals(segmentPos)) {
                        Hint.log(this, "Adding segment: " + segment.getName() + ", distance: 0");
                        continue;
                    }

                    double currentDist = LocationUtils.distance(segmentPos, coordinates.get(j), coordinates.get(j + 1));
                    if (currentDist < dist) {
                        dist = currentDist;
                        closestIdx = j + 1;
                    }
                }
                if (dist <= 50) {
                    Hint.log(this, "Adding segment: " + segment.getName() + ", distance: " + dist);
                    coordinates.add(closestIdx, segmentPos);
                } else
                    segments.set(i, null);
            }

            Segment currentSegment = new Segment("Start");
            coordinates.firstElement().distance = 0;
            for (int i = 1; i < coordinates.size(); ++i)
                coordinates.get(i).distance = coordinates.get(i - 1).distance
                        + Distance.calculateDistance(coordinates.get(i - 1), coordinates.get(i));

            for (GeoPos pos : coordinates) {
                for (Segment segment : segments) {
                    if ((segment != null) && !segment.positions.isEmpty()
                            && (pos.equals(segment.positions.firstElement()))) {
                        route.addSegment(currentSegment);
                        currentSegment = segment;
                    }
                }
                if (currentSegment != null)
                    currentSegment.addPosition(pos);
            }

            route.setName(docName);
            route.setDescription(docName);
            return route;
        } catch (Exception e) {
            Hint.show(GlobalSettings.getInstance().getContext(), e);
            return null;
        }
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
        if (localName.equalsIgnoreCase("kml")) {
            currentSegment = null;
            segments = new Vector<Segment>();
            styleMap = new HashMap<String, String>();
        } else if (localName.equalsIgnoreCase("Placemark")) {
            this.in_placemarktag = true;
            currentSegment = new Segment();
            // navigationDataSet.setCurrentPlacemark(new Placemark());
        } else if (localName.equalsIgnoreCase("name")) {
            this.in_nametag = true;
        } else if (localName.equalsIgnoreCase("Style")) {
            currentStyle = atts.getValue("id");
            this.in_styletag = true;
        } else if (localName.equalsIgnoreCase("StyleMap")) {
            currentStyle = atts.getValue("id");
            this.in_stylemaptag = true;
            // } else if (localName.equalsIgnoreCase("Pair")) {
            // this.in_pairtag = true;
        } else if (localName.equalsIgnoreCase("IconStyle")) {
            this.in_iconstyletag = true;
        } else if (localName.equalsIgnoreCase("Icon")) {
            this.in_icontag = true;
        } else if (localName.equalsIgnoreCase("href")) {
            this.in_href = true;
        } else if (localName.equalsIgnoreCase("styleUrl")) {
            this.in_styleurl = true;
        } else if (localName.equalsIgnoreCase("description")) {
            this.in_descriptiontag = true;
        } else if (localName.equalsIgnoreCase("LineString")) {
            this.in_linestringtag = true;
        } else if (localName.equalsIgnoreCase("point")) {
            this.in_pointtag = true;
        } else if (localName.equalsIgnoreCase("coordinates")) {
            if (in_linestringtag)
                buffer = new StringBuffer();
            this.in_coordinatestag = true;
            coordinate = "";
        } else if (localName.equalsIgnoreCase("coord")) {
            // Hint.log(this, "gx:coord start");
            this.in_pointtag = true;
            this.in_coordinatestag = true;
            coordinate = "";
        }
    }

    /**
     * Gets be called on closing tags like: </tag>
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (localName.equalsIgnoreCase("Placemark")) {
            this.in_placemarktag = false;
            segments.add(currentSegment);
            // if ("Route".equals(navigationDataSet.getCurrentPlacemark()
            // .getTitle()))
            // navigationDataSet.setRoutePlacemark(navigationDataSet
            // .getCurrentPlacemark());
            // else
            // navigationDataSet.addCurrentPlacemark();

        } else if (localName.equalsIgnoreCase("name")) {
            this.in_nametag = false;
        } else if (localName.equalsIgnoreCase("description")) {
            this.in_descriptiontag = false;
        } else if (localName.equalsIgnoreCase("Style")) {
            this.in_styletag = false;
        } else if (localName.equalsIgnoreCase("StyleMap")) {
            this.in_stylemaptag = false;
            // } else if (localName.equalsIgnoreCase("Pair")) {
            // this.in_pairtag = true;
        } else if (localName.equalsIgnoreCase("IconStyle")) {
            this.in_iconstyletag = false;
        } else if (localName.equalsIgnoreCase("Icon")) {
            this.in_icontag = false;
        } else if (localName.equalsIgnoreCase("href")) {
            this.in_href = false;
        } else if (localName.equalsIgnoreCase("styleUrl")) {
            this.in_styleurl = false;
        } else if (localName.equalsIgnoreCase("LineString")) {
            this.in_linestringtag = false;
        } else if (localName.equalsIgnoreCase("point")) {
            this.in_pointtag = false;
        } else if (localName.equalsIgnoreCase("coordinates")) {
            this.in_coordinatestag = false;
            try {
                currentSegment.addPosition(coordStringToGeoPos(coordinate));
            } catch (Exception e) {
                Hint.log(this, e);
            }
        } else if (localName.equalsIgnoreCase("coord")) {
            // Hint.log(this, "gx:coord end");
            this.in_coordinatestag = false;
            try {
                currentSegment.addPosition(coordStringToGeoPos(coordinate));
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    /**
     * Gets be called on the following structure: <tag>characters</tag>
     */
    @Override
    public void characters(char ch[], int start, int length) {
        if (this.in_nametag) {
            // if (navigationDataSet.getCurrentPlacemark() == null)
            // navigationDataSet.setCurrentPlacemark(new Placemark());
            // navigationDataSet.getCurrentPlacemark().setTitle(
            // new String(ch, start, length));
            String name = new String(ch, start, length).trim();
            if (in_placemarktag && (currentSegment != null))
                currentSegment.setName(name);

            Hint.log(this, "Name: " + name);
            if (!in_placemarktag && docName.equals(""))
                docName = name;
        } else if (this.in_descriptiontag) {
            // if (navigationDataSet.getCurrentPlacemark() == null)
            // navigationDataSet.setCurrentPlacemark(new Placemark());
            // navigationDataSet.getCurrentPlacemark().setDescription(
            // new String(ch, start, length));
            if (in_placemarktag && (currentSegment != null))
                currentSegment.setInstruction(new String(ch, start, length).trim());
            Hint.log(this, "Description: " + new String(ch, start, length));
        } else if (this.in_styleurl) {
            if (in_placemarktag && (currentSegment != null)) {
                currentSegment.setImageUrl(new String(ch, start, length).trim());
            } else if (in_stylemaptag && (currentStyle != null) && !styleMap.containsKey(currentStyle)) {
                styleMap.put(currentStyle, new String(ch, start, length).trim());
            }
        } else if (this.in_href) {
            if (in_styletag && in_iconstyletag && in_icontag && (currentStyle != null)) {
                styleMap.put(currentStyle, new String(ch, start, length).trim());
            }
        } else if (this.in_coordinatestag) {
            // if (navigationDataSet.getCurrentPlacemark() == null)
            // navigationDataSet.setCurrentPlacemark(new Placemark());
            // // navigationDataSet.getCurrentPlacemark().setCoordinates(new
            // // String(ch, start, length));
            // Hint.log(this, "inCoord line: " + in_linestringtag +
            // ", placemark: " + in_placemarktag + ", point: " + in_pointtag);
            if (this.in_linestringtag) {
                buffer.append(ch, start, length);
            } else if (in_placemarktag && this.in_pointtag && (currentSegment != null)) {
                coordinate += new String(ch, start, length);
            }
        }
    }
}
