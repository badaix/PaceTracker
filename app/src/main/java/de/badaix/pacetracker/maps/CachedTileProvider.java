package de.badaix.pacetracker.maps;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import org.apache.http.client.HttpResponseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import de.badaix.pacetracker.util.FileUtils;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.HttpDownloader;

public class CachedTileProvider implements TileProvider {

    protected BaseTileSource tileSource;
    protected File basePath;

    // static Semaphore semaphore = new Semaphore(6);

    public CachedTileProvider(Context context, BaseTileSource tileSource) {
        this.tileSource = tileSource;
        basePath = FileUtils.getExternalDir("PaceTracker", "Maps");
    }

    public BaseTileSource getTileSource() {
        return tileSource;
    }

    public File getBasePath() {
        return basePath;
    }

    public String getTileName(int x, int y, int zoom) {
        String result = tileSource.getName() + "/" + zoom + "/" + x + "/" + y + "." + tileSource.getExtension();
        // Hint.log(this, "TileFilename: " + result);
        return result;
    }

    public File getTileFile(int x, int y, int zoom) {
        return new File(basePath, getTileName(x, y, zoom));
    }

    public Tile getTileFromDisk(int x, int y, int zoom) {
        if (zoom > tileSource.getZoomMaxLevel())
            return NO_TILE;

        File tileFilename = getTileFile(x, y, zoom);
        if (!tileFilename.exists()) {
            return null;
        }

        try {
            InputStream input = null;
            try {
                input = new FileInputStream(tileFilename);
                int length = (int) tileFilename.length();
                if (length == 0)
                    return NO_TILE;

                byte bytes[] = new byte[length];
                input.read(bytes);

                return new Tile(tileSource.getTileSizePixels(), tileSource.getTileSizePixels(), bytes);
            } finally {
                if (input != null)
                    input.close();
            }
        } catch (Exception e) {
            Hint.log(this, e);
            return null;
        }
    }

    public Tile getTileFromCache(int x, int y, int zoom) {
        if (zoom > tileSource.getZoomMaxLevel())
            return NO_TILE;

        try {
            byte[] bytes = null;// bos.toByteArray();
            String tileName = getTileName(x, y, zoom);
            bytes = TileCache.getInstance().getTileFromDiskCache(tileName);
            if (bytes == null) {
                return null;
            }
            if (bytes.length == 0)
                return NO_TILE;

            // Hint.log(this, "TileCache hit: " + tileName);
            return new Tile(tileSource.getTileSizePixels(), tileSource.getTileSizePixels(), bytes);
        } catch (Exception e) {
            Hint.log(this, e);
            return null;
        }
    }

    public Tile getTileFromUrl(int x, int y, int zoom) throws IOException {
        if (zoom > tileSource.getZoomMaxLevel())
            return NO_TILE;

        URL url = tileSource.getTileURL(x, y, zoom);
        byte[] result = null;
        try {
            result = new HttpDownloader().getBytes(url);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404)
                return NO_TILE;
            throw e;
        }

        if (result == null)
            return null;

        return new Tile(tileSource.getTileSizePixels(), tileSource.getTileSizePixels(), result);
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        Hint.log(this, "getTile: " + zoom + "/" + x + "/" + y);
        if (zoom > tileSource.getZoomMaxLevel())
            return NO_TILE;

        Tile tile = getTileFromCache(x, y, zoom);
        if (tile != null)
            return tile;

        tile = getTileFromDisk(x, y, zoom);
        if (tile != null)
            return tile;

        try {
            tile = getTileFromUrl(x, y, zoom);
            if (!NO_TILE.equals(tile) && (BitmapFactory.decodeByteArray(tile.data, 0, tile.data.length) == null))
                throw new IOException("Error decoding tile");
        } catch (IOException e) {
            Hint.log(this, e);
            tile = null;
        }

        if (tile != null) {
            if (NO_TILE.equals(tile))
                TileCache.getInstance().addTileToCache(getTileName(x, y, zoom), new byte[0]);
            else
                TileCache.getInstance().addTileToCache(getTileName(x, y, zoom), tile.data);
            return tile;
        }

        return null;
    }

}
