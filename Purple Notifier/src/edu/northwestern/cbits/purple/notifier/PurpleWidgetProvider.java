package edu.northwestern.cbits.purple.notifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

public abstract class PurpleWidgetProvider extends AppWidgetProvider 
{
	public void onDeleted (Context context, int[] appWidgetIds)
	{
		
	}
	
	private static InputStream inputStreamForUri(Context context, Uri imageUri) throws MalformedURLException, IOException
	{
		// TODO: Insert caching layer../
		
		InputStream input = null;

		try
		{
			if ("http".equals(imageUri.getScheme().toLowerCase()) || 
				"https".equals(imageUri.getScheme().toLowerCase()))
			{
				HttpURLConnection conn = (HttpURLConnection) (new URL(imageUri.toString())).openConnection();
				
				input = conn.getInputStream();
			}
			else
				input = context.getContentResolver().openInputStream(imageUri);
		}
		catch (NullPointerException e)
		{
			
		}
		
		return input;
	}

	protected static Bitmap bitmapForUri(Context context, Uri imageUri) throws IOException 
	{
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
}
