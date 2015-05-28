package edu.northwestern.cbits.purple_robot_manager.tests;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.Assert;
import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.R;

public class LocalHttpEndpointTestCase extends RobotTestCase
{
    private static final String PLUS_STRING = "1 + 1 = 2";

    public LocalHttpEndpointTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        try
        {
            this.broadcastUpdate("Testing Plus (+) Encoding...");

            JSONObject command = new JSONObject();
            command.put("command", "execute_script");
            command.put("script", "PurpleRobot.persistString('plus-test', '" + LocalHttpEndpointTestCase.PLUS_STRING
                    + "');");

            HashMap<String, String> payload = new HashMap<>();
            payload.put("json", command.toString(2));

            String response = this.syncHttpPost("http://127.0.0.1:12345/json/submit", payload);

            Assert.assertNotNull("LHE1", response);

            JSONObject responseJson = new JSONObject(response);

            Assert.assertEquals("LHE2", "ok", responseJson.get("status"));

            Thread.sleep(5000);

            command.put("script", "PurpleRobot.fetchString('plus-test');");
            payload.put("json", command.toString(2));

            response = this.syncHttpPost("http://127.0.0.1:12345/json/submit", payload);

            Assert.assertNotNull("LHE3", response);

            responseJson = new JSONObject(response);

            Assert.assertEquals("LHE4", "ok", responseJson.get("status"));
            Assert.assertEquals("LHE3", LocalHttpEndpointTestCase.PLUS_STRING, responseJson.get("payload"));

        }
        catch (InterruptedException e)
        {
            Assert.fail("LHE100");
        }
        catch (JSONException e)
        {
            Assert.fail("LHE101");
        }
        catch (KeyManagementException e)
        {
            Assert.fail("LHE102");
        }
        catch (UnrecoverableKeyException e)
        {
            Assert.fail("LHE103");
        }
        catch (KeyStoreException e)
        {
            Assert.fail("LHE104");
        }
        catch (NoSuchAlgorithmException e)
        {
            Assert.fail("LHE105");
        }
        catch (CertificateException e)
        {
            Assert.fail("LHE106");
        }
        catch (IOException e)
        {
            Assert.fail("LHE107");
        }
        catch (URISyntaxException e)
        {
            Assert.fail("LHE108");
        }
    }

    public int estimatedMinutes()
    {
        return 1;
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_local_http_server_test);
    }
}
