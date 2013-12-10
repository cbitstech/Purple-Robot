package edu.northwestern.cbits.purplescope;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class PerformanceContentProvider extends ContentProvider 
{
	public static final String AUTHORITY = "edu.northwestern.cbits.purplescope.content";

	private static final int PERFORMANCE_VALUE_LIST = 1;
	private static final int PERFORMANCE_VALUE = 2;

	private static final String PERFORMANCE_VALUES_TABLE = "performance_values";

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE = "performance_values.db";

	private UriMatcher _uriMatcher = null;
	private SQLiteOpenHelper _openHelper = null;
	
	public int delete(Uri uri, String selection, String[] selectionArgs) 
	{
		SQLiteDatabase db = this._openHelper.getWritableDatabase();

		int result = 0;
		
		switch(this._uriMatcher.match(uri))
		{
			case PerformanceContentProvider.PERFORMANCE_VALUE_LIST:
				result = db.delete(PerformanceContentProvider.PERFORMANCE_VALUES_TABLE, selection, selectionArgs);
				break;
			case PerformanceContentProvider.PERFORMANCE_VALUE:
				result = db.delete(PerformanceContentProvider.PERFORMANCE_VALUES_TABLE, selection, selectionArgs);
				break;
		}

		return result;
	}

	public String getType(Uri uri) 
	{
		switch(this._uriMatcher.match(uri))
		{
			case PerformanceContentProvider.PERFORMANCE_VALUE_LIST:
				return "vnd.android.cursor.dir/vnd.edu.northwestern.cbits.purplescope.performance_value";
			case PerformanceContentProvider.PERFORMANCE_VALUE:
				return "vnd.android.cursor.item/vnd.edu.northwestern.cbits.purplescope.performance_value";
		}
		
		return null;
	}

	public Uri insert(Uri uri, ContentValues values) 
	{
		SQLiteDatabase db = this._openHelper.getWritableDatabase();

		switch(this._uriMatcher.match(uri))
		{
			case PerformanceContentProvider.PERFORMANCE_VALUE_LIST:
				long id = db.insert(PerformanceContentProvider.PERFORMANCE_VALUES_TABLE, null, values);
				
				return Uri.withAppendedPath(uri, "" + id);
		}

		return null;
	}

	public boolean onCreate() 
	{
		this._uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		this._uriMatcher.addURI(PerformanceContentProvider.AUTHORITY, PerformanceContentProvider.PERFORMANCE_VALUES_TABLE, PerformanceContentProvider.PERFORMANCE_VALUE_LIST);
		this._uriMatcher.addURI(PerformanceContentProvider.AUTHORITY, PerformanceContentProvider.PERFORMANCE_VALUES_TABLE + "/#", PerformanceContentProvider.PERFORMANCE_VALUE);

		final PerformanceContentProvider me = this;

		this._openHelper = new SQLiteOpenHelper(this.getContext(), PerformanceContentProvider.DATABASE, null, PerformanceContentProvider.DATABASE_VERSION)
		{
			public void onCreate(SQLiteDatabase db) 
			{
				this.onUpgrade(db, 0, PerformanceContentProvider.DATABASE_VERSION);
			}

			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
			{
				switch (oldVersion)
				{
					case 0:
						db.execSQL(me.getContext().getString(R.string.create_performance_values_table));
					default:
						break;
				}
			}
		};

		return true;

	}

	public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) 
	{
		SQLiteDatabase db = this._openHelper.getWritableDatabase();
		
		switch(this._uriMatcher.match(uri))
		{
			case PerformanceContentProvider.PERFORMANCE_VALUE_LIST:
				return db.query(PerformanceContentProvider.PERFORMANCE_VALUES_TABLE, projection, selection, args, null, null, sort);
		}
		
		return null;
	}

	public int update(Uri uri, ContentValues values, String selection, String[] args) 
	{
		SQLiteDatabase db = this._openHelper.getWritableDatabase();

		switch(this._uriMatcher.match(uri))
		{
			case PerformanceContentProvider.PERFORMANCE_VALUE_LIST:
				return db.update(PerformanceContentProvider.PERFORMANCE_VALUES_TABLE, values, selection, args);
		}
		
		return 0;
	}
}
