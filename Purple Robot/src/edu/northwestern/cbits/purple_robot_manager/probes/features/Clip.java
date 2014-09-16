package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.util.List;
import java.util.ArrayList;

public class Clip {
    
    public List<double[]> value;
    public List<Long> timestamp;
    private int dim;
    private long window_size;

    public Clip(int dim, long window_size) {

        value = new ArrayList<double[]>();
        timestamp = new ArrayList<Long>();
        this.dim = dim;
        this.window_size = window_size;

    }

    public int add(double[] value, long timestamp) {

        if (value.length!=this.dim)
            return -1;
        else {
            if (this.timestamp.size()==0)
                addthestuff(value, timestamp);
            else {
                while ((timestamp-this.timestamp.get(0) > window_size)&&(this.timestamp.size() >= 2)) 
                {
                    this.timestamp.remove(0);
                    this.value.remove(0);
                }
                if (timestamp-this.timestamp.get(0) > window_size) {
                    this.value = new ArrayList<double[]>();
                    this.timestamp = new ArrayList<Long>();
                }
                addthestuff(value, timestamp);
            }
            return 0;
        }

    }

    private void addthestuff(double[] value, long timestamp) {
        
        this.timestamp.add(timestamp);
        this.value.add(new double[dim]);
        this.value.set(this.value.size()-1, value);

    }

}