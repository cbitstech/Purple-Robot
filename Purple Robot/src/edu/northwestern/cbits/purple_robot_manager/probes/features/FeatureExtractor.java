package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class FeatureExtractor {
    

    private long window_size;
    private List<String> feature_list;
    //private double[] features;
    private boolean hasFFT = false;
    private boolean hasDiff = false;
    private int dim;

    private List<double[]> signal_diff;

    private FastFourierTransformer fft;

    public FeatureExtractor(long window_size, List<String> feature_list, int dim) {

        this.window_size = window_size;
        this.feature_list = feature_list;
        this.dim = dim;

        for (String s: feature_list) {

            if ((!hasFFT)&&(s.contains("fft"))) {
                hasFFT = true;
                fft = new FastFourierTransformer(DftNormalization.STANDARD);
            }
            if ((!hasDiff)&&(s.contains("diff")))
                hasDiff = true;
        }

    }

    public List<Double> ExtractFeatures(Clip clip) {
        
        //Calculating the statistical moments
        double[] mean = new double[dim];
        double[] std = new double[dim];
        double[] skewness = new double[dim];
        double[] kurtosis = new double[dim];
        for (int i=0; i<dim; i++) {
            double[] moments = getMoments(clip.value, i);
            mean[i] = moments[0];
            std[i] = moments[1]; 
            skewness[i] = moments[2];
            kurtosis[i] = moments[3];
        }

        double[] diff_mean = new double[dim];
        double[] diff_std = new double[dim];
        double[] diff_skewness = new double[dim];
        double[] diff_kurtosis = new double[dim];
        if (hasDiff) {
            signal_diff = new ArrayList<double[]>();
            signal_diff = getDiff(clip.value);
            //Calculating the statistical moments of the difference signal
            for (int i=0; i<dim; i++) {
                double[] moments = getMoments(signal_diff, i);
                diff_mean[i] = moments[0];
                diff_std[i] = moments[1]; 
                diff_skewness[i] = moments[2];
                diff_kurtosis[i] = moments[3];
            }

        }
/*
        if (hasFFT)
            Complex[] fft_values = fft.transform(signal, TransformType.FORWARD);
*/
        List<Double> features = new ArrayList<Double>();

        int i=0;

        for (String s: feature_list) {

            switch (s) {
             
                case "nsamp": //for debugging purpose. can be removed later
                    features.add((double)clip.value.size());
                    break;

                case "_mean":
                case "x_mean":
                    features.add(mean[0]);
                    break;
                case "y_mean":
                    features.add(mean[1]);
                    break;
                case "z_mean":
                    features.add(mean[2]);
                    break;

                case "_std":
                case "x_std":
                    features.add(std[0]);
                    break;
                case "y_std":
                    features.add(std[1]);
                    break;
                case "z_std":
                    features.add(std[2]);
                    break;

                case "_skew":
                case "x_skew":
                    features.add(skewness[0]);
                    break;
                case "y_skew":
                    features.add(skewness[1]);
                    break;
                case "z_skew":
                    features.add(skewness[2]);
                    break;

                case "_kurt":
                case "x_kurt":
                    features.add(kurtosis[0]);
                    break;
                case "y_kurt":
                    features.add(kurtosis[1]);
                    break;
                case "z_kurt":
                    features.add(kurtosis[2]);
                    break;

                case "_diff_mean":
                case "x_diff_mean":
                    features.add(diff_mean[0]);
                    break;
                case "y_diff_mean":
                    features.add(diff_mean[1]);
                    break;
                case "z_diff_mean":
                    features.add(diff_mean[2]);
                    break;

                case "_diff_std":
                case "x_diff_std":
                    features.add(diff_std[0]);
                    break;
                case "y_diff_std":
                    features.add(diff_std[1]);
                    break;
                case "z_diff_std":
                    features.add(diff_std[2]);
                    break;

                case "_diff_skew":
                case "x_diff_skew":
                    features.add(diff_skewness[0]);
                    break;
                case "y_diff_skew":
                    features.add(diff_skewness[1]);
                    break;
                case "z_diff_skew":
                    features.add(diff_skewness[2]);
                    break;

                case "_diff_kurt":
                case "x_diff_kurt":
                    features.add(diff_kurtosis[0]);
                    break;
                case "y_diff_kurt":
                    features.add(diff_kurtosis[1]);
                    break;
                case "z_diff_kurt":
                    features.add(diff_kurtosis[2]);
                    break;


                default:

            }

            i++;

        }

        return features;

    }

    private List<double[]> getDiff(List<double[]> signal) {

        List<double[]> signal_diff = new ArrayList<double[]>();

        for (int i=0; i<signal.size()-1; i++) {

            double[] sig = signal.get(i);
            double[] sig_next = signal.get(i+1);

            double[] sig_diff = new double[sig.length];

            for (int j=0; j<sig.length; j++)
                sig_diff[j] = sig_next[j] - sig[j];

            signal_diff.add(sig_diff);

        }

        return signal_diff;

    }

    // This method will calculate mean, standard deviation, skewness, and kurtosis.
    // each member of the list is one statistical moment, which consists of an array, with
    // each element accounting for one dimension

    private double[] getMoments(List<double[]> signal, int axis) {

        double N = (double)signal.size();

        if (N<2) {
            double[] out = {0, 0, 0, 0};
            return out;
        }

        double sum = 0f;
        // for some reason, the following commented-out code generates "concurrent modification exception"!
        //for (double[] value : signal)
            //sum += value[axis];
        for (int i=0; i<N; i++)
            sum += signal.get(i)[axis];
            
        
        double mean = sum/N;

        double m2 = 0f;
        double m3 = 0f;
        double m4 = 0f;
        double t2,t3,t4;
        
        for (int i=0; i<N; i++) {

            t2 = (signal.get(i)[axis]-mean)*(signal.get(i)[axis]-mean);
            m2 += t2;

            t3 = t2*(signal.get(i)[axis]-mean);
            m3 += t3;

            t4 = t3*(signal.get(i)[axis]-mean);
            m4 += t4;
        }

        double std = (double)Math.sqrt(m2/(N-1)); //unbiased
        
        m2 /= N;
        m3 /= N;
        m4 /= N;
 
        double skewness = m3/(std*std*std); //unbiased

        double kurtosis = m4/(m2*m2) - 3; //unbiased

        double out[] = {mean, std, skewness, kurtosis};
        return out;

    }


}