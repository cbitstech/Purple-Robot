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
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class UserIdTestCase extends RobotTestCase
{
    private static final String TEST_ID_1 = "test1@example.com";
    private static final String TEST_ID_2 = "test2@example.com";

    public UserIdTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        JavaScriptEngine engine = new JavaScriptEngine(this._context);

        String originalId = engine.fetchUserId();

        try
        {
            this.broadcastUpdate("Testing code directly...");

            Assert.assertNotNull("UID1", originalId);

            Thread.sleep(1000);

            engine.setUserId(UserIdTestCase.TEST_ID_1, false);

            Thread.sleep(1000);

            Assert.assertEquals("UID2", UserIdTestCase.TEST_ID_1, engine.fetchUserId());

            engine.setUserId(UserIdTestCase.TEST_ID_2, false);

            Thread.sleep(1000);

            Assert.assertEquals("UID3", UserIdTestCase.TEST_ID_2, engine.fetchUserId());

            this.broadcastUpdate("Testing over HTTP API... (1/3)");

            JSONObject command = new JSONObject();
            command.put("command", "execute_script");
            command.put("script", "PurpleRobot.setUserId('" + UserIdTestCase.TEST_ID_1 + "', false);");

            HashMap<String, String> payload = new HashMap<>();
            payload.put("json", command.toString(2));

            String response = this.syncHttpPost("http://127.0.0.1:12345/json/submit", payload);

            Assert.assertNotNull("UID4", response);

            JSONObject responseJson = new JSONObject(response);

            Assert.assertEquals("UID5", "ok", responseJson.get("status"));

            Thread.sleep(1000);

            command.put("script", "PurpleRobot.fetchUserId();");

            payload.put("json", command.toString(2));

            response = this.syncHttpPost("http://127.0.0.1:12345/json/submit", payload);

            Assert.assertNotNull("UID6", response);

            responseJson = new JSONObject(response);

            Assert.assertEquals("UID7", "ok", responseJson.get("status"));
            Assert.assertEquals("UID8", UserIdTestCase.TEST_ID_1, responseJson.get("payload"));

            this.broadcastUpdate("Testing over HTTP API... (2/3)");

            Thread.sleep(1000);

            command.put("script", "PurpleRobot.setUserId('" + UserIdTestCase.TEST_ID_2 + "', false);");

            payload.put("json", command.toString(2));

            response = this.syncHttpPost("http://127.0.0.1:12345/json/submit", payload);

            Assert.assertNotNull("UID9", response);

            responseJson = new JSONObject(response);

            Assert.assertEquals("UID10", "ok", responseJson.get("status"));

            Thread.sleep(1000);

            command.put("script", "PurpleRobot.fetchUserId();");

            payload.put("json", command.toString(2));

            response = this.syncHttpPost("http://127.0.0.1:12345/json/submit", payload);

            Assert.assertNotNull("UID11", response);

            responseJson = new JSONObject(response);

            Assert.assertEquals("UID12", "ok", responseJson.get("status"));
            Assert.assertEquals("UID13", UserIdTestCase.TEST_ID_2, responseJson.get("payload"));

            Thread.sleep(1000);

            this.broadcastUpdate("Testing over HTTP API... (3/3)");

            command.put("command", "execute_script");
            command.put("script", "PurpleRobot.setUserId('" + UserIdTestCase.TEST_ID_1 + "', false);");

            payload.put("json", command.toString(2));

            response = this.syncHttpPost("http://127.0.0.1:12345/json/submit", payload);

            Assert.assertNotNull("UID14", response);

            responseJson = new JSONObject(response);

            Assert.assertEquals("UID15", "ok", responseJson.get("status"));

            Thread.sleep(1000);

            command.put("script", "PurpleRobot.fetchUserId();");

            payload.put("json", command.toString(2));

            response = this.syncHttpPost("http://127.0.0.1:12345/json/submit", payload);

            Assert.assertNotNull("UID16", response);

            responseJson = new JSONObject(response);

            Assert.assertEquals("UID17", "ok", responseJson.get("status"));
            Assert.assertEquals("UID18", UserIdTestCase.TEST_ID_1, responseJson.get("payload"));
        }
        catch (InterruptedException e)
        {
            Assert.fail("UID100");
        }
        catch (JSONException e)
        {
            Assert.fail("UID101");
        }
        catch (KeyManagementException e)
        {
            Assert.fail("UID102");
        }
        catch (UnrecoverableKeyException e)
        {
            Assert.fail("UID103");
        }
        catch (KeyStoreException e)
        {
            Assert.fail("UID104");
        }
        catch (NoSuchAlgorithmException e)
        {
            Assert.fail("UID105");
        }
        catch (CertificateException e)
        {
            Assert.fail("UID106");
        }
        catch (IOException e)
        {
            Assert.fail("UID107");
        }
        catch (URISyntaxException e)
        {
            Assert.fail("UID108");
        }

        engine.setUserId(originalId, true);

        Assert.assertEquals("UID1000", originalId, engine.fetchUserId());
    }

    public int estimatedMinutes()
    {
        return 1;
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_user_id_test);
    }
}
