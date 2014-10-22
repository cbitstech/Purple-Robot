package edu.northwestern.cbits.purple_robot_manager.db.filters;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;

public class FrequencyThrottleFilter extends Filter {
    private Map<String, Double> _lastSaves = new HashMap<String, Double>();
    private Set<String> _exclude = new HashSet<String>();
    private Set<String> _include = new HashSet<String>();

    private long _minInterval = 1000;

    public FrequencyThrottleFilter(long minInterval,
            Collection<String> include, Collection<String> exclude) {
        this._minInterval = minInterval;

        if (include != null)
            this._include.addAll(include);

        if (exclude != null)
            this._exclude.addAll(exclude);
    }

    public boolean allow(String name, Map<String, Object> values) {
        if (this._include.size() == 0 || this._include.contains(name)) {
            if (this._exclude.contains(name))
                return true;

            Double timestamp = (Double) values
                    .get(ProbeValuesProvider.TIMESTAMP) * 1000;

            Double lastSave = this._lastSaves.get(name);

            if (lastSave == null)
                lastSave = Double.valueOf(0);

            if (lastSave.doubleValue() > timestamp.doubleValue()
                    || Math.abs(timestamp.doubleValue()
                            - lastSave.doubleValue()) < this._minInterval)
                return false;

            this._lastSaves.put(name, Double.valueOf(timestamp.doubleValue()));

            return true;
        }

        return true;
    }

    @Override
    public String description() {
        // TODO Auto-generated method stub
        return null;
    }
}
