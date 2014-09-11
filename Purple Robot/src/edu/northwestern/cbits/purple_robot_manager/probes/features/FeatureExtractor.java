package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.util.List;
import java.util.ArrayList;

public class FeatureExtractor {
    

    private long window_size;
    private ArrayList<String> feature_list;
    public ArrayList<Float> feature_values;

    public FeatureExtractor(long window_size, ArrayList<String> feature_list) {

        this.window_size = window_size;
        this.feature_list = feature_list;

    }

    public void ExtractFeatures(Clip clip) {

        feature_values = new ArrayList<Float>();

        for (String feature_name : feature_list) {

            switch (feature_name) {

                case "meanx":
                    feature_values.add(mean(clip.value, 0));
                    break;

                case "meany":
                    feature_values.add(mean(clip.value, 1));
                    break;

                case "meanz":
                    feature_values.add(mean(clip.value, 2));
                    break;
                    
                default:

            }

        }

    }

    private float mean(List<float[]> values, int axis) {
        float sum = 0.0f;
        for (float[] value : values)
            sum += value[axis];
        return sum/values.size();
    }

}