package edu.northwestern.cbits.purple_robot_manager.config;

import java.util.List;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;
import jsint.Pair;
import jsint.Symbol;
import android.content.Context;

public class SchemeConfigFile
{
    private Context _context;

    public SchemeConfigFile(Context context)
    {
        this._context = context;
    }

    public String triggersScript(Context context)
    {
        return this.triggersList(TriggerManager.getInstance(context).triggerConfigurations(this._context)).first()
                .toString();
    }

    public String toString()
    {
        Pair rest = this.triggersList(TriggerManager.getInstance(this._context).triggerConfigurations(this._context));
        rest = new Pair(SchemeConfigFile.probesList(ProbeManager.probeConfigurations(this._context)), rest);
        rest = new Pair(this.configuration(this._context), rest);

        Pair root = new Pair(Symbol.BEGIN, rest);

        return root.toString();
    }

    private Pair configuration(Context context)
    {
        Map<String, Object> configMap = PurpleRobotApplication.configuration(context);

        return new Pair(Symbol.intern("pr-update-config"), SchemeConfigFile.pairsList(configMap));
    }

    private Pair triggersList(List<Map<String, Object>> configs)
    {
        Pair rest = Pair.EMPTY;

        if (configs.size() == 0)
            return rest;

        for (Map<String, Object> config : configs)
        {
            rest = new Pair(new Pair(Symbol.intern("pr-update-trigger"), SchemeConfigFile.pairsList(config)), rest);
        }

        return new Pair(new Pair(Symbol.BEGIN, rest), Pair.EMPTY);
    }

    private static Pair probesList(List<Map<String, Object>> configs)
    {
        Pair rest = Pair.EMPTY;

        for (Map<String, Object> config : configs)
        {
            rest = new Pair(new Pair(Symbol.intern("pr-update-probe"), SchemeConfigFile.pairsList(config)), rest);
        }

        return new Pair(Symbol.BEGIN, rest);
    }

    private static Pair pairsList(Map<String, Object> config)
    {
        Pair list = Pair.EMPTY;

        for (String key : config.keySet())
        {
            list = new Pair(new Pair(key, config.get(key)), list);
        }

        return new Pair(new Pair(Symbol.QUOTE, new Pair(list, Pair.EMPTY)), Pair.EMPTY);
    }

    public static Pair probeConfig(Map<String, Object> config)
    {
        return new Pair(Symbol.intern("pr-update-probe"), SchemeConfigFile.pairsList(config));
    }
}
