package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.util.ArrayList;

public class Clip {
    
    public final float window_size = 4000.0f; // timestamps are in ms
    public final float window_overlap = 0.75f;

    private ArrayList<float[]> value;
    private ArrayList<Long> timestamp;
    private int dim;

    public Clip(int dim) {

        value = new ArrayList<float[]>();
        timestamp = new ArrayList();
        this.dim = dim;

    }

    public int add(float[] value, long timestamp) {

        if (value.length!=this.dim)
            return -1;
        else {
            if (this.timestamp.size()==0)
                addthestuff(value, timestamp);
            else {
                while ((timestamp-this.timestamp.get(0) > window_size)&&(this.timestamp.size()>0)) {
                    this.timestamp.remove(0);
                    this.value.remove(0);
                }
                addthestuff(value, timestamp);
            }
            return 0;
        }
    }

    private void addthestuff(float[] value, long timestamp) {
        
        this.timestamp.add(timestamp);
        this.value.add(new float[dim]);
        this.value.set(this.value.size()-1, value);

    }

}