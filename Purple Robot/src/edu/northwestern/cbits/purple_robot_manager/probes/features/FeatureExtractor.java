package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class FeatureExtractor {
    

    private long window_size;
    private String[] feature_list;
    private float[] values;
    private boolean hasFFT = false;
    private boolean hasDiff = false;

    private List<float[]> signal_diff;

    private FastFourierTransformer fft;

    public FeatureExtractor(long window_size, String[] feature_list) {

        this.window_size = window_size;
        this.feature_list = feature_list;

        for (String s: feature_list) {
            if (s.startsWith("fft")) {
                hasFFT = true;
                fft = new FastFourierTransformer(DftNormalization.STANDARD);
            }
            if (s.startsWith("diff")) {
                hasDiff = true;
                signal_diff = new ArrayList<float[]>();
            }
        }

    }

    public float[] ExtractFeatures(Clip clip) {

        if (hasDiff)
            signal_diff = getDiff(clip.value);
/*
        if (hasFFT) {
            Complex[] fft_values = fft.transform(signal, TransformType.FORWARD);
        }
*/
        values = new float[this.feature_list.length];

        for (int i=0; i<this.feature_list.length; i++) {

            switch (feature_list[i]) {

                case "nsamp":
                    values[i] = clip.value.size();
                    break;

                case "mean":
                case "meanx":
                    values[i] = getMean(clip.value, 0);
                    break;

                case "meany":
                    values[i] = getMean(clip.value, 1);
                    break;

                case "meanz":
                    values[i] = getMean(clip.value, 2);
                    break;

                case "std":
                case "stdx":
                    values[i] = getStd(clip.value, 0);
                    break;

                case "stdy":
                    values[i] = getStd(clip.value, 1);
                    break;

                case "stdz":
                    values[i] = getStd(clip.value, 2);
                    break;

                default:

            }

        }

        return values;

    }

    private List<float[]> getDiff(List<float[]> signal) {

        List<float[]> signal_diff = new ArrayList<float[]>();

        for (int i=0; i<signal.size()-1; i++) {

            float[] sig = signal.get(i);
            float[] sig_next = signal.get(i+1);

            float[] sig_diff = new float[sig.length];

            for (int j=0; j<sig.length; j++)
                sig_diff[j] = sig_next[j] - sig[j];

            signal_diff.add(sig_diff);

        }

        return signal_diff;

    }

    private float getMean(List<float[]> signal, int axis) {
        
        float sum = 0.0f;
        for (float[] value : signal)
            sum += value[axis];
        return sum/signal.size();

    }

    private float getStd(List<float[]> signal, int axis) {
        
        float mean = 0f;
        for (float[] value : signal)
            mean += value[axis];
        mean /= signal.size();
        
        float std = 0f;
        for (float[] value : signal)
            std = std + (value[axis]-mean)*(value[axis]-mean);
        std /= signal.size()-1; //unbiased estimator
        std = (float)Math.sqrt(std);

        return std;

    }


}