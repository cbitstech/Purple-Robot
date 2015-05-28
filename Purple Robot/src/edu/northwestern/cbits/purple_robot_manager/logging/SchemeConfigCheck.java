package edu.northwestern.cbits.purple_robot_manager.logging;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.activities.settings.LegacySettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsKeys;
import jscheme.JScheme;
import jscheme.SchemeException;
import jsint.Evaluator;
import jsint.Pair;
import jsint.Symbol;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.JSONHelper;
import edu.northwestern.cbits.purple_robot_manager.scripting.SchemeEngine;

public class SchemeConfigCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_scheme_config_check);
    }

    public void runCheck(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        this._errorMessage = null;
        this._errorLevel = SanityCheck.OK;

        if (prefs.contains("scheme_config_contents") && prefs.getBoolean("check_scheme_config", true))
        {
            String schemeScript = prefs.getString("scheme_config_contents", "()");

            Evaluator eval = new Evaluator();
            eval.getInteractionEnvironment().setValue(Symbol.intern("PurpleRobot"), this);
            eval.getInteractionEnvironment().setValue(Symbol.intern("JSONHelper"), new JSONHelper());
            JScheme scheme = new JScheme(eval);

            try
            {
                scheme.load(new InputStreamReader(context.getAssets().open("scheme/pregexp.scm")));
            }
            catch (IOException e)
            {
                LogManager.getInstance(context).logException(e);
            }
            catch (StackOverflowError e)
            {
                // Happens on older devices...
                // LogManager.getInstance(context).logException(e);
            }

            try
            {
                scheme.load(new InputStreamReader(context.getAssets().open("scheme/json.scm")));
            }
            catch (IOException | StackOverflowError e)
            {
                LogManager.getInstance(context).logException(e);
            }

            try
            {
                scheme.load(new InputStreamReader(context.getAssets().open("scheme/purple-robot.scm")));
            }
            catch (IOException | StackOverflowError e)
            {
                LogManager.getInstance(context).logException(e);
            }

            try
            {
                Object sexp = scheme.read(schemeScript);

                if (sexp instanceof Pair)
                {
                    Pair pair = (Pair) sexp;

                    List<Map<String, Object>> configs = SchemeConfigCheck.parseConfigMaps(pair);

                    if (configs.size() > 0)
                    {
                        for (Map<String, Object> config : configs)
                        {
                            for (String key : config.keySet())
                            {
                                if (prefs.contains(key))
                                {
                                    Object cfgObject = config.get(key);

                                    if (cfgObject instanceof Boolean)
                                    {
                                        Boolean setting = (Boolean) cfgObject;

                                        boolean pref = prefs.getBoolean(key, (setting == false));

                                        if (pref != setting)
                                        {
                                            this._errorLevel = SanityCheck.WARNING;
                                            this._errorMessage = context.getString(
                                                    R.string.scheme_config_check_changed, key);
                                        }
                                    }
                                    else if (cfgObject instanceof String)
                                    {
                                        String pref = prefs.getString(key, null);

                                        if (cfgObject.toString().equals(pref) == false)
                                        {
                                            boolean mismatched = true;

                                            if (SettingsKeys.CONFIG_URL.equals(key))
                                            {
                                                if (pref.indexOf(cfgObject.toString()) == 0)
                                                    mismatched = false;
                                            }

                                            if (mismatched)
                                            {
                                                this._errorLevel = SanityCheck.WARNING;
                                                this._errorMessage = context.getString(R.string.scheme_config_check_changed, key);
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    this._errorLevel = SanityCheck.ERROR;
                                    this._errorMessage = context.getString(R.string.scheme_config_check_missing, key);

                                    return;
                                }
                            }
                        }
                    }
                }
            }
            catch (SchemeException e)
            {
                this._errorLevel = SanityCheck.ERROR;
                this._errorMessage = context.getString(R.string.scheme_config_invalid_config);

                LogManager.getInstance(context).logException(e);
            }
        }
    }

    private static List<Map<String, Object>> parseConfigMaps(Pair pair)
    {
        ArrayList<Map<String, Object>> maps = new ArrayList<>();

        Object first = pair.first;
        Object rest = pair.rest;

        if (first instanceof Symbol && "pr-update-config".equals(first.toString()) && rest instanceof Pair)
        {
            Pair restPair = (Pair) rest;

            restPair = (Pair) restPair.first;
            restPair = (Pair) restPair.rest;
            restPair = (Pair) restPair.first;

            Map<String, Object> map = SchemeEngine.parsePairList(restPair);

            maps.add(map);
        }
        else
        {
            if (first instanceof Pair)
            {
                Pair firstPair = (Pair) first;

                List<Map<String, Object>> configs = SchemeConfigCheck.parseConfigMaps(firstPair);

                if (configs.size() > 0)
                    maps.addAll(configs);
            }

            if (rest instanceof Pair)
            {
                Pair restPair = (Pair) rest;

                if (((Pair) rest).isEmpty() == false)
                {
                    List<Map<String, Object>> configs = SchemeConfigCheck.parseConfigMaps(restPair);

                    if (configs.size() > 0)
                        maps.addAll(configs);
                }
            }
        }

        return maps;
    }

    public Runnable getAction(final Context context)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                {
                    Intent intent = new Intent(context, LegacySettingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                }
                else
                {
                    Intent intent = new Intent(context, SettingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                }
            }
        };

        return r;
    }
}
