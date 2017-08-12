package de.badaix.pacetracker.maps;

import java.util.HashSet;
import java.util.Vector;

import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.util.BoundingBox;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

public class TileUtils {
    static public TilePos getTileNumber(final GeoPos pos, final int zoom) {
        int xtile = (int) Math.floor((pos.longitude + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(pos.latitude)) + 1
                / Math.cos(Math.toRadians(pos.latitude)))
                / Math.PI)
                / 2 * (1 << zoom));

        return new TilePos(zoom, xtile, ytile);
    }

    static public double getTileSizeMeters(final int zoom) {
        final double earth = 40041472.015861;
        return earth / Math.pow(2, zoom);
    }

    static public BoundingBox tile2boundingBox(final int x, final int y, final int zoom) {
        return new BoundingBox(tile2lat(y, zoom), tile2lon(x, zoom), tile2lat(y + 1, zoom), tile2lon(x + 1, zoom));
    }

    static public double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    static public double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    static public HashSet<TilePos> addMargin(HashSet<TilePos> tiles, final int marginM) {
        HashSet<TilePos> result = new HashSet<TilePos>();
        int marginTiles = -1;
        for (TilePos tile : tiles) {
            if (marginTiles == -1) {
                double size = TileUtils.getTileSizeMeters(tile.z);
                marginTiles = (int) Math.ceil((double) marginM / size);
                Hint.log("TileUtils", "Margin: " + marginM + ", TileSize: " + size + ", TileMargin: " + marginTiles);
            }

            for (int x = -marginTiles; x <= marginTiles; ++x) {
                for (int y = -marginTiles; y <= marginTiles; ++y) {
                    TilePos tilePos = new TilePos(tile);
                    tilePos.x += x;
                    tilePos.y += y;
                    result.add(tilePos);
                }
            }
        }
        return result;
    }

    static public HashSet<TilePos> boundingBoxToTile(BoundingBox boundingBox, final int zoom) {
        GeoPos nw = new GeoPos(boundingBox.getMaxLat(), boundingBox.getMinLon());
        GeoPos so = new GeoPos(boundingBox.getMinLat(), boundingBox.getMaxLon());
        TilePos tileNW = TileUtils.getTileNumber(nw, zoom);
        TilePos tileSO = TileUtils.getTileNumber(so, zoom);
        HashSet<TilePos> tileSet = new HashSet<TilePos>();
        if (nw.longitude - so.longitude > 180.0) {
            for (double x = tileNW.x; x >= 0; --x)
                for (double y = tileNW.y; y <= tileSO.y; ++y)
                    tileSet.add(new TilePos(zoom, x, y));
            for (double x = Math.pow(2, zoom); x >= tileSO.x; --x)
                for (double y = tileNW.y; y <= tileSO.y; ++y)
                    tileSet.add(new TilePos(zoom, x, y));
        } else {
            for (double x = tileNW.x; x <= tileSO.x; ++x)
                for (double y = tileNW.y; y <= tileSO.y; ++y)
                    tileSet.add(new TilePos(zoom, x, y));
        }
        return tileSet;
    }

    static public HashSet<TilePos> routeToTile(Vector<? extends GeoPos> route, final int zoom) {
        Vector<GeoPos> decimatedRoute = new Vector<GeoPos>();
        LocationUtils.decimate(5.0, route, decimatedRoute);
        Vector<TilePos> tileRoute = new Vector<TilePos>();
        HashSet<TilePos> tileSet = new HashSet<TilePos>();

        // Route to TileRoute
        for (GeoPos pos : decimatedRoute) {
            TilePos tilePos = TileUtils.getTileNumber(pos, zoom);
            if (tileRoute.isEmpty() || !tileRoute.lastElement().equals(tilePos)) {
                tileRoute.add(tilePos);
                tileSet.add(tilePos);
            }
        }

        // Fill gaps
        TilePos first;
        TilePos second;
        for (int i = 1; i < tileRoute.size(); ++i) {
            first = tileRoute.get(i - 1);
            second = tileRoute.get(i);
            double diffX = second.x - first.x;
            double diffY = second.y - first.y;
            if ((Math.abs(diffX) + Math.abs(diffY)) > 1.0) {
                Hint.log("TileUtils", "distance > 1: " + first.x + " " + first.y + ", " + second.x + " " + second.y);
                double steps = Math.max(Math.abs(diffX), Math.abs(diffY));
                diffX /= steps;
                diffY /= steps;
                for (double s = 0.5; s < steps; s += 0.5) {
                    double addX = (first.x + s * diffX);
                    double addY = (first.y + s * diffY);
                    tileSet.add(new TilePos(zoom, Math.floor(addX), Math.floor(addY)));
                    tileSet.add(new TilePos(zoom, Math.floor(addX), Math.ceil(addY)));
                    tileSet.add(new TilePos(zoom, Math.ceil(addX), Math.floor(addY)));
                    tileSet.add(new TilePos(zoom, Math.ceil(addX), Math.ceil(addY)));
                }
            }
        }
        return tileSet;
    }

    static public HashSet<TilePos> zoomOut(HashSet<TilePos> tiles) {
        HashSet<TilePos> coarseTiles = new HashSet<TilePos>();
        for (TilePos tile : tiles) {
            coarseTiles.add(new TilePos(tile).zoomOut());
        }
        return coarseTiles;
    }

}
