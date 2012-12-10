package edu.northwestern.cbits.purple_robot_manager.db;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class ProbeValuesProvider
{
	public static final String INTEGER_TYPE = "integer";
	public static final String REAL_TYPE = "real";
	public static final String TEXT_TYPE = "text";

	public static final String TIMESTAMP = "timestamp";
	private static final String ID = "_id";

	private SQLiteDatabase _database = null;
	private ProbeValuesSqlHelper _dbHelper = null;

	private HashMap<String, Long> _lastSaves = new HashMap<String, Long>();

	private long _lastCleanup = 0;

	private static ProbeValuesProvider _instance = null;

	public static ProbeValuesProvider getProvider(Context context)
	{
		if (ProbeValuesProvider._instance == null)
			ProbeValuesProvider._instance = new ProbeValuesProvider(context.getApplicationContext());

		return ProbeValuesProvider._instance;
	}

	public ProbeValuesProvider(Context context)
	{
		this._dbHelper = new ProbeValuesSqlHelper(context);

		try
		{
			this._database = this._dbHelper.getWritableDatabase();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	public void close()
	{
		this._dbHelper.close();
	}

	private String tableName(String name, Map<String, String> schema)
	{
		String tableName = name;

		ArrayList<String> columns = new ArrayList<String>(schema.keySet());
		Collections.sort(columns);

		for (String key : columns)
		{
			tableName += (key + schema.get(key));
		}

		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(tableName.getBytes("UTF-8"));

			tableName = "table_" + (new BigInteger(1, digest)).toString(16);
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}

		return tableName;
	}

	private boolean tableExists(String tableName)
	{
		Cursor c = null;

		boolean tableExists = false;

		try
		{
		    c = this._database.query(tableName, null, null, null, null, null, null);

		    tableExists = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (c != null)
				c.close();
		}

		return tableExists;
	}

	private boolean isValidColumn(String key)
	{
		// TODO: Add more checks...

		return true;
	}

	private boolean createTable(String name, Map<String, String> schema)
	{
		String createSql = "create table " + name + " (" + ProbeValuesProvider.ID + " integer primary key autoincrement";

		createSql += (", " + ProbeValuesProvider.TIMESTAMP + " real");

		for (String key : schema.keySet())
		{
			if (this.isValidColumn(key))
			{
				String dbType = null;

				String type = schema.get(key);

				if (ProbeValuesProvider.REAL_TYPE.equals(type))
					dbType = ProbeValuesProvider.REAL_TYPE;
				else if (ProbeValuesProvider.INTEGER_TYPE.equals(type))
					dbType = ProbeValuesProvider.INTEGER_TYPE;
				if (ProbeValuesProvider.TEXT_TYPE.equals(type))
					dbType = ProbeValuesProvider.TEXT_TYPE;

				if (dbType != null)
					createSql += (", " + key + " " + dbType);
			}
		}

		createSql += ");";

		this._database.execSQL(createSql);

		return false;
	}

	public boolean insertValue(String name, Map<String, String> schema, Map<String, Object> values)
	{
		long now = System.currentTimeMillis();

		Long lastSave = this._lastSaves.get(name);

		if (lastSave == null)
			lastSave = Long.valueOf(0);

		if (now - lastSave.longValue() < 500)
			return false;

		this._lastSaves.put(name, Long.valueOf(now));

		if (now - this._lastCleanup > 300000) // FLush old entries every 5 minutes...
			this.cleanup();

		String localName = this.tableName(name, schema);

		if (this.tableExists(localName) == false)
			this.createTable(localName, schema);

		ContentValues toInsert = new ContentValues();

		for (String key : schema.keySet())
		{
			String type = schema.get(key);

			if (ProbeValuesProvider.REAL_TYPE.equals(type))
			{
				Double d = (Double) values.get(key);

				toInsert.put(key, d);
			}
			else if (ProbeValuesProvider.INTEGER_TYPE.equals(type))
			{
				Integer i = (Integer) values.get(key);

				toInsert.put(key, i);
			}
			else if (ProbeValuesProvider.TEXT_TYPE.equals(type))
				toInsert.put(key, values.get(key).toString());
		}

		toInsert.put(ProbeValuesProvider.TIMESTAMP, (Double) values.get(ProbeValuesProvider.TIMESTAMP));

		return (this._database.insert(localName, null, toInsert) != -1);
	}


	private void cleanup()
	{
		this._lastCleanup = System.currentTimeMillis();

		String tableSelect = "select name from sqlite_master where type='table';";

		Cursor c = this._database.rawQuery(tableSelect, null);

		while (c.moveToNext())
		{
//			String tableName = c.getString(c.getColumnIndex("name"));

			try
			{
				// TODO: calculate timestamp or get params from probe...

//				int deleted = this._database.delete(tableName, ProbeValuesProvider.TIMESTAMP + " < 1.0", null);

//				Log.e("PRM", "DELETED " + deleted);
			}
			catch (SQLException e)
			{

			}
		}

		c.close();
	}

	public Cursor retrieveValues(String name, Map<String, String> schema)
	{
		String localName = this.tableName(name, schema);

		if (this.tableExists(localName) == false)
			this.createTable(localName, schema);

		Cursor c = null;

		try
		{
		    c = this._database.query(localName, null, null, null, null, null, ProbeValuesProvider.TIMESTAMP);

		    return c;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}
}
