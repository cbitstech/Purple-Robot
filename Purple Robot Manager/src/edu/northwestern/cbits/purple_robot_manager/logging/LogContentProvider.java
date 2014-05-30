package edu.northwestern.cbits.purple_robot_manager.logging;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class LogContentProvider extends ContentProvider 
{
	private static final int DATABASE_VERSION = 1;

	private static final String AUTHORITY = "edu.northwestern.cbits.purple_robot.events";

	private static final String APP_EVENTS_TABLE = "app_events";

	private static final int APP_EVENTS = 1;

	public static final Uri APP_EVENTS_URI = Uri.parse("content://" + AUTHORITY + "/" + APP_EVENTS_TABLE);

	public static final String APP_EVENT_RECORDED = "recorded";
	public static final String APP_EVENT_NAME = "name";
	public static final String APP_EVENT_PAYLOAD = "payload";
	public static final String APP_EVENT_TRANSMITTED = "transmitted";
	public static final String APP_EVENT_ID = "_id";

	private UriMatcher _matcher = new UriMatcher(UriMatcher.NO_MATCH);

    public LogContentProvider()
    {
    	super();
        this._matcher.addURI(AUTHORITY, APP_EVENTS_TABLE, APP_EVENTS);
    }

	private SQLiteDatabase _db = null;
	
	public boolean onCreate() 
	{
        final Context context = this.getContext().getApplicationContext();
        
        SQLiteOpenHelper helper = new SQLiteOpenHelper(context, "event_log.db", null, LogContentProvider.DATABASE_VERSION)
        {
            public void onCreate(SQLiteDatabase db) 
            {
	            db.execSQL(context.getString(R.string.db_create_events_log_table));

	            this.onUpgrade(db, 0, LogContentProvider.DATABASE_VERSION);
            }

            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
            {
            	switch (oldVersion)
            	{
	                case 0:

	                default:
                        break;
            	}
            }
        };
        
        this._db   = helper.getWritableDatabase();

        return true;
	}

	public int delete(Uri uri, String selection, String[] selectionArgs) 
	{
        switch(this._matcher.match(uri))
        {
	        case LogContentProvider.APP_EVENTS:
	            return this._db.delete(LogContentProvider.APP_EVENTS_TABLE, selection, selectionArgs);
        }

        return 0;
	}

	@Override
	public String getType(Uri uri) 
	{
        switch(this._matcher.match(uri))
        {
	        case LogContentProvider.APP_EVENTS:
	        	return "vnd.android.cursor.dir/" + AUTHORITY + ".app_event";
        }
        
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) 
	{
        switch(this._matcher.match(uri))
        {
	        case LogContentProvider.APP_EVENTS:
	            long id = this._db.insert(LogContentProvider.APP_EVENTS_TABLE, null, values);
	            
	            return Uri.withAppendedPath(uri, "" + id);
        }

		return null;
	}


	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 
	{
        switch(this._matcher.match(uri))
        {
	        case LogContentProvider.APP_EVENTS:
	            return this._db.query(LogContentProvider.APP_EVENTS_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
        }
        
		return null;
	}

	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) 
	{
        switch(this._matcher.match(uri))
        {
	        case LogContentProvider.APP_EVENTS:
	            return this._db.update(LogContentProvider.APP_EVENTS_TABLE, values, selection, selectionArgs);
        }

        return 0;
	}
}
