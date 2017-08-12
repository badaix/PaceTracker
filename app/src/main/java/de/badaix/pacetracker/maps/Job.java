package de.badaix.pacetracker.maps;

import java.util.HashSet;

import de.badaix.pacetracker.maps.TileSourceFactory.TileSource;

public class Job {
    public HashSet<TilePos> tiles;
    public TileSource tileSource;

    public Job(HashSet<TilePos> tiles, TileSource tileSource) {
        this.tileSource = tileSource;
        this.tiles = tiles;
    }
}
