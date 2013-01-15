package edu.northwestern.cbits.purple_robot_manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.NullCipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Base64;
import edu.emory.mathcs.backport.java.util.Arrays;

public class EncryptionManager 
{
	private static final String CRYPTO_ALGORITHM = "AES/CBC/PKCS5Padding";

	private static final EncryptionManager _instance = new EncryptionManager();
    
    private EncryptionManager() 
    {
        if (EncryptionManager._instance != null)
            throw new IllegalStateException("Already instantiated");
    }

    public static EncryptionManager getInstance() 
    {
        return EncryptionManager._instance;
    }
    
	protected static SharedPreferences getPreferences(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
	}
	
	public Cipher encryptCipher(Context context, boolean functional)
	{
		Cipher cipher = new NullCipher();
		
		if (functional)
		{
			try
			{
				SecretKeySpec secretKey = this.keyForCipher(context, EncryptionManager.CRYPTO_ALGORITHM);

				IvParameterSpec ivParameterSpec = new IvParameterSpec(this.getIVBytes());

				cipher = Cipher.getInstance(EncryptionManager.CRYPTO_ALGORITHM);
				cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException(e);
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new RuntimeException(e);
			}
			catch (NoSuchPaddingException e)
			{
				throw new RuntimeException(e);
			}
			catch (InvalidKeyException e)
			{
				throw new RuntimeException(e);
			}
			catch (InvalidAlgorithmParameterException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return cipher;
	}

	public Cipher decryptCipher(Context context, boolean functional)
	{
		Cipher cipher = new NullCipher();
		
		if (functional)
		{
			try
			{
				SecretKeySpec secretKey = this.keyForCipher(context, EncryptionManager.CRYPTO_ALGORITHM);

				IvParameterSpec ivParameterSpec = new IvParameterSpec(this.getIVBytes());

				cipher = Cipher.getInstance(EncryptionManager.CRYPTO_ALGORITHM);
				cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException(e);
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new RuntimeException(e);
			}
			catch (NoSuchPaddingException e)
			{
				throw new RuntimeException(e);
			}
			catch (InvalidKeyException e)
			{
				throw new RuntimeException(e);
			}
			catch (InvalidAlgorithmParameterException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return cipher;
	}
	
	public String createHash(String string)
	{
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
	
	public String getUserId(Context context)
	{
		SharedPreferences prefs = EncryptionManager.getPreferences(context);

		String userId = prefs.getString("config_user_id", null);

		if (userId == null)
		{
			userId = "unknown-user";

			AccountManager manager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
			Account[] list = manager.getAccountsByType("com.google");

			if (list.length == 0)
				list = manager.getAccounts();

			if (list.length > 0)
				userId = list[0].name;

			Editor e = prefs.edit();
			e.putString("config_user_id", userId);
			e.commit();
		}
		
		return userId;
	}

	public String getUserHash(Context context)
	{
		SharedPreferences prefs = EncryptionManager.getPreferences(context);

		String userHash = prefs.getString("config_user_hash", null);

		if (userHash == null)
		{
			String userId = this.getUserId(context);

			userHash = this.createHash(userId);

			Editor e = prefs.edit();

			if (userHash != null)
				e.putString("config_user_hash", userHash);

			e.commit();
		}

		return userHash;
	}

    public SecretKeySpec keyForCipher(Context context, String cipherName) throws UnsupportedEncodingException
	{
		String userHash = this.getUserHash(context);
		String keyString = (new StringBuffer(userHash)).reverse().toString();

		if (cipherName != null && cipherName.startsWith("AES"))
		{
			byte[] stringBytes = keyString.getBytes("UTF-8");

			byte[] keyBytes = new byte[32];
			Arrays.fill(keyBytes, (byte) 0x00);

			for (int i = 0; i < keyBytes.length && i < stringBytes.length; i++)
			{
				keyBytes[i] = stringBytes[i];
			}

			SecretKeySpec key = new SecretKeySpec(keyBytes, cipherName);

			return key;
		}

		return this.keyForCipher(context, EncryptionManager.CRYPTO_ALGORITHM);
	}

	protected byte[] getIVBytes()
	{
		byte[] bytes = {(byte) 0xff, 0x00, 0x11, (byte) 0xee, 0x22,
						(byte) 0xdd, 0x33, (byte) 0xcc, 0x44, (byte) 0xbb, 0x55,
						(byte) 0xaa, 0x66, (byte) 0x99, 0x77, (byte) 0x88 };

		return bytes;
	}

	public void writeToEncryptedStream(Context context, OutputStream out, byte[] bytes, boolean functional) throws IOException 
	{
		CipherOutputStream cout = new CipherOutputStream(out, this.encryptCipher(context, functional));

		cout.write(bytes);

		cout.flush();
		cout.close();
	}

	public byte[] readFromEncryptedStream(Context context, InputStream in, boolean functional) throws IOException 
	{
		CipherInputStream cin = new CipherInputStream(in, this.decryptCipher(context, functional));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024];
		int read = 0;

		while ((read = cin.read(buffer, 0, buffer.length)) != -1)
		{
			baos.write(buffer, 0, read);
		}

		cin.close();
		
		return baos.toByteArray();
	}
	
	public String fetchEncryptedString(Context context, String key)
	{
		key = this.createHash(key);
		
		SharedPreferences prefs = EncryptionManager.getPreferences(context);

		String encoded = prefs.getString(key, null);
		
		if (encoded != null)
		{
			try 
			{
				byte[] baseDecoded = Base64.decode(encoded, Base64.DEFAULT);
			
				byte[] decoded = this.decryptCipher(context, true).doFinal(baseDecoded);
				
				return new String(decoded, "UTF-8");
			} 
			catch (IllegalBlockSizeException e) 
			{
				e.printStackTrace();
			} 
			catch (BadPaddingException e) 
			{
				e.printStackTrace();
			} 
			catch (UnsupportedEncodingException e) 
			{
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public boolean persistEncryptedString(Context context, String key, String value)
	{
		if (value != null)
		{
			try 
			{
				byte[] encoded = this.encryptCipher(context, true).doFinal(value.getBytes("UTF-8"));
				
				String baseEncoded = Base64.encodeToString(encoded, Base64.DEFAULT);
				
				key = this.createHash(key);

				SharedPreferences prefs = EncryptionManager.getPreferences(context);
				Editor edit = prefs.edit();

				edit.putString(key, baseEncoded);
				
				return edit.commit();
			}
			catch (IllegalBlockSizeException e) 
			{
				e.printStackTrace();
			} 
			catch (BadPaddingException e) 
			{
				e.printStackTrace();
			} 
			catch (UnsupportedEncodingException e) 
			{
				e.printStackTrace();
			}
		}
		
		return false;
	}
}
