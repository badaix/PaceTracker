package de.badaix.pacetracker.session;

import java.util.Date;

public class RouteInfo {
    protected String name = "";
    protected String copyright = "";
    protected String source = "";
    protected String description = "";
    protected String type = "";
    protected String filename = "";
    protected String from = "";
    protected String to = "";
    protected GeoPos startPos = null;
    protected GeoPos endPos = null;
    protected Date created = new Date();
    protected long id = -1;
    protected int version = 1;
    private double distance = -1;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public GeoPos getStartPos() {
        return startPos;
    }

    public void setStartPos(GeoPos geoPos) {
        this.startPos = geoPos;
    }

    public GeoPos getEndPos() {
        return endPos;
    }

    public void setEndPos(GeoPos endPos) {
        this.endPos = endPos;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
