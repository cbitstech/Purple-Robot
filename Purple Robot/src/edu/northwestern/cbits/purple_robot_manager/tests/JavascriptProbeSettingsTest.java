package edu.northwestern.cbits.purple_robot_manager.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public class JavascriptProbeSettingsTest extends RobotTestCase
{
    public JavascriptProbeSettingsTest(Context context, int priority)
    {
        super(context, priority);
    }

    @Override
    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        HashMap<String, JSONObject> probeDefs = new HashMap<>();

        for (Probe probe : ProbeManager.allProbes(this._context))
        {
            JSONObject settings = probe.fetchSettings(this._context);

            if (settings != null)
                probeDefs.put(probe.name(this._context), settings);
        }

        try
        {
            ArrayList<String> names = new ArrayList<>();

            for (String name : probeDefs.keySet())
            {
                names.add(name);
            }

            Collections.sort(names);

            for (int j = 0; j < names.size(); j++)
            {
                String name = names.get(j);

                Probe probe = ProbeManager.probeForName(name, this._context);

                if (probe != null)
                {
                    this.broadcastUpdate("Testing " + probe.title(this._context) + " (" + (j + 1) + "/" + names.size() + ")...");

                    JSONObject settings = probeDefs.get(name);

                    Iterator<String> keys = settings.keys();

                    while (keys.hasNext())
                    {
                        String key = keys.next();

                        JSONObject value = settings.getJSONObject(key);
                        JSONArray options = value.getJSONArray(Probe.PROBE_VALUES);

                        for (int i = 0; i < options.length(); i++)
                        {
                            Object option = options.get(i);

                            Log.e("PR", "TESTING JSPSX-" + probe.shortName(this._context) + "-" + key + "-" + option);

                            JSONObject payload = new JSONObject();
                            payload.put(Probe.PROBE_NAME, name);
                            payload.put(key, option);

                            Object returned = BaseScriptEngine.runScript(this._context, "PurpleRobot.updateProbe(" + payload.toString().replace("\"", "'") + ");");

                            Assert.assertEquals("JSPS0-" + probe.shortName(this._context) + "-" + key + "-" + option, returned.getClass(), Boolean.class);
                            Assert.assertTrue("JSPS1-" + probe.shortName(this._context) + "-" + key + "-" + option, ((Boolean) returned).booleanValue());

                            Thread.sleep(1000);

                            Map<String, Object> config = probe.configuration(this._context);

                            Assert.assertEquals("JSPS2-" + probe.shortName(this._context) + "-" + key + "-" + option, option, config.get(key));

                            Thread.sleep(1000);
                        }
                    }
                }
                else
                    Assert.fail("JSPS 9000: " + name);
            }
        }
        catch (JSONException | InterruptedException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }
    }

    @Override
    public int estimatedMinutes()
    {
        HashMap<String, JSONObject> probeDefs = new HashMap<>();

        for (Probe probe : ProbeManager.allProbes(this._context))
        {
            JSONObject settings = probe.fetchSettings(this._context);

            if (settings != null)
                probeDefs.put(probe.name(this._context), settings);
        }

        return probeDefs.keySet().size() / 2;
    }

    @Override
    public String name(Context context)
    {
        return context.getString(R.string.name_javascript_probe_config_test);
    }

}
