package edu.northwestern.cbits.purple_robot_manager.util;

import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.util.LongSparseArray;

import java.util.ArrayList;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class WakeLockManager
{
    private static final String TIMESTAMP_KEY = "TIMESTAMP";
    private static final String LOCK_TYPE_KEY = "LOCK_TYPE";
    private static final String LOCK_IS_HELD_KEY = "IS_HELD";
    private static final String LOCK_DESCRIPTION_KEY = "DESCRIPTION";
    private static final String LOCK_TAG = "LOCK_TAG";

    private final Context _context;
    private LongSparseArray<PowerManager.WakeLock> _partialLocks = new LongSparseArray<PowerManager.WakeLock>();
    private LongSparseArray<PowerManager.WakeLock> _dimLocks = new LongSparseArray<PowerManager.WakeLock>();
    private LongSparseArray<PowerManager.WakeLock> _brightLocks = new LongSparseArray<PowerManager.WakeLock>();
    private LongSparseArray<PowerManager.WakeLock> _fullLocks = new LongSparseArray<PowerManager.WakeLock>();

    private LongSparseArray<String> _tags = new LongSparseArray<String>();

    private static WakeLockManager _instance = null;

    public static WakeLockManager getInstance(Context context)
    {
        if (WakeLockManager._instance != null)
            return WakeLockManager._instance;

        WakeLockManager._instance = new WakeLockManager(context.getApplicationContext());

        return WakeLockManager._instance;
    }

    protected WakeLockManager(Context context)
    {
        this._context = context;
    }

    public PowerManager.WakeLock requestWakeLock(int lockType, String tag)
    {
        tag = tag.replace("edu.northwestern.cbits.purple_robot_manager.", "");

        PowerManager power = (PowerManager) this._context.getSystemService(Context.POWER_SERVICE);

        PowerManager.WakeLock lock = power.newWakeLock(lockType, tag);

        long now = System.currentTimeMillis();

        if((lockType & PowerManager.PARTIAL_WAKE_LOCK) == PowerManager.PARTIAL_WAKE_LOCK)
            this._partialLocks.append(now, lock);
        else if((lockType & PowerManager.SCREEN_DIM_WAKE_LOCK) == PowerManager.SCREEN_DIM_WAKE_LOCK)
            this._dimLocks.append(now, lock);
        else if((lockType & PowerManager.SCREEN_BRIGHT_WAKE_LOCK) == PowerManager.SCREEN_BRIGHT_WAKE_LOCK)
            this._brightLocks.append(now, lock);
       else  if((lockType & PowerManager.FULL_WAKE_LOCK) == PowerManager.FULL_WAKE_LOCK)
            this._fullLocks.append(now, lock);

        this._tags.put(now, tag);

        lock.acquire();

        return lock;
    }

    public void releaseWakeLock(PowerManager.WakeLock lock)
    {
        LongSparseArray[] allLocks = { this._partialLocks, this._dimLocks, this._brightLocks, this._fullLocks };

        for (LongSparseArray<PowerManager.WakeLock> locks : allLocks)
        {
            int index = locks.indexOfValue(lock);

            if (index >= 0)
            {
                long timestamp = locks.keyAt(index);

                this._tags.remove(timestamp);

                locks.removeAt(index);
            }
        }

        lock.release();
    }

    private ArrayList<Bundle> locks(LongSparseArray<PowerManager.WakeLock> locks, String lockType) {
        ArrayList<Bundle> bundles = new ArrayList<>();

        for (int i = 0; i < locks.size(); i++) {
            long timestamp = locks.keyAt(i);

            try {
                PowerManager.WakeLock lock = locks.valueAt(i);

                Bundle lockBundle = new Bundle();
                lockBundle.putLong(WakeLockManager.TIMESTAMP_KEY, timestamp);
                lockBundle.putString(WakeLockManager.LOCK_TYPE_KEY, lockType);
                lockBundle.putString(WakeLockManager.LOCK_DESCRIPTION_KEY, lock.toString());
                lockBundle.putBoolean(WakeLockManager.LOCK_IS_HELD_KEY, lock.isHeld());
                lockBundle.putString(WakeLockManager.LOCK_TAG, this._tags.get(timestamp));

                bundles.add(lockBundle);
            }
            catch (ClassCastException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }

        return bundles;
    }

    public ArrayList<Bundle> partialLocks() {
        return this.locks(this._partialLocks, "PARTIAL");
    }

    public ArrayList<Bundle> dimLocks() {
        return this.locks(this._dimLocks, "SCREEN_DIM");
    }

    public ArrayList<Bundle> brightLocks() {
        return this.locks(this._brightLocks, "SCREEN_BRIGHT");
    }

    public ArrayList<Bundle> fullLocks() {
        return this.locks(this._fullLocks, "FULL");
    }
}
