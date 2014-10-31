package edu.northwestern.cbits.purple_robot_manager.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ProbeValuesSqlHelper extends SQLiteOpenHelper
{
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String DATABASE_NAME = "probe_data.db";
    private static final int DATABASE_VERSION = 1;

    public ProbeValuesSqlHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase arg0)
    {
        // Tables will be constructed dynamically...
    }

    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2)
    {
        // Tables will be constructed dynamically...
    }
}
