package edu.northwestern.cbits.purple_robot_manager.logging;

import java.io.File;

import edu.northwestern.cbits.purple_robot_manager.R;

import android.content.Context;
import android.os.StatFs;

public class DiskSpaceCheck extends SanityCheck
{
    private static long WARNING_SIZE = 10485760;
    private static long ERROR_SIZE = 4194304;

    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_disk_space);
    }

    @SuppressWarnings("deprecation")
    public void runCheck(Context context)
    {
        File cache = context.getCacheDir();
        File externalCache = context.getExternalCacheDir();

        this._errorMessage = null;
        this._errorLevel = SanityCheck.OK;

        if (cache != null || externalCache != null)
        {
            if (cache != null)
            {
                StatFs stat = new StatFs(cache.getAbsolutePath());

                long free = ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize());

                if (free < DiskSpaceCheck.ERROR_SIZE)
                {
                    this._errorLevel = SanityCheck.ERROR;
                    this._errorMessage = context.getString(R.string.name_sanity_disk_space_local_error);
                }
                else if (free < DiskSpaceCheck.WARNING_SIZE)
                {
                    this._errorLevel = SanityCheck.WARNING;
                    this._errorMessage = context.getString(R.string.name_sanity_disk_space_local_warning);
                }
            }

            if (this._errorMessage == null && externalCache != null)
            {
                try
                {
                    StatFs stat = new StatFs(externalCache.getAbsolutePath());

                    long free = ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize());

                    if (free < DiskSpaceCheck.ERROR_SIZE)
                    {
                        this._errorLevel = SanityCheck.ERROR;
                        this._errorMessage = context.getString(R.string.name_sanity_disk_space_external_error);
                    }
                    else if (free < DiskSpaceCheck.WARNING_SIZE)
                    {
                        this._errorLevel = SanityCheck.WARNING;
                        this._errorMessage = context.getString(R.string.name_sanity_disk_space_external_warning);
                    }
                }
                catch (IllegalArgumentException e)
                {
                    if (this._errorLevel == SanityCheck.OK)
                    {
                        this._errorLevel = SanityCheck.WARNING;
                        this._errorMessage = context
                                .getString(R.string.name_sanity_disk_space_external_unknown_warning);
                    }
                }
            }
        }
    }
}
