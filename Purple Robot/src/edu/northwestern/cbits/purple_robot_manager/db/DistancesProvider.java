package edu.northwestern.cbits.purple_robot_manager.db;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class DistancesProvider extends ContentProvider
{
    public static final Uri CONTENT_URI = Uri
            .parse("content://edu.northwestern.cbits.purple_robot_manager.distances/distances");

    private final static String DB_NAME = "distance_history";

    private static final String TABLE_NAME = "distances";
    public final static String NAME = "name";
    public final static String DISTANCE = "distance";
    public final static String TIMESTAMP = "timestamp";

    private static final int DATABASE_VERSION = 1;

    private SQLiteOpenHelper _openHelper = null;

    public int delete(Uri arg0, String selection, String[] selectionArgs)
    {
        SQLiteDatabase db = this._openHelper.getWritableDatabase();

        return db.delete(DistancesProvider.DB_NAME, selection, selectionArgs);
    }

    public String getType(Uri arg0)
    {
        return "vnd.android.cursor.item/vnd.purple-bot.distance";
    }

    public Uri insert(Uri uri, ContentValues values)
    {
        SQLiteDatabase db = this._openHelper.getWritableDatabase();

        db.insert(DistancesProvider.DB_NAME, null, values);

        return uri;
    }

    public boolean onCreate()
    {
        final DistancesProvider me = this;

        this._openHelper = new SQLiteOpenHelper(this.getContext(), DistancesProvider.TABLE_NAME, null,
                DistancesProvider.DATABASE_VERSION)
        {
            public void onCreate(SQLiteDatabase db)
            {
                Context context = me.getContext().getApplicationContext();

                db.execSQL(context.getString(R.string.create_distances_table));

                this.onUpgrade(db, 0, DistancesProvider.DATABASE_VERSION);
            }

            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
            {
                switch (oldVersion)
                {
                case 1:
                case 2:
                    // db.execSQL(context.getString(R.string.upgrade_issues_add_reprint));
                default:
                    break;
                }
            }
        };

        return false;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy)
    {
        SQLiteDatabase db = this._openHelper.getReadableDatabase();

        return db.query(DistancesProvider.DB_NAME, projection, selection, selectionArgs, null, null, orderBy);
    }

    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3)
    {
        return 0;
    }
}
