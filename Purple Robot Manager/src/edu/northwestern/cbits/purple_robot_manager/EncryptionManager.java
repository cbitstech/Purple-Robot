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

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
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
	
	public Cipher encryptCipher(Context context)
	{
		Cipher cipher = new NullCipher();
		
		SharedPreferences prefs = EncryptionManager.getPreferences(context);

		if (prefs.getBoolean("config_http_encrypt", true))
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

	public Cipher decryptCipher(Context context)
	{
		Cipher cipher = new NullCipher();
		
		SharedPreferences prefs = EncryptionManager.getPreferences(context);

		if (prefs.getBoolean("config_http_encrypt", true))
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

	public String getUserHash(Context context)
	{
		SharedPreferences prefs = EncryptionManager.getPreferences(context);

		String userHash = prefs.getString("config_user_hash", null);

		if (userHash == null)
		{
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
			}

			try
			{
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] digest = md.digest(userId.getBytes("UTF-8"));

				userHash = (new BigInteger(1, digest)).toString(16);

				while (userHash.length() < 32)
				{
					userHash = "0" + userHash;
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

			Editor e = prefs.edit();

			if (userId != null)
				e.putString("config_user_id", userId);

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

	public void writeToEncryptedStream(Context context, OutputStream out, byte[] bytes) throws IOException 
	{
		CipherOutputStream cout = new CipherOutputStream(out, this.encryptCipher(context));

		cout.write(bytes);

		cout.flush();
		cout.close();
	}

	public byte[] readFromEncryptedStream(Context context, InputStream in) throws IOException 
	{
		CipherInputStream cin = new CipherInputStream(in, this.decryptCipher(context));

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
}
