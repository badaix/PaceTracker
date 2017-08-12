package de.badaix.pacetracker.maps;

public class TilePos {
    public double x;
    public double y;
    public int z;

    public TilePos(TilePos tilePos) {
        this.z = tilePos.z;
        this.x = tilePos.x;
        this.y = tilePos.y;
    }

    public TilePos(int z, double x, double y) {
        this.z = z;
        this.x = x;
        this.y = y;
    }

    public TilePos zoomOut() {
        if (z == 0)
            return this;

        x = Math.floor(x / 2.0);
        y = Math.floor(y / 2.0);
        --z;
        return this;
    }

    @Override
    public int hashCode() {
        return (int) (Math.round(x) * Math.round(y));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TilePos)) {
            return false; // different class
        }
        TilePos other = (TilePos) obj;
        if ((other.x == x) && (other.y == y) && (other.z == z))
            return true;

        return false;
    }

}
