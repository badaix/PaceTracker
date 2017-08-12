package de.badaix.pacetracker.maps;

import de.badaix.pacetracker.util.DiskCache;

public class TileCache extends DiskCache {
    private static TileCache instance = null;

    public static TileCache getInstance() {
        if (instance == null) {
            instance = new TileCache();
        }
        return instance;
    }

    public void addTileToCache(String id, byte[] tile) {
        addDataToCache(id, tile);
    }

    public byte[] getTileFromDiskCache(String name) {
        return getDataFromDiskCache(name);
    }
}
