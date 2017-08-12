package de.badaix.pacetracker.maps;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import de.badaix.pacetracker.maps.TileSourceFactory.TileSource;
import de.badaix.pacetracker.util.Hint;

public class BaseTileSource {

    protected final TileSource tileSource;
    protected Random random;

    public BaseTileSource(final TileSource tileSource) {
        this.tileSource = tileSource;
        random = new Random();
    }

    /*
     * public BaseTileSource(final String name, final String extension, final
     * int zoomMinLevel, final int zoomMaxLevel, final int tileSizePixels, final
     * String... baseUrls) { this.name = name; this.zoomMinLevel = zoomMinLevel;
     * this.zoomMaxLevel = zoomMaxLevel; this.tileSizePixels = tileSizePixels;
     * this.baseUrls = baseUrls; this.extension = extension; random = new
     * Random(); }
     */
    public URL getTileURL(int idx, int x, int y, int zoom) {
        try {
            String strUlr = tileSource.baseUrls[idx]
                    + tileSource.tileExtension.replace("{x}", "" + x).replace("{y}", "" + y).replace("{z}", "" + zoom);

            URL url = new URL(strUlr);
            // Hint.log(this, url.toString());
            return url;
        } catch (MalformedURLException e) {
            Hint.log(this, e);
        }
        return null;
    }

    public URL getTileURL(int x, int y, int zoom) {
        return getTileURL(random.nextInt(tileSource.baseUrls.length), x, y, zoom);
    }

    public URL getTileURL(TilePos tilePos) {
        return getTileURL((int) tilePos.x, (int) tilePos.y, tilePos.z);
    }

    public TileSource getTileSource() {
        return tileSource;
    }

    public String getName() {
        return tileSource.name;
    }

    public String getCopyright() {
        return tileSource.copyright;
    }

    public String getTileExtension() {
        return tileSource.tileExtension;
    }

    public String getExtension() {
        return tileSource.extension;
    }

    public int getZoomMinLevel() {
        return tileSource.zoomMinLevel;
    }

    public int getZoomMaxLevel() {
        return tileSource.zoomMaxLevel;
    }

    public int getTileSizePixels() {
        return tileSource.tileSizePixels;
    }

}
