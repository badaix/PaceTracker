package de.badaix.pacetracker.util;

import android.os.StatFs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DiskCache {

    private final Object mDiskCacheLock = new Object();
    private ICSDiskLruCache mDiskLruCache;
    private boolean mDiskCacheStarting = true;
    // private Context context;
    private File cacheDir;
    private int cacheSize;

    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     * @return The space available in bytes
     */
    public static long getUsableSpace(File path) {
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable
     * for using as a disk filename.
     */
    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Clears both the memory and disk cache associated with this ImageCache
     * object. Note that this includes disk access so this should not be
     * executed on the main/UI thread.
     */
    public void clearCache() {
        synchronized (mDiskCacheLock) {
            mDiskCacheStarting = true;
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                try {
                    mDiskLruCache.delete();
                    Hint.log(this, "Disk cache cleared");
                } catch (IOException e) {
                    Hint.log(this, "clearCache - " + e);
                }
                mDiskLruCache = null;
                initDiskCache(cacheDir, cacheSize);
            }
        }
    }

    public void initDiskCache(File cacheDir, int cacheSize) {
        // this.context = context;
        this.cacheSize = cacheSize;
        this.cacheDir = cacheDir;
        // Set up disk cache
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                if (getUsableSpace(cacheDir) > cacheSize) {
                    try {
                        mDiskLruCache = ICSDiskLruCache.open(cacheDir, 1, 1, cacheSize);
                        Hint.log(this, "Disk cache initialized");
                    } catch (final IOException e) {
                        Hint.log(this, "initDiskCache - " + e);
                    }
                }
            }
            mDiskCacheStarting = false;
            mDiskCacheLock.notifyAll();
        }
    }

    /**
     * Adds a bitmap to both memory and disk cache.
     *
     * @param data   Unique identifier for the bitmap to store
     * @param bitmap The bitmap to store
     */
    protected void addDataToCache(String id, byte[] data) {
        if (id == null || data == null) {
            return;
        }

        synchronized (mDiskCacheLock) {
            // Add to disk cache
            if (mDiskLruCache != null) {
                final String key = hashKeyForDisk(id);
                OutputStream out = null;
                try {
                    ICSDiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot == null) {
                        final ICSDiskLruCache.Editor editor = mDiskLruCache.edit(key);
                        if (editor != null) {
                            out = editor.newOutputStream(0);
                            out.write(data);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream(0).close();
                    }
                } catch (final IOException e) {
                    Hint.log(this, "addBitmapToCache - " + e);
                } catch (Exception e) {
                    Hint.log(this, "addBitmapToCache - " + e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    /**
     * Get from disk cache.
     *
     * @param data Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    protected byte[] getDataFromDiskCache(String id) {
        final String key = hashKeyForDisk(id);
        synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                }
            }
            if (mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    final ICSDiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot != null) {
                        inputStream = snapshot.getInputStream(0);
                        if (inputStream != null) {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte data[] = new byte[1024];
                            int count;
                            while ((count = inputStream.read(data)) != -1) {
                                bos.write(data, 0, count);
                            }
                            return bos.toByteArray();
                        }
                    }
                } catch (final IOException e) {
                    Hint.log(this, "getBitmapFromDiskCache - " + e);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        }
    }
}
