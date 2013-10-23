package edu.northwestern.cbits.purple.notifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.DisplayMetrics;

public abstract class PurpleWidgetProvider extends AppWidgetProvider 
{
	public void onDeleted (Context context, int[] appWidgetIds)
	{
		
	}

	public static String createHash(String string)
	{
		if (string == null)
			return null;
		
		String hash = null;
		
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(string.getBytes("UTF-8"));

			hash = (new BigInteger(1, digest)).toString(16);

			while (hash.length() < 32)
			{
				hash = "0" + hash;
			}
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return hash;
	}
	
	private static InputStream inputStreamForUri(Context context, Uri imageUri)
	{
		String hash = PurpleWidgetProvider.createHash(imageUri.toString());
		
		File folder = context.getCacheDir();
		
		File cacheFile = new File(folder, hash);
		
		try 
		{
			return new FileInputStream(cacheFile);
		} 
		catch (FileNotFoundException e) 
		{
			try 
			{
				HttpURLConnection conn = (HttpURLConnection) (new URL(imageUri.toString())).openConnection();

				InputStream input = conn.getInputStream();

				FileOutputStream fout = new FileOutputStream(cacheFile);
				
				byte[] buffer = new byte[4096];
				int read = 0;
				
				while ((read = input.read(buffer, 0, buffer.length)) != -1)
				{
					fout.write(buffer, 0, read);
				}
				
				input.close();
				fout.close();
				
				return PurpleWidgetProvider.inputStreamForUri(context, imageUri);
			} 
			catch (MalformedURLException e1) 
			{
				e1.printStackTrace();
			} 
			catch (IOException e1) 
			{
				e1.printStackTrace();
			}
			
			e.printStackTrace();
		}

		return null;
	}

	protected static Bitmap bitmapForUri(Context context, Uri imageUri) throws IOException 
	{
		if (imageUri == null)
		{
			Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);

			return b;
		}
		
		InputStream input = PurpleWidgetProvider.inputStreamForUri(context, imageUri);
		
		if (input == null)
			return null;

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > 144) ? (originalSize / 144) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

        bitmapOptions.inSampleSize = PurpleWidgetProvider.getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true; 
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional

		input = PurpleWidgetProvider.inputStreamForUri(context,  imageUri);
		
		Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return bitmap;
	}
	
	// http://stackoverflow.com/questions/3879992/get-bitmap-from-an-uri-android

	private static int getPowerOfTwoForSampleRatio(double ratio)
    {
        int k = Integer.highestOneBit((int)Math.floor(ratio));

        if (k == 0)
        	return 1;
        
        return k;
    }

	public static Bitmap badgedBitmapForUri(Context context, Uri imageUri, String badge, double fillRatio, int color) throws IOException 
	{
		badge = badge.trim();
		
		Bitmap b = PurpleWidgetProvider.bitmapForUri(context, imageUri);
		
		Bitmap badged = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
		
		Canvas c = new Canvas(badged);

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(color);
        textPaint.setTextSize(50); 
        textPaint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        
        if (badge.length() > 0)
        {
	        textPaint.getTextBounds(badge, 0, badge.length(), bounds);
	
	        while (bounds.width() < (b.getWidth() * fillRatio) && bounds.height() < (b.getHeight() * fillRatio))
	        {
	        	textPaint.setTextSize(textPaint.getTextSize() + 1);
	
	        	textPaint.getTextBounds(badge, 0, badge.length(), bounds);
	        }
	
	        while (bounds.width() > (b.getWidth() * fillRatio) || bounds.height() > (b.getHeight() * fillRatio))
	        {
	        	textPaint.setTextSize(textPaint.getTextSize() - 1);
	
	        	textPaint.getTextBounds(badge, 0, badge.length(), bounds);
	        }
        }

    	c.drawBitmap(b, 0, 0, textPaint);
        
        if (badge.length() > 0)
            c.drawText(badge, b.getWidth() / 2, (b.getHeight() / 2) + (bounds.height() / 2), textPaint);

		return badged;
	}

	public static Bitmap bitmapForText(Context context, String text, int width, int height, String color) 
	{
		return PurpleWidgetProvider.bitmapForText(context, text, width, height, color, false, true);
	}

	public static Bitmap bitmapForText(Context context, String text, int width, int height, String color, boolean verticalCenter, boolean horizontalCenter) 
	{
		text = text.trim();
		
		if (text.length() == 0)
			return null;
		
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();

		float floatWidth = width * metrics.density;
		float floatHeight = height * metrics.density;

		width = (int) floatWidth;
		height = (int) floatHeight;
		
		if (width < 1 || height < 1)
			return null;
		
		Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		Canvas c = new Canvas(b);

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.parseColor(color));
        textPaint.setTextSize(128); 
        textPaint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        
        float drawHeight = height;
        
        while (bounds.width() < b.getWidth() && bounds.height() <  drawHeight)
        {
        	textPaint.setTextSize(textPaint.getTextSize() + 1);

        	textPaint.getTextBounds(text, 0, text.length(), bounds);

            if (verticalCenter == false)
            	drawHeight = height - textPaint.descent();
        }

        while ((bounds.width() > b.getWidth() || bounds.height() > drawHeight) && textPaint.getTextSize() > 0)
        {
        	textPaint.setTextSize(textPaint.getTextSize() - 1);

        	textPaint.getTextBounds(text, 0, text.length(), bounds);
        
            if (verticalCenter == false)
            	drawHeight = height - textPaint.descent();
        }

        if (verticalCenter == false)
        	c.drawText(text, b.getWidth() / 2, (b.getHeight() / 2) + (bounds.height() / 2) - (textPaint.descent() / 2), textPaint);
        else
        	c.drawText(text, b.getWidth() / 2, (b.getHeight() / 2) + (bounds.height() / 2), textPaint);
		
		return b;
	}
}
