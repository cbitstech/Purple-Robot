package edu.northwestern.cbits.purple_robot_manager.db.filters;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;

public class ValueDeltaFilter extends Filter {
    private Set<String> _check = new HashSet<String>();

    private Map<String, Map<String, Double>> _priorValues = new HashMap<String, Map<String, Double>>();

    private double _minDelta = 0;

    public ValueDeltaFilter(double minDelta, Collection<String> toCheck) {
        this._minDelta = minDelta;

        if (toCheck != null)
            this._check.addAll(toCheck);
    }

    public boolean allow(String name, Map<String, Object> values) {
        if (this._check.contains(name) == false)
            return true;

        boolean allow = true;

        Map<String, Double> lastValues = this._priorValues.get(name);

        if (lastValues == null)
            lastValues = new HashMap<String, Double>();

        for (String key : values.keySet()) {
            if (allow == true
                    && ProbeValuesProvider.TIMESTAMP.equals(key) == false) {
                Object o = values.get(key);

                Double lastValue = lastValues.get(key);

                if (lastValue != null) {
                    if (o instanceof Double) {
                        Double d = (Double) o;

                        if (Math.abs(lastValue.doubleValue() - d.doubleValue()) < this._minDelta)
                            allow = false;
                    } else if (o instanceof Long) {
                        Long l = (Long) o;

                        if (Math.abs(lastValue.doubleValue() - l.doubleValue()) < this._minDelta)
                            allow = false;
                    }
                }
            }
        }

        if (allow) {
            for (String key : values.keySet()) {
                if (ProbeValuesProvider.TIMESTAMP.equals(key) == false) {
                    Object o = values.get(key);

                    if (o instanceof Double) {
                        Double d = (Double) o;

                        lastValues.put(key, Double.valueOf(d.doubleValue()));
                    } else if (o instanceof Long) {
                        Long l = (Long) o;

                        lastValues.put(key, Double.valueOf(l.doubleValue()));
                    }
                }
            }

            this._priorValues.put(name, lastValues);
        }

        return allow;
    }

    @Override
    public String description() {
        // TODO Auto-generated method stub
        return null;
    }
}
