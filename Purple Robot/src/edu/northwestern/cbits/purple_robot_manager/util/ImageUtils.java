package edu.northwestern.cbits.purple_robot_manager.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.StatFs;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.net.ssl.SSLException;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

/**
 * Created by Chris Karr on 6/2/2015.
 */
public class ImageUtils {

    private static long _lastSpaceChecked = -1;
    private static long _lastCacheAccess = -1;
    private static long _lastSpaceFree = -1;
    private static File _cacheDir = null;

    public static Uri fetchResizedImageSync(final Context context, final Uri uri, final int width, final int height, final boolean fillSpace)
    {
        if (uri == null)
            return null;

        Uri resized = Uri.parse(uri.toString() + "/" + height + "-" + width);

        String hashString = EncryptionManager.getInstance().createHash(context, resized.toString());

        final File cachedFile = new File(ImageUtils.fetchCacheDir(context), hashString);

        if (cachedFile.exists())
        {
            cachedFile.setLastModified(System.currentTimeMillis());
            return Uri.fromFile(cachedFile);
        }
        else
        {
            Uri cached = ImageUtils.fetchCachedUri(context, uri, false, null);

            if (cached != null)
            {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                opts.inInputShareable = true;
                opts.inPurgeable = true;
                BitmapFactory.decodeFile(cached.getPath(), opts);

                int scale = 1;

                int thisWidth = width;
                int thisHeight = height;

                while ((thisWidth > 0 && thisHeight > 0) && (opts.outWidth > thisWidth || opts.outHeight > thisHeight))
                {
                    scale += 1;

                    thisWidth = thisWidth * 2;
                    thisHeight = thisHeight * 2;
                }

                if (scale < 1)
                    scale = 1;

                if (fillSpace)
                {
                    // CJK: Memory fix?
                    scale += 1;
                }

                opts = new BitmapFactory.Options();
                opts.inDither = false;
                opts.inPurgeable = true;
                opts.inInputShareable = true;
                opts.inSampleSize = scale;

                try
                {
                    Bitmap bitmap = BitmapFactory.decodeFile(cached.getPath(), opts);

                    if (bitmap != null)
                    {
                        cachedFile.createNewFile();

                        FileOutputStream fout = new FileOutputStream(cachedFile);

                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fout);

                        fout.close();

                        bitmap.recycle();

                        return Uri.fromFile(cachedFile);
                    }
                }
                catch (OutOfMemoryError e)
                {
                    System.gc();

                }
                catch (FileNotFoundException e)
                {
                    LogManager.getInstance(context).logException(e);

                }
                catch (IOException e)
                {
                    LogManager.getInstance(context).logException(e);
                }
            }
        }

