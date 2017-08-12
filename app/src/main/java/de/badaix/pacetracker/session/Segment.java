package de.badaix.pacetracker.session;

import java.util.Vector;

public class Segment {
    public Vector<GeoPos> positions = new Vector<GeoPos>();
    private String instruction = "";
    private String name = "";
    private String imageUrl = "";

    public Segment(String name) {
        this.name = name;
    }

    public Segment() {
    }

    public Vector<GeoPos> getPositions() {
        return positions;
    }

    public void setPositions(Vector<GeoPos> positions) {
        this.positions = positions;
    }

    public void addPosition(GeoPos position) {
        this.positions.add(position);
    }

    public double getLength() {
        if (positions.isEmpty())
            return 0.;
        return positions.lastElement().distance - positions.firstElement().distance;
    }

    public double getDistance() {
        if (positions.isEmpty())
            return 0.;
        return positions.firstElement().distance;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
