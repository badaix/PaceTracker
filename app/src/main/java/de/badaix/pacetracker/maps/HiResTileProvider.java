package de.badaix.pacetracker.maps;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;

import de.badaix.pacetracker.util.Hint;

public class HiResTileProvider implements TileProvider {

    protected TileProvider tileProvider;
    protected BaseTileSource tileSource;

    public HiResTileProvider(TileProvider tileProvider, BaseTileSource tileSource) {
        this.tileProvider = tileProvider;
        this.tileSource = tileSource;
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        String tileName = "HiRes/" + tileSource.getName() + "/" + zoom + "/" + x + "/" + y;
        byte[] bytes = null;// bos.toByteArray();
        bytes = TileCache.getInstance().getTileFromDiskCache(tileName);
        if (bytes != null) {
            Hint.log(this, "File found in cache: " + tileName);
            return new Tile(2 * tileSource.getTileSizePixels(), 2 * tileSource.getTileSizePixels(), bytes);
        }

        Hint.log(this, "File does not exist: " + tileName);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inPurgeable = true;

        Tile tile = tileProvider.getTile(2 * x, 2 * y, zoom + 1);
        if (tile == null)
            return tileProvider.getTile(x, y, zoom);
        Bitmap bmp = BitmapFactory.decodeByteArray(tile.data, 0, tile.data.length, bmOptions);

        int height = tile.height * 3 / 4;
        int width = tile.width * 3 / 4;

        Bitmap hiBmp = Bitmap.createBitmap(2 * width, 2 * height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(hiBmp);
        canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, width, height, true), 0, 0, null);

        tile = tileProvider.getTile(2 * x + 1, 2 * y, zoom + 1);
        if (tile == null)
            return tileProvider.getTile(x, y, zoom);
        bmp = BitmapFactory.decodeByteArray(tile.data, 0, tile.data.length, bmOptions);
        canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, width, height, true), width, 0, null);

        tile = tileProvider.getTile(2 * x, 2 * y + 1, zoom + 1);
        if (tile == null)
            return tileProvider.getTile(x, y, zoom);
        bmp = BitmapFactory.decodeByteArray(tile.data, 0, tile.data.length, bmOptions);
        canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, width, height, true), 0, height, null);

        tile = tileProvider.getTile(2 * x + 1, 2 * y + 1, zoom + 1);
        if (tile == null)
            return tileProvider.getTile(x, y, zoom);
        bmp = BitmapFactory.decodeByteArray(tile.data, 0, tile.data.length, bmOptions);
        canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, width, height, true), width, height, null);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        hiBmp.compress(CompressFormat.PNG, 100, bos);

        bytes = bos.toByteArray();
        TileCache.getInstance().addTileToCache(tileName, bytes);

        return new Tile(2 * width, 2 * height, bytes);
    }
}
