package edu.northwestern.cbits.purple_robot_manager.db.filters;

import java.util.Map;

public abstract class Filter
{
    public abstract boolean allow(String name, Map<String, Object> values);

    public abstract String description();
}