        return null;
    }

    public static Uri fetchCachedUri(final Context context, final Uri uri, boolean async, final Runnable next)
    {
        if (uri == null)
            return null;

        final String hashString = EncryptionManager.getInstance().createHash(context, uri.toString());

        final File cacheDir = ImageUtils.fetchCacheDir(context);
        final File cachedFile = new File(cacheDir, hashString);

        if (cachedFile.exists() && cachedFile.length() > 0)
        {
            cachedFile.setLastModified(System.currentTimeMillis());
            return Uri.fromFile(cachedFile);
        }

        Runnable r = new Runnable()
        {
            public void run()
            {
                File tempFile = null;

                try
                {
                    long freeSpace = ImageUtils.cacheSpace(context);

                    if (freeSpace < 4096 * 4096)
                    {
                        ImageUtils.cleanCache(context);

                        freeSpace = ImageUtils.cacheSpace(context);

                        if (freeSpace < 4096 * 4096)
                            ImageUtils.clearCache(context);
                    }

                    freeSpace = ImageUtils.cacheSpace(context);

                    if (freeSpace > 1024 * 1024)
                    {
                        URL u = new URL(uri.toString());

                        URLConnection conn = u.openConnection();
                        InputStream in = conn.getInputStream();

                        tempFile = File.createTempFile(hashString, "tmp", cacheDir);

                        FileOutputStream out = new FileOutputStream(tempFile);

                        byte[] buffer = new byte[8192];
                        int read = 0;

                        while ((read = in.read(buffer, 0, buffer.length)) != -1)
                        {
                            out.write(buffer, 0, read);
                        }

                        out.close();

                        tempFile.renameTo(cachedFile);
                    }
                }
                catch (SocketTimeoutException e)
                {
                    LogManager.getInstance(context).logException(e);
                }
                catch (SocketException e)
                {
                    LogManager.getInstance(context).logException(e);
                }
                catch (SSLException e)
                {
                    LogManager.getInstance(context).logException(e);
                }
                catch (UnknownHostException e)
                {
                    LogManager.getInstance(context).logException(e);
                }
                catch (EOFException e)
                {
                    LogManager.getInstance(context).logException(e);
                }
                catch (FileNotFoundException e)
                {
                    LogManager.getInstance(context).logException(e);
                }
                catch (IOException e)
                {
                    LogManager.getInstance(context).logException(e);
                }
                finally
                {
                    if (tempFile != null && tempFile.exists())
                        tempFile.delete();

                    if (next != null)
                        next.run();
                }
            }
        };

        if (async)
        {
            Thread t = new Thread(r);
            t.start();

            return null;
        }

        r.run();

        return ImageUtils.fetchCachedUri(context, uri, true, next);
    }

    private static File fetchCacheDir(Context context)
    {
        long now = System.currentTimeMillis();

        if (ImageUtils._cacheDir == null || now - ImageUtils._lastCacheAccess > 1000)
        {
            File cache = context.getCacheDir();

            if (cache.exists() == false)
                cache.mkdirs();

            ImageUtils._cacheDir = cache;
        }

        ImageUtils._lastCacheAccess = now;

        return ImageUtils._cacheDir;
    }

    @SuppressWarnings("deprecation")
    public static synchronized long cacheSpace(Context context)
    {
        long now = System.currentTimeMillis();

        if (now - ImageUtils._lastSpaceChecked < 60000)
            return ImageUtils._lastSpaceFree;

        ImageUtils._lastSpaceChecked = now;

        File cache = context.getCacheDir();

        if (cache.exists() == false)
            cache.mkdirs();

        StatFs stat = new StatFs(cache.getAbsolutePath());

        ImageUtils._lastSpaceFree = ((long) stat.getBlockSize()) * ((long) stat.getAvailableBlocks());

        return ImageUtils._lastSpaceFree;
    }

    public static synchronized void cleanCache(Context context)
    {
        File cache = context.getCacheDir();

        long cacheSize = 1024;

        cacheSize = cacheSize * 1024 * 1024;

        if (cache.exists() == false)
            cache.mkdirs();

        synchronized(context.getApplicationContext())
        {
            List<File> files = new ArrayList<File>();

            for (File f : cache.listFiles())
            {
                files.add(f);
            }

            try
            {
                Collections.sort(files, new Comparator<File>() {
                    public int compare(File first, File second) {
                        if (first.lastModified() < second.lastModified())
                            return 1;
                        else if (first.lastModified() > second.lastModified())
                            return -1;

                        return first.getName().compareTo(second.getName());
                    }
                });
            }
            catch (IllegalArgumentException e)
            {
                LogManager.getInstance(context).logException(e);
            }

            List<File> toRemove = new ArrayList<File>();

            long totalCached = 0;

            for (File f : files)
            {
                if (totalCached < cacheSize)
                    totalCached += f.length();
                else
                    toRemove.add(f);
            }

            for (File f : toRemove)
            {
                f.delete();
            }
        }

        ImageUtils._lastSpaceChecked = 0;
    }

    public static synchronized void clearCache(Context context)
    {
        File cache = context.getCacheDir();

        if (cache.exists() == false)
            cache.mkdirs();

        List<File> files = new ArrayList<File>();

        for (File f : cache.listFiles())
        {
            files.add(f);
        }

        for (File f : files)
        {
            f.delete();
        }

        ImageUtils._lastSpaceChecked = 0;
    }
}
