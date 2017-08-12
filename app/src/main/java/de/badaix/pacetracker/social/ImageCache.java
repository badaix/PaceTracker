package de.badaix.pacetracker.social;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import de.badaix.pacetracker.util.DiskCache;

public class ImageCache extends DiskCache {
    private static ImageCache instance = null;
    private MemCache memCache;

    public static synchronized ImageCache getInstance() {
        if (instance == null) {
            instance = new ImageCache();
        }
        return instance;
    }

    public void cleanUp() {
        if (memCache != null)
            memCache.evictAll();
    }

    public void initCache(File cacheDir, int memSize, int diskSize) {
        memCache = new MemCache(memSize);
        initDiskCache(cacheDir, diskSize);
    }

    public void addBitmapToCache(String id, Bitmap bitmap) {
        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            addDataToCache(id, stream.toByteArray());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }

        memCache.addBitmapToMemoryCache(id, bitmap);
    }

    public Bitmap getBitmapFromMemCache(String name) {
        return memCache.getBitmapFromMemCache(name);
    }

    public Bitmap getBitmapFromCache(String name) {
        Bitmap bitmap = getBitmapFromMemCache(name);
        if (bitmap == null) {
            byte[] bytes = getDataFromDiskCache(name);
            if (bytes != null) {
                // Hint.log(this, "Disk cache hit");
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                memCache.addBitmapToMemoryCache(name, bitmap);
            }
            // } else {
            // Hint.log(this, "Mem cache hit");
        }

        return bitmap;
    }

    class MemCache extends LruCache<String, Bitmap> {
        private final Object cacheLock = new Object();

        public MemCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getRowBytes() * bitmap.getHeight();
        }

        public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
            synchronized (cacheLock) {
                if (getBitmapFromMemCache(key) == null)
                    put(key, bitmap);
            }
        }

        public Bitmap getBitmapFromMemCache(String key) {
            synchronized (cacheLock) {
                return get(key);
            }
        }

    }

}
