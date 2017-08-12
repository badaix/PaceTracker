package de.badaix.pacetracker.maps;

import android.content.Context;

public class TileSourceFactory {
    // http://vatavia.net/mark/VataviaMap/servers.html

    static public BaseTileSource getTileSource(final TileSource tileSource) {
        if (tileSource.baseUrls == null)
            return null;

        return new BaseTileSource(tileSource);
    }

    static public CachedTileProvider getTileProvider(final Context context, final TileSource tileSource) {
        BaseTileSource baseTileSource = getTileSource(tileSource);
        if (baseTileSource == null)
            return null;

        return new CachedTileProvider(context, baseTileSource);
    }

    public enum TileSource {
        NONE("None", "", "", "", 0, 20, 256, (String[]) null),

        GOOGLE("Google", "", "&x={x}&y={y}&z={z}", "png", 0, 20, 256, (String[]) null),

        GOOGLE_BITMAP("Google (bitmap)", "", "&x={x}&y={y}&z={z}", "png", 0, 20, 256,
                "http://mt0.google.com/vt/lyrs=m", "http://mt1.google.com/vt/lyrs=m",
                "http://mt2.google.com/vt/lyrs=m", "http://mt3.google.com/vt/lyrs=m"),

        GOOGLESATELLITE("Google satellite", "", "&x={x}&y={y}&z={z}", "jpg", 0, 20, 256, (String[]) null),

        GOOGLETERRAIN("Google terrain", "", "&x={x}&y={y}&z={z}", "jpg", 0, 20, 256, (String[]) null);

        // GOOGLESATELLITE("Google satellite", "", "&x={x}&y={y}&z={z}", "jpg",
        // 0, 20, 256,
        // "http://mt0.google.com/vt/lyrs=y",
        // "http://mt1.google.com/vt/lyrs=y",
        // "http://mt2.google.com/vt/lyrs=y",
        // "http://mt3.google.com/vt/lyrs=y"),
        /*MAPNIK("Mapnik", "© <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors",
                "{z}/{x}/{y}.png", "png", 0, 18, 256, "http://tile.openstreetmap.org/"),

        MAPQUESTOSM("MapQuest-OSM",
                "Tiles Courtesy of <a href=\"http://www.mapquest.com/\" target=\"_blank\">MapQuest</a>",
                "{z}/{x}/{y}.jpg", "jpg", 0, 18, 256, "http://otile1.mqcdn.com/tiles/1.0.0/map/",
                "http://otile2.mqcdn.com/tiles/1.0.0/map/", "http://otile3.mqcdn.com/tiles/1.0.0/map/",
                "http://otile4.mqcdn.com/tiles/1.0.0/map/"),

        MAPQUESTOPENAERIAL("MapQuest Open Aerial", "", "{z}/{x}/{y}.jpg", "jpg", 0, 18, 256,
                "http://otile1.mqcdn.com/tiles/1.0.0/sat/", "http://otile2.mqcdn.com/tiles/1.0.0/sat/",
                "http://otile3.mqcdn.com/tiles/1.0.0/sat/", "http://otile4.mqcdn.com/tiles/1.0.0/sat/"),

        CYCLEMAP("OpenCycleMap",
                "Map tiles by <a href=\"http://www.opencyclemap.org\" target=\"_blank\">OpenCycleMap</a>",
                "{z}/{x}/{y}.png", "png", 0, 17, 256, "http://a.tile.opencyclemap.org/cycle/",
                "http://b.tile.opencyclemap.org/cycle/", "http://c.tile.opencyclemap.org/cycle/"),

        OSMPUBLICTRANSPORT("OpnvKarte", "", "{z}/{x}/{y}.png", "png", 0, 17, 256,
                "http://tile.xn--pnvkarte-m4a.de/tilegen/"),

        SEAMARK("OpenSeaMap", "", "{z}/{x}/{y}.png", "png", 0, 18, 256, "http://tiles.openseamap.org/seamark/"),

        OPENPISTEMAP("OpenPisteMap", "", "{z}/{x}/{y}.png", "png", 0, 17, 256,
                "http://tiles.openpistemap.org/nocontours/"),

        OPENPISTEMAPSHADED("OpenPisteMap landshaded", "", "{z}/{x}/{y}.png", "png", 0, 17, 256,
                "http://tiles2.openpistemap.org/landshaded/"),

        WATERCOLOR("Watercolor", "Map tiles by <a href=\"http://www.stamen.com\" target=\"_blank\">Stamen Design</a>",
                "{z}/{x}/{y}.jpg", "jpg", 0, 17, 256, "http://tile.stamen.com/watercolor/"),

        GEOCACHING("Geocaching", "", "?x={x}&y={y}&z={z}", "png", 0, 18, 256, "http://www.geocaching.com/map/map.png"),

        NOKIA("Nokia", "", "{z}/{x}/{y}/256/png8", "png", 0, 20, 256,
                "http://maptile.maps.svc.ovi.com/maptiler/maptile/newest/normal.day/");
*/
        public final String baseUrls[];
        public final String name;
        public final String tileExtension;
        public final String extension;
        public final int zoomMinLevel;
        public final int zoomMaxLevel;
        public final int tileSizePixels;
        public final String copyright;

        TileSource(final String name, final String copyright, final String tileExtension, final String extension,
                   final int zoomMinLevel, final int zoomMaxLevel, final int tileSizePixels, final String... baseUrls) {
            this.name = name;
            this.copyright = copyright;
            this.zoomMinLevel = zoomMinLevel;
            this.zoomMaxLevel = zoomMaxLevel;
            this.tileSizePixels = tileSizePixels;
            this.baseUrls = baseUrls;
            this.extension = extension;
            this.tileExtension = tileExtension;
        }

        public static TileSource fromEnumString(String name) {
            for (TileSource source : TileSource.values()) {
                if (source.toEnumString().equals(name))
                    return source;
            }
            return GOOGLE;
        }

        public String toEnumString() {
            return super.toString();
        }

        @Override
        public String toString() {
            return this.name;
        }

    }

}
