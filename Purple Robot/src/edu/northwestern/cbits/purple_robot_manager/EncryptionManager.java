package edu.northwestern.cbits.purple_robot_manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

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
import android.net.Uri;
import android.net.Uri.Builder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsKeys;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class EncryptionManager
{
    private static final String CRYPTO_ALGORITHM = "AES/CBC/PKCS5Padding";

    private HashMap<String, String> _cachedHashes = new HashMap<String, String>();

    private static final EncryptionManager _instance = new EncryptionManager();
    private boolean _configurationReady = false;

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

    public String createHash(Context context, String string, String algorithm)
    {
        if (string == null)
            return null;

        String hash = null;

        try
        {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(string.getBytes("UTF-8"));

            hash = (new BigInteger(1, digest)).toString(16);

            while (hash.length() < 32)
            {
                hash = "0" + hash;
            }
        }
        catch (NoSuchAlgorithmException e)
        {
            LogManager.getInstance(context).logException(e);
        }
        catch (UnsupportedEncodingException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return hash;
    }

    public String getUserId(Context context)
    {
        SharedPreferences prefs = EncryptionManager.getPreferences(context);

        String userId = prefs.getString(SettingsKeys.USER_ID_KEY, null);

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
            e.putString(SettingsKeys.USER_ID_KEY, userId);
            e.commit();
        }

        return userId;
    }

    public String getUserHash(Context context)
    {
        String userId = this.getUserId(context);

        String hash = this._cachedHashes.get(userId);

        if (hash == null)
        {
            hash = this.createHash(context, userId);
            this._cachedHashes.put(userId, hash);
        }

        return hash;
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
        byte[] bytes =
        { (byte) 0xff, 0x00, 0x11, (byte) 0xee, 0x22, (byte) 0xdd, 0x33, (byte) 0xcc, 0x44, (byte) 0xbb, 0x55,
                (byte) 0xaa, 0x66, (byte) 0x99, 0x77, (byte) 0x88 };

        return bytes;
    }

    public void writeToEncryptedStream(Context context, OutputStream out, byte[] bytes, boolean functional)
            throws IOException
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

        byte[] buffer = new byte[2048];
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
        key = this.createHash(context, key);

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
                LogManager.getInstance(context).logException(e);
            }
            catch (BadPaddingException e)
            {
                LogManager.getInstance(context).logException(e);
            }
            catch (UnsupportedEncodingException e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }

        return null;
    }

    public String encryptString(Context context, String value) throws IllegalBlockSizeException, BadPaddingException,
            UnsupportedEncodingException
    {
        byte[] encoded = this.encryptCipher(context, true).doFinal(value.getBytes("UTF-8"));

        String baseEncoded = Base64.encodeToString(encoded, Base64.DEFAULT);

        return baseEncoded;
    }

    public boolean persistEncryptedString(Context context, String key, String value)
    {
        try
        {
            SharedPreferences prefs = EncryptionManager.getPreferences(context);
            Editor edit = prefs.edit();

            key = this.createHash(context, key);

            if (value != null)
            {
                byte[] encoded = this.encryptCipher(context, true).doFinal(value.getBytes("UTF-8"));

                String baseEncoded = Base64.encodeToString(encoded, Base64.DEFAULT);

                edit.putString(key, baseEncoded);
            }
            else
                edit.remove(key);

            return edit.commit();
        }
        catch (IllegalBlockSizeException e)
        {
            LogManager.getInstance(context).logException(e);
        }
        catch (BadPaddingException e)
        {
            LogManager.getInstance(context).logException(e);
        }
        catch (UnsupportedEncodingException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return false;
    }

    public void setConfigUri(Context context, Uri configUri)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor e = prefs.edit();

        if (configUri != null)
        {
            Builder builder = new Builder();

            builder.scheme(configUri.getScheme());
            builder.encodedAuthority(configUri.getAuthority());

            if (configUri.getPath() != null)
                builder.encodedPath(configUri.getPath());

            if (configUri.getFragment() != null)
                builder.encodedFragment(configUri.getFragment());

            String query = configUri.getQuery();

            ArrayList<String> keys = new ArrayList<String>();

            if (query != null)
            {
                String[] params = query.split("&");

                for (String param : params)
                {
                    String[] components = param.split("=");

                    keys.add(components[0]);
                }
            }

            for (String key : keys)
            {
                if ("user_id".equals(key))
                {
                    // Skip for now.
                }
                else
                    builder.appendQueryParameter(key, configUri.getQueryParameter(key));
            }

            builder.appendQueryParameter("user_id", this.getUserId(context));

            configUri = builder.build();

            e.putString(SettingsKeys.CONFIG_URL, configUri.toString());
        }
        else
            e.remove(SettingsKeys.CONFIG_URL);

        e.commit();

        this._configurationReady = false;
    }

    public Uri getConfigUri(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String uriString = prefs.getString(SettingsKeys.CONFIG_URL, null);

        if (uriString != null)
        {
            Uri uri = Uri.parse(uriString);

            Builder builder = new Builder();

            builder.scheme(uri.getScheme());
            builder.encodedAuthority(uri.getAuthority());

            if (uri.getPath() != null)
                builder.encodedPath(uri.getPath());

            if (uri.getFragment() != null)
                builder.encodedFragment(uri.getFragment());

            String query = uri.getQuery();

            ArrayList<String> keys = new ArrayList<String>();

            if (query != null)
            {
                String[] params = query.split("&");

                for (String param : params)
                {
                    String[] components = param.split("=");

                    keys.add(components[0]);
                }
            }

            for (String key : keys)
            {
                if ("user_id".equals(key))
                    this.setUserId(context, uri.getQueryParameter(key));
                else
                    builder.appendQueryParameter(key, uri.getQueryParameter(key));
            }

            builder.appendQueryParameter("user_id", this.getUserId(context));

            uri = builder.build();

            return uri;
        }

        return null;
    }

    public void restoreDefaultId(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor e = prefs.edit();
        e.remove(SettingsKeys.USER_ID_KEY);
        e.commit();

        LogManager.getInstance(context).log("restored_default_user_id", null);
    }

    public void setUserId(Context context, String userId)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        HashMap<String, Object> payload = new HashMap<String, Object>();
        payload.put("source", "EncryptionManager");
        payload.put("new_id", userId);
        payload.put("old_id", prefs.getString(SettingsKeys.USER_ID_KEY, ""));

        LogManager.getInstance(context).log("set_user_id", payload);

        Editor e = prefs.edit();
        e.putString(SettingsKeys.USER_ID_KEY, userId);
        e.commit();

        this.setConfigUri(context, Uri.parse(prefs.getString(SettingsKeys.CONFIG_URL, context.getString(R.string.json_config_url))));
    }

    public String createHash(Context context, String name)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String algorithm = prefs.getString("config_preferred_hash_function", "MD5");

        try
        {
            MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e)
        {
            // Fall back to MD5 if SHA-2 is not present...

            algorithm = "MD5";
        }

        return this.createHash(context, name, algorithm);
    }

    public void setConfigurationReady(boolean isReady)
    {
        this._configurationReady = isReady;
    }

    public boolean getConfigurationReady()
    {
        return this._configurationReady;
    }
}
