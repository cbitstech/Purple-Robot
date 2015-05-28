package edu.northwestern.cbits.purple_robot_manager.probes.features.p20;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import android.util.Log;
import edu.emory.mathcs.backport.java.util.Arrays;

public class FeatureExtractor
{
    private long _windowSize = -1;
    private int _dimensions = -1;

    private boolean _hasFFT = false;
    private boolean _hasDiff = false;
    private boolean _hasHist = false;
    private boolean _hasCross = false;
    private boolean _hasNormCross = false;

    // private FastFourierTransformer _fft = null;

    // bin edges must be in ascending order and equally spaced.
    private double[] _binEdges = new double[]
    { -3, -2, -1, 0, 1, 2, 3 };

    public enum Feature
    {
        ACC_NUM_SAMPLES, ACC_MEAN, ACCX_MEAN, ACCY_MEAN, ACCZ_MEAN, ACC_MEAN_ABS, ACCX_MEAN_ABS, ACCY_MEAN_ABS, ACCZ_MEAN_ABS, ACCX_STD, ACCY_STD, ACCZ_STD, ACCX_SKEW, ACCY_SKEW, ACCZ_SKEW, ACCX_KURT, ACCY_KURT, ACCZ_KURT, ACCX_DIFF_MEAN, ACCY_DIFF_MEAN, ACCZ_DIFF_MEAN, ACCX_DIFF_STD, ACCY_DIFF_STD, ACCZ_DIFF_STD, ACCX_DIFF_SKEW, ACCY_DIFF_SKEW, ACCZ_DIFF_SKEW, ACCX_DIFF_KURT, ACCY_DIFF_KURT, ACCZ_DIFF_KURT, ACCX_MAX, ACCY_MAX, ACCZ_MAX, ACCX_MIN, ACCY_MIN, ACCZ_MIN, ACCX_MAX_ABS, ACCY_MAX_ABS, ACCZ_MAX_ABS, ACCX_MIN_ABS, ACCY_MIN_ABS, ACCZ_MIN_ABS, ACCX_RMS, ACCY_RMS, ACCZ_RMS, ACC_CROSS_XY, ACC_CROSS_YZ, ACC_CROSS_ZX, ACC_CROSS_XY_ABS, ACC_CROSS_YZ_ABS, ACC_CROSS_ZX_ABS, ACC_CROSS_XY_NORM, ACC_CROSS_YZ_NORM, ACC_CROSS_ZX_NORM, ACC_CROSS_XY_NORM_ABS, ACC_CROSS_YZ_NORM_ABS, ACC_CROSS_ZX_NORM_ABS, ACCX_FFT1, ACCX_FFT2, ACCX_FFT3, ACCX_FFT4, ACCX_FFT5, ACCX_FFT6, ACCX_FFT7, ACCX_FFT8, ACCX_FFT9, ACCX_FFT10, ACCY_FFT1, ACCY_FFT2, ACCY_FFT3, ACCY_FFT4, ACCY_FFT5, ACCY_FFT6, ACCY_FFT7, ACCY_FFT8, ACCY_FFT9, ACCY_FFT10, ACCZ_FFT1, ACCZ_FFT2, ACCZ_FFT3, ACCZ_FFT4, ACCZ_FFT5, ACCZ_FFT6, ACCZ_FFT7, ACCZ_FFT8, ACCZ_FFT9, ACCZ_FFT10, ACCX_HIST1, ACCX_HIST2, ACCX_HIST3, ACCX_HIST4, ACCX_HIST5, ACCX_HIST6, ACCY_HIST1, ACCY_HIST2, ACCY_HIST3, ACCY_HIST4, ACCY_HIST5, ACCY_HIST6, ACCZ_HIST1, ACCZ_HIST2, ACCZ_HIST3, ACCZ_HIST4, ACCZ_HIST5, ACCZ_HIST6, GYR_NUM_SAMPLES, GYR_MEAN, GYRX_MEAN, GYRY_MEAN, GYRZ_MEAN, GYR_MEAN_ABS, GYRX_MEAN_ABS, GYRY_MEAN_ABS, GYRZ_MEAN_ABS, GYRX_STD, GYRY_STD, GYRZ_STD, GYRX_SKEW, GYRY_SKEW, GYRZ_SKEW, GYRX_KURT, GYRY_KURT, GYRZ_KURT, GYRX_DIFF_MEAN, GYRY_DIFF_MEAN, GYRZ_DIFF_MEAN, GYRX_DIFF_STD, GYRY_DIFF_STD, GYRZ_DIFF_STD, GYRX_DIFF_SKEW, GYRY_DIFF_SKEW, GYRZ_DIFF_SKEW, GYRX_DIFF_KURT, GYRY_DIFF_KURT, GYRZ_DIFF_KURT, GYRX_MAX, GYRY_MAX, GYRZ_MAX, GYRX_MIN, GYRY_MIN, GYRZ_MIN, GYRX_MAX_ABS, GYRY_MAX_ABS, GYRZ_MAX_ABS, GYRX_MIN_ABS, GYRY_MIN_ABS, GYRZ_MIN_ABS, GYRX_RMS, GYRY_RMS, GYRZ_RMS, GYR_CROSS_XY, GYR_CROSS_YZ, GYR_CROSS_ZX, GYR_CROSS_XY_ABS, GYR_CROSS_YZ_ABS, GYR_CROSS_ZX_ABS, GYR_CROSS_XY_NORM, GYR_CROSS_YZ_NORM, GYR_CROSS_ZX_NORM, GYR_CROSS_XY_NORM_ABS, GYR_CROSS_YZ_NORM_ABS, GYR_CROSS_ZX_NORM_ABS, GYRX_FFT1, GYRX_FFT2, GYRX_FFT3, GYRX_FFT4, GYRX_FFT5, GYRX_FFT6, GYRX_FFT7, GYRX_FFT8, GYRX_FFT9, GYRX_FFT10, GYRY_FFT1, GYRY_FFT2, GYRY_FFT3, GYRY_FFT4, GYRY_FFT5, GYRY_FFT6, GYRY_FFT7, GYRY_FFT8, GYRY_FFT9, GYRY_FFT10, GYRZ_FFT1, GYRZ_FFT2, GYRZ_FFT3, GYRZ_FFT4, GYRZ_FFT5, GYRZ_FFT6, GYRZ_FFT7, GYRZ_FFT8, GYRZ_FFT9, GYRZ_FFT10, GYRX_HIST1, GYRX_HIST2, GYRX_HIST3, GYRX_HIST4, GYRX_HIST5, GYRX_HIST6, GYRY_HIST1, GYRY_HIST2, GYRY_HIST3, GYRY_HIST4, GYRY_HIST5, GYRY_HIST6, GYRZ_HIST1, GYRZ_HIST2, GYRZ_HIST3, GYRZ_HIST4, GYRZ_HIST5, GYRZ_HIST6, PROCESSING_TIME
    }

    public FeatureExtractor(long windowSize, List<Feature> features, int dimensions)
    {
        this._windowSize = windowSize;
        this._dimensions = dimensions;

        for (Feature f : features)
        {
            String featureName = f.toString();

            if (featureName.contains("FFT"))
            {
                this._hasFFT = true;
                // this._fft = new
                // FastFourierTransformer(DftNormalization.STANDARD);
            }
            else if (featureName.contains("DIFF"))
                this._hasDiff = true;
            else if (featureName.contains("HIST"))
                this._hasHist = true;
            else if (featureName.contains("CROSS"))
            {
                this._hasCross = true;

                if (featureName.contains("NORM"))
                    this._hasNormCross = true;
            }
        }
    }

    public void setBinEdges(double[] edges)
    {
        this._binEdges = Arrays.copyOf(edges, edges.length);
    }

    public Map<Feature, Double> extractFeatures(Clip clp)
    {
        HashMap<Feature, Double> features = new HashMap<>();

        // build a copy of the clip. because it sometimes crashes suspiciously.
        Clip clip = new Clip(clp);

        // Spline Interpolation
        List<double[]> signal = this.interpolate(clip.getValues(), clip.getTimestamps(), 50);

        // Calculating the statistical moments
        double[] mean = new double[this._dimensions];
        double[] std = new double[this._dimensions];
        double[] skewness = new double[this._dimensions];
        double[] kurtosis = new double[this._dimensions];

        for (int i = 0; i < this._dimensions; i++)
        {
            double[] moments = this.getMoments(signal, i);

            mean[i] = moments[0];
            std[i] = moments[1];
            skewness[i] = moments[2];
            kurtosis[i] = moments[3];
        }

        switch (clip.getType())
        {
        case Clip.ACCELEROMETER:
            features.put(Feature.ACC_NUM_SAMPLES, (double) signal.size());
            features.put(Feature.ACC_MEAN, this.getOverallMean(signal));

            features.put(Feature.ACCX_MAX, this.getMax(signal, 0));
            features.put(Feature.ACCY_MAX, this.getMax(signal, 1));
            features.put(Feature.ACCZ_MAX, this.getMax(signal, 2));

            features.put(Feature.ACCX_MIN, this.getMin(signal, 0));
            features.put(Feature.ACCY_MIN, this.getMin(signal, 1));
            features.put(Feature.ACCZ_MIN, this.getMin(signal, 2));

            features.put(Feature.ACCX_MAX_ABS, Math.abs(this.getMax(signal, 0)));
            features.put(Feature.ACCY_MAX_ABS, Math.abs(this.getMax(signal, 1)));
            features.put(Feature.ACCZ_MAX_ABS, Math.abs(this.getMax(signal, 2)));

            features.put(Feature.ACCX_MIN_ABS, Math.abs(this.getMin(signal, 0)));
            features.put(Feature.ACCY_MIN_ABS, Math.abs(this.getMin(signal, 1)));
            features.put(Feature.ACCZ_MIN_ABS, Math.abs(this.getMin(signal, 2)));

            features.put(Feature.ACCX_MEAN, mean[0]);
            features.put(Feature.ACCY_MEAN, mean[1]);
            features.put(Feature.ACCZ_MEAN, mean[2]);

            features.put(Feature.ACCX_MEAN_ABS, Math.abs(mean[0]));
            features.put(Feature.ACCY_MEAN_ABS, Math.abs(mean[1]));
            features.put(Feature.ACCZ_MEAN_ABS, Math.abs(mean[2]));

            features.put(Feature.ACCX_STD, std[0]);
            features.put(Feature.ACCY_STD, std[1]);
            features.put(Feature.ACCZ_STD, std[2]);

            features.put(Feature.ACCX_SKEW, skewness[0]);
            features.put(Feature.ACCY_SKEW, skewness[1]);
            features.put(Feature.ACCZ_SKEW, skewness[2]);

            features.put(Feature.ACCX_KURT, kurtosis[0]);
            features.put(Feature.ACCY_KURT, kurtosis[1]);
            features.put(Feature.ACCZ_KURT, kurtosis[2]);

            features.put(Feature.ACCX_RMS, this.getRMS(signal, 0));
            features.put(Feature.ACCY_RMS, this.getRMS(signal, 1));
            features.put(Feature.ACCZ_RMS, this.getRMS(signal, 2));

            break;
        case Clip.GYROSCOPE:
            features.put(Feature.GYR_NUM_SAMPLES, (double) signal.size());
            features.put(Feature.GYR_MEAN, this.getOverallMean(signal));

            features.put(Feature.GYRX_MAX, this.getMax(signal, 0));
            features.put(Feature.GYRY_MAX, this.getMax(signal, 1));
            features.put(Feature.GYRZ_MAX, this.getMax(signal, 2));

            features.put(Feature.GYRX_MIN, this.getMin(signal, 0));
            features.put(Feature.GYRY_MIN, this.getMin(signal, 1));
            features.put(Feature.GYRZ_MIN, this.getMin(signal, 2));

            features.put(Feature.GYRX_MAX_ABS, Math.abs(this.getMax(signal, 0)));
            features.put(Feature.GYRY_MAX_ABS, Math.abs(this.getMax(signal, 1)));
            features.put(Feature.GYRZ_MAX_ABS, Math.abs(this.getMax(signal, 2)));

            features.put(Feature.GYRX_MIN_ABS, Math.abs(this.getMin(signal, 0)));
            features.put(Feature.GYRY_MIN_ABS, Math.abs(this.getMin(signal, 1)));
            features.put(Feature.GYRZ_MIN_ABS, Math.abs(this.getMin(signal, 2)));

            features.put(Feature.GYRX_MEAN, mean[0]);
            features.put(Feature.GYRY_MEAN, mean[1]);
            features.put(Feature.GYRZ_MEAN, mean[2]);

            features.put(Feature.GYRX_MEAN_ABS, Math.abs(mean[0]));
            features.put(Feature.GYRY_MEAN_ABS, Math.abs(mean[1]));
            features.put(Feature.GYRZ_MEAN_ABS, Math.abs(mean[2]));

            features.put(Feature.GYRX_STD, std[0]);
            features.put(Feature.GYRY_STD, std[1]);
            features.put(Feature.GYRZ_STD, std[2]);

            features.put(Feature.GYRX_SKEW, skewness[0]);
            features.put(Feature.GYRY_SKEW, skewness[1]);
            features.put(Feature.GYRZ_SKEW, skewness[2]);

            features.put(Feature.GYRX_KURT, kurtosis[0]);
            features.put(Feature.GYRY_KURT, kurtosis[1]);
            features.put(Feature.GYRZ_KURT, kurtosis[2]);

            features.put(Feature.GYRX_RMS, this.getRMS(signal, 0));
            features.put(Feature.GYRY_RMS, this.getRMS(signal, 1));
            features.put(Feature.GYRZ_RMS, this.getRMS(signal, 2));

            break;
        }

        if (this._hasDiff)
        {
            double[] diffMean = new double[this._dimensions];
            double[] diffStd = new double[this._dimensions];
            double[] diffSkewness = new double[this._dimensions];
            double[] diffKurtosis = new double[this._dimensions];

            List<double[]> signalDiff = new ArrayList<>();

            signalDiff = this.getDiff(signal);

            // Calculating the statistical moments of the difference signal
            for (int i = 0; i < this._dimensions; i++)
            {
                double[] moments = this.getMoments(signalDiff, i);
                diffMean[i] = moments[0];
                diffStd[i] = moments[1];
                diffSkewness[i] = moments[2];
                diffKurtosis[i] = moments[3];
            }

            switch (clip.getType())
            {
            case Clip.ACCELEROMETER:
                features.put(Feature.ACCX_DIFF_MEAN, diffMean[0]);
                features.put(Feature.ACCY_DIFF_MEAN, diffMean[1]);
                features.put(Feature.ACCZ_DIFF_MEAN, diffMean[2]);

                features.put(Feature.ACCX_DIFF_STD, diffStd[0]);
                features.put(Feature.ACCY_DIFF_STD, diffStd[1]);
                features.put(Feature.ACCZ_DIFF_STD, diffStd[2]);

                features.put(Feature.ACCX_DIFF_SKEW, diffSkewness[0]);
                features.put(Feature.ACCY_DIFF_SKEW, diffSkewness[1]);
                features.put(Feature.ACCZ_DIFF_SKEW, diffSkewness[2]);

                features.put(Feature.ACCX_DIFF_KURT, diffKurtosis[0]);
                features.put(Feature.ACCY_DIFF_KURT, diffKurtosis[1]);
                features.put(Feature.ACCZ_DIFF_KURT, diffKurtosis[2]);

                break;
            case Clip.GYROSCOPE:
                features.put(Feature.GYRX_DIFF_MEAN, diffMean[0]);
                features.put(Feature.GYRY_DIFF_MEAN, diffMean[1]);
                features.put(Feature.GYRZ_DIFF_MEAN, diffMean[2]);

                features.put(Feature.GYRX_DIFF_STD, diffStd[0]);
                features.put(Feature.GYRY_DIFF_STD, diffStd[1]);
                features.put(Feature.GYRZ_DIFF_STD, diffStd[2]);

                features.put(Feature.GYRX_DIFF_SKEW, diffSkewness[0]);
                features.put(Feature.GYRY_DIFF_SKEW, diffSkewness[1]);
                features.put(Feature.GYRZ_DIFF_SKEW, diffSkewness[2]);

                features.put(Feature.GYRX_DIFF_KURT, diffKurtosis[0]);
                features.put(Feature.GYRY_DIFF_KURT, diffKurtosis[1]);
                features.put(Feature.GYRZ_DIFF_KURT, diffKurtosis[2]);

                break;
            }
        }

        if (this._hasHist)
        {
            // histogram of zscore values

            int[][] hist = new int[this._dimensions][this._binEdges.length - 1];

            int bin = 0;

            List<double[]> signalZScore = this.getZScore(signal, mean, std);

            for (int i = 0; i < this._dimensions; i++)
            {
                for (int j = 0; j < this._binEdges.length - 1; j++)
                    hist[i][j] = 0;

                // TODO: Pull out into it's own variable: signalZScore.size()

                for (int j = 0; j < signalZScore.size(); j++)
                {
                    bin = (int) ((signalZScore.get(j)[i] - this._binEdges[0]) / (this._binEdges[1] - this._binEdges[0]));

                    if ((bin < this._binEdges.length - 1) && (bin >= 0)) // values
                                                                         // outside
                                                                         // the
                                                                         // range
                                                                         // are
                                                                         // neglected
                        hist[i][bin]++;
                }
            }

            // TODO
            // Add another set of histograms on raw signals (not zscore)
            // TBD also on MATLAB side

            switch (clip.getType())
            {
            case Clip.ACCELEROMETER:
                features.put(Feature.ACCX_HIST1, (double) hist[0][0]);
                features.put(Feature.ACCX_HIST2, (double) hist[0][1]);
                features.put(Feature.ACCX_HIST3, (double) hist[0][2]);
                features.put(Feature.ACCX_HIST4, (double) hist[0][3]);
                features.put(Feature.ACCX_HIST5, (double) hist[0][4]);
                features.put(Feature.ACCX_HIST6, (double) hist[0][5]);

                features.put(Feature.ACCY_HIST1, (double) hist[1][0]);
                features.put(Feature.ACCY_HIST2, (double) hist[1][1]);
                features.put(Feature.ACCY_HIST3, (double) hist[1][2]);
                features.put(Feature.ACCY_HIST4, (double) hist[1][3]);
                features.put(Feature.ACCY_HIST5, (double) hist[1][4]);
                features.put(Feature.ACCY_HIST6, (double) hist[1][5]);

                features.put(Feature.ACCZ_HIST1, (double) hist[2][0]);
                features.put(Feature.ACCZ_HIST2, (double) hist[2][1]);
                features.put(Feature.ACCZ_HIST3, (double) hist[2][2]);
                features.put(Feature.ACCZ_HIST4, (double) hist[2][3]);
                features.put(Feature.ACCZ_HIST5, (double) hist[2][4]);
                features.put(Feature.ACCZ_HIST6, (double) hist[2][5]);

                break;
            case Clip.GYROSCOPE:
                features.put(Feature.GYRX_HIST1, (double) hist[0][0]);
                features.put(Feature.GYRX_HIST2, (double) hist[0][1]);
                features.put(Feature.GYRX_HIST3, (double) hist[0][2]);
                features.put(Feature.GYRX_HIST4, (double) hist[0][3]);
                features.put(Feature.GYRX_HIST5, (double) hist[0][4]);
                features.put(Feature.GYRX_HIST6, (double) hist[0][5]);

                features.put(Feature.GYRY_HIST1, (double) hist[1][0]);
                features.put(Feature.GYRY_HIST2, (double) hist[1][1]);
                features.put(Feature.GYRY_HIST3, (double) hist[1][2]);
                features.put(Feature.GYRY_HIST4, (double) hist[1][3]);
                features.put(Feature.GYRY_HIST5, (double) hist[1][4]);
                features.put(Feature.GYRY_HIST6, (double) hist[1][5]);

                features.put(Feature.GYRZ_HIST1, (double) hist[2][0]);
                features.put(Feature.GYRZ_HIST2, (double) hist[2][1]);
                features.put(Feature.GYRZ_HIST3, (double) hist[2][2]);
                features.put(Feature.GYRZ_HIST4, (double) hist[2][3]);
                features.put(Feature.GYRZ_HIST5, (double) hist[2][4]);
                features.put(Feature.GYRZ_HIST6, (double) hist[2][5]);

                break;
            }
        }

        if (this._hasCross)
        {
            double[] cross = new double[this._dimensions];

            for (int i = 0; i < this._dimensions; i++)
                cross[i] = 0;

            if (this._dimensions == 3)
            {
                cross = this.get3DInnerProds(signal);

                switch (clip.getType())
                {
                case Clip.ACCELEROMETER:
                    features.put(Feature.ACC_CROSS_XY, cross[0]);
                    features.put(Feature.ACC_CROSS_YZ, cross[1]);
                    features.put(Feature.ACC_CROSS_ZX, cross[2]);

                    features.put(Feature.ACC_CROSS_XY_ABS, Math.abs(cross[0]));
                    features.put(Feature.ACC_CROSS_YZ_ABS, Math.abs(cross[1]));
                    features.put(Feature.ACC_CROSS_ZX_ABS, Math.abs(cross[2]));

                    break;
                case Clip.GYROSCOPE:
                    features.put(Feature.GYR_CROSS_XY, cross[0]);
                    features.put(Feature.GYR_CROSS_YZ, cross[1]);
                    features.put(Feature.GYR_CROSS_ZX, cross[2]);

                    features.put(Feature.GYR_CROSS_XY_ABS, Math.abs(cross[0]));
                    features.put(Feature.GYR_CROSS_YZ_ABS, Math.abs(cross[1]));
                    features.put(Feature.GYR_CROSS_ZX_ABS, Math.abs(cross[2]));

                    break;
                }
            }
            else
                Log.e("PR",
                        "FeatureExtractor: Calculating cross-dimensional inner-products for a non-3D signal - values set to zero!");
        }

        if (this._hasNormCross)
        {
            double[] crossNorm = new double[this._dimensions];

            for (int i = 0; i < this._dimensions; i++)
                crossNorm[i] = 0;

            if (this._dimensions == 3)
            {
                crossNorm = this.get3DNormInnerProds(signal);

                switch (clip.getType())
                {
                case Clip.ACCELEROMETER:
                    features.put(Feature.ACC_CROSS_XY_NORM, crossNorm[0]);
                    features.put(Feature.ACC_CROSS_YZ_NORM, crossNorm[1]);
                    features.put(Feature.ACC_CROSS_ZX_NORM, crossNorm[2]);

                    features.put(Feature.ACC_CROSS_XY_NORM_ABS, Math.abs(crossNorm[0]));
                    features.put(Feature.ACC_CROSS_YZ_NORM_ABS, Math.abs(crossNorm[1]));
                    features.put(Feature.ACC_CROSS_ZX_NORM_ABS, Math.abs(crossNorm[2]));

                    break;
                case Clip.GYROSCOPE:
                    features.put(Feature.GYR_CROSS_XY_NORM, crossNorm[0]);
                    features.put(Feature.GYR_CROSS_YZ_NORM, crossNorm[1]);
                    features.put(Feature.GYR_CROSS_ZX_NORM, crossNorm[2]);

                    features.put(Feature.GYR_CROSS_XY_NORM_ABS, Math.abs(crossNorm[0]));
                    features.put(Feature.GYR_CROSS_YZ_NORM_ABS, Math.abs(crossNorm[1]));
                    features.put(Feature.GYR_CROSS_ZX_NORM_ABS, Math.abs(crossNorm[2]));

                    break;
                }
            }
            else
                Log.e("Warning",
                        "Calculating cross-dimensional inner-products for a non-3D signal - values set to zero!");
        }

        // TODO: MAX, MIN, ABS_MAX, ABS_MIN, RMS?

        /*
         * if (hasFFT) Complex[] FFTvalues = fft.transform(signal,
         * TransformType.FORWARD);
         * 
         * // int i=0;
         */

        if (this._hasFFT)
        {
            // TODO: Update & uncomment FFT code below...

            switch (clip.getType())
            {
            case Clip.ACCELEROMETER:
                features.put(Feature.ACCX_FFT1, 0.0);
                features.put(Feature.ACCX_FFT2, 0.0);
                features.put(Feature.ACCX_FFT3, 0.0);
                features.put(Feature.ACCX_FFT4, 0.0);
                features.put(Feature.ACCX_FFT5, 0.0);
                features.put(Feature.ACCX_FFT6, 0.0);
                features.put(Feature.ACCX_FFT7, 0.0);
                features.put(Feature.ACCX_FFT8, 0.0);
                features.put(Feature.ACCX_FFT9, 0.0);
                features.put(Feature.ACCX_FFT10, 0.0);

                features.put(Feature.ACCY_FFT1, 0.0);
                features.put(Feature.ACCY_FFT2, 0.0);
                features.put(Feature.ACCY_FFT3, 0.0);
                features.put(Feature.ACCY_FFT4, 0.0);
                features.put(Feature.ACCY_FFT5, 0.0);
                features.put(Feature.ACCY_FFT6, 0.0);
                features.put(Feature.ACCY_FFT7, 0.0);
                features.put(Feature.ACCY_FFT8, 0.0);
                features.put(Feature.ACCY_FFT9, 0.0);
                features.put(Feature.ACCY_FFT10, 0.0);

                features.put(Feature.ACCZ_FFT1, 0.0);
                features.put(Feature.ACCZ_FFT2, 0.0);
                features.put(Feature.ACCZ_FFT3, 0.0);
                features.put(Feature.ACCZ_FFT4, 0.0);
                features.put(Feature.ACCZ_FFT5, 0.0);
                features.put(Feature.ACCZ_FFT6, 0.0);
                features.put(Feature.ACCZ_FFT7, 0.0);
                features.put(Feature.ACCZ_FFT8, 0.0);
                features.put(Feature.ACCZ_FFT9, 0.0);
                features.put(Feature.ACCZ_FFT10, 0.0);

                break;
            case Clip.GYROSCOPE:
                features.put(Feature.GYRX_FFT1, 0.0);
                features.put(Feature.GYRX_FFT2, 0.0);
                features.put(Feature.GYRX_FFT3, 0.0);
                features.put(Feature.GYRX_FFT4, 0.0);
                features.put(Feature.GYRX_FFT5, 0.0);
                features.put(Feature.GYRX_FFT6, 0.0);
                features.put(Feature.GYRX_FFT7, 0.0);
                features.put(Feature.GYRX_FFT8, 0.0);
                features.put(Feature.GYRX_FFT9, 0.0);
                features.put(Feature.GYRX_FFT10, 0.0);

                features.put(Feature.GYRY_FFT1, 0.0);
                features.put(Feature.GYRY_FFT2, 0.0);
                features.put(Feature.GYRY_FFT3, 0.0);
                features.put(Feature.GYRY_FFT4, 0.0);
                features.put(Feature.GYRY_FFT5, 0.0);
                features.put(Feature.GYRY_FFT6, 0.0);
                features.put(Feature.GYRY_FFT7, 0.0);
                features.put(Feature.GYRY_FFT8, 0.0);
                features.put(Feature.GYRY_FFT9, 0.0);
                features.put(Feature.GYRY_FFT10, 0.0);

                features.put(Feature.GYRZ_FFT1, 0.0);
                features.put(Feature.GYRZ_FFT2, 0.0);
                features.put(Feature.GYRZ_FFT3, 0.0);
                features.put(Feature.GYRZ_FFT4, 0.0);
                features.put(Feature.GYRZ_FFT5, 0.0);
                features.put(Feature.GYRZ_FFT6, 0.0);
                features.put(Feature.GYRZ_FFT7, 0.0);
                features.put(Feature.GYRZ_FFT8, 0.0);
                features.put(Feature.GYRZ_FFT9, 0.0);
                features.put(Feature.GYRZ_FFT10, 0.0);

                break;
            }
        }

        return features;
    }

    private List<double[]> interpolate(List<double[]> signal, List<Long> ts, int freq)
    {
        List<double[]> signalOut = new ArrayList<>();

        if (ts.size() < 2) // CJK TODO: Ok returning uninitialized signalOut?
            return signalOut;

        double stepSize = 1e9 / (double) freq; // step size in nanosec

        // checking if the timestamps are incremental - if not, the datapoint is
        // removed

        List<Long> t2 = new ArrayList<>();

        List<double[]> signal2 = new ArrayList<>();

        t2.add(ts.get(0));

        signal2.add(Arrays.copyOf(signal.get(0), signal.get(0).length));

        // TODO: Pull out into own variable: ts.size()

        for (int j = 1; j < ts.size(); j++)
        {
            // TODO: Pull out into own variable: ts.get(j)
            // TODO: Pull out into own variable: t2.size()

            if (ts.get(j) > t2.get(t2.size() - 1))
            {
                t2.add(ts.get(j));

                // TODO: Pull out into own variable: signal.get(j)

                signal2.add(Arrays.copyOf(signal.get(j), signal.get(j).length));
            }
            else
            {
                Log.e("PR", "FeatureExtractor: Non-increasing timestamp found!");
            }
        }

        // converting time instances to double and getting rid of big numbers
        long tStart = t2.get(0);
        double[] tDouble = new double[signal2.size()];

        // TODO: Pull out into own variable: signal2.size()

        for (int j = 0; j < signal2.size(); j++)
            tDouble[j] = t2.get(j) - tStart;

        // calculating the number of interpolated samples
        int nSamp = (int) Math.floor(tDouble[signal2.size() - 1] / stepSize);

        // creating new, regular time instances

        // TODO: Reminder - pull out into own variable: signal2.size()

        double[] tNew = new double[nSamp];
        for (int j = 0; j < nSamp; j++)
            tNew[j] = tDouble[signal2.size() - 1] - (double) j * stepSize;

        double[][] signalOutTemp = new double[nSamp][this._dimensions];

        for (int i = 0; i < this._dimensions; i++)
        {
            // building a separate array for the current axis
            double[] signal1D = new double[signal2.size()];

            for (int j = 0; j < signal2.size(); j++)
                signal1D[j] = signal2.get(j)[i];

            // spline interpolation
            SplineInterpolator interp = new SplineInterpolator();
            PolynomialSplineFunction func = interp.interpolate(tDouble, signal1D);

            // interpolating onto new instances
            for (int j = 0; j < nSamp; j++)
                signalOutTemp[j][i] = func.value(tNew[j]);
        }

        // TODO: Pull out into own variable: signalOut.size()

        for (int i = 0; i < nSamp; i++)
        {
            signalOut.add(new double[this._dimensions]);
            signalOut.set(signalOut.size() - 1, signalOutTemp[i]);
        }

        return signalOut;
    }

    private List<double[]> getDiff(List<double[]> signal)
    {
        List<double[]> signalDiff = new ArrayList<>();

        // TODO: Pull out into own variable: signal.size()

        for (int i = 0; i < signal.size() - 1; i++)
        {
            double[] sig = signal.get(i);
            double[] sigNext = signal.get(i + 1);

            double[] sigDiff = new double[sig.length];

            for (int j = 0; j < sig.length; j++)
                sigDiff[j] = sigNext[j] - sig[j];

            signalDiff.add(sigDiff);
        }

        return signalDiff;
    }

    private List<double[]> getZScore(List<double[]> signal, double[] mean, double[] std)
    {
        List<double[]> signalZScore = new ArrayList<>();

        // TODO: Pull out into own variable: signal.size()

        for (int i = 0; i < signal.size(); i++)
        {
            double[] sig = signal.get(i);
            double[] sigZScore = new double[this._dimensions];

            for (int j = 0; j < this._dimensions; j++)
                sigZScore[j] = (sig[j] - mean[j]) / std[j];

            signalZScore.add(sigZScore);
        }

        return signalZScore;
    }

    // This method will calculate mean, standard deviation, skewness, and
    // kurtosis.
    // each member of the list is one statistical moment, which consists of an
    // array, with
    // each element accounting for one dimension

    private double[] getMoments(List<double[]> signal, int axis)
    {
        double[][] signalArray = new double[signal.size()][this._dimensions];

        for (int i = 0; i < signalArray.length; i++)
        {
            signalArray[i] = signal.get(i);
        }

        if (signalArray.length < 2)
        {
            double[] out =
            { 0.0, 0.0, 0.0, 0.0 };

            return out;
        }

        double sum = 0.0;

        // For some reason, the following commented-out code generates
        // "concurrent modification exception"!
        // for (double[] value : signal)
        // sum += value[axis];
        // CJK TODO: Solve mystery ^

        for (double[] aSignalArray : signalArray) sum += aSignalArray[axis];

        double mean = sum / signalArray.length;

        double m2 = 0.0;
        double m3 = 0.0;
        double m4 = 0.0;

        double t2 = 0.0;
        double t3 = 0.0;
        double t4 = 0.0;

        for (double[] aSignalArray : signalArray) {
            t2 = (aSignalArray[axis] - mean) * (aSignalArray[axis] - mean);
            m2 += t2;

            t3 = t2 * (aSignalArray[axis] - mean);
            m3 += t3;

            t4 = t3 * (aSignalArray[axis] - mean);
            m4 += t4;
        }

        double std = Math.sqrt(m2 / (signalArray.length - 1)); // unbiased

        m2 /= signalArray.length;
        m3 /= signalArray.length;
        m4 /= signalArray.length;

        double skewness = m3 / (std * std * std); // unbiased

        double kurtosis = m4 / (m2 * m2) - 3; // unbiased

        double out[] =
        { mean, std, skewness, kurtosis };
        return out;
    }

    // overall mean of squares
    private double getOverallMean(List<double[]> signal)
    {
        double[][] signalArray = new double[signal.size()][this._dimensions];

        for (int i = 0; i < signalArray.length; i++)
        {
            signalArray[i] = signal.get(i);
        }

        double ms = 0;

        // TODO: Pull out into own variable: signal.size()

        /*
         * for (int i = 0; i < signal.size(); i++) { for (int j = 0; j <
         * this._dimensions; j++) ms += signal.get(i)[j] * signal.get(i)[j] /
         * this._dimensions; }
         */

        for (double[] aSignalArray : signalArray) {
            for (int j = 0; j < this._dimensions; j++)
                ms += aSignalArray[j] * aSignalArray[j] / this._dimensions;
        }

        ms /= signalArray.length;

        return ms;
    }

    private double getRMS(List<double[]> signal, int axis)
    {
        double rms = 0;

        // TODO: Pull out into own variable: signal.size()

        for (int i = 0; i < signal.size(); i++)
            rms += signal.get(i)[axis] * signal.get(i)[axis];

        rms /= (double) signal.size();

        return rms;
    }

    private double getMax(List<double[]> signal, int axis)
    {
        if (signal.size() == 0) // CJK Question / TODO: Is this always the right
                                // answer?
            return 0;

        double max = signal.get(0)[axis];

        // TODO: Pull out into own variable: signal.size()

        for (int i = 1; i < signal.size(); i++)
        {
            if (max < signal.get(i)[axis])
                max = signal.get(i)[axis];
        }

        return max;
    }

    private double getMin(List<double[]> signal, int axis)
    {
        if (signal.size() == 0) // CJK Question / TODO: Is this always the right
                                // answer?
            return 0;

        double min = signal.get(0)[axis];

        // TODO: Pull out into own variable: signal.size()

        for (int i = 1; i < signal.size(); i++)
        {
            if (min > signal.get(i)[axis])
                min = signal.get(i)[axis];
        }

        return min;
    }

    private double[] get3DInnerProds(List<double[]> signal)
    {
        // This feature only works for 3D signals (acceleration, magnetic field,
        // etc).

        double[] innerProds = new double[3];

        if (this._dimensions != 3)
            return innerProds; // double-check for dimension

        // CJK Question / TODO: what's in the uninitialized double above^?

        innerProds[0] = 0;

        // TODO: Pull out into own variable: signal.size()

        for (int j = 0; j < signal.size(); j++)
            innerProds[0] += signal.get(j)[0] * signal.get(j)[1];
        innerProds[0] /= (double) signal.size(); // mean

        innerProds[1] = 0;
        for (int j = 0; j < signal.size(); j++)
            innerProds[1] += signal.get(j)[1] * signal.get(j)[2];
        innerProds[1] /= (double) signal.size(); // mean

        innerProds[2] = 0;
        for (int j = 0; j < signal.size(); j++)
            innerProds[2] += signal.get(j)[2] * signal.get(j)[0];
        innerProds[2] /= (double) signal.size(); // mean

        return innerProds;
    }

    private double[] get3DNormInnerProds(List<double[]> signal)
    {
        double[] innerProds = new double[3];

        if (this._dimensions != 3)
            return innerProds; // double-check for dimension

        double[] magnitude = new double[signal.size()];

        for (int j = 0; j < signal.size(); j++)
        {
            magnitude[j] = (signal.get(j)[0] * signal.get(j)[0]) + (signal.get(j)[1] * signal.get(j)[1])
                    + (signal.get(j)[2] * signal.get(j)[2]);
        }

        innerProds[0] = 0;
        for (int j = 0; j < signal.size(); j++)
            innerProds[0] += signal.get(j)[0] * signal.get(j)[1] / magnitude[j];
        innerProds[0] /= (double) signal.size(); // mean

        innerProds[1] = 0;
        for (int j = 0; j < signal.size(); j++)
            innerProds[1] += signal.get(j)[1] * signal.get(j)[2] / magnitude[j];
        innerProds[1] /= (double) signal.size(); // mean

        innerProds[2] = 0;
        for (int j = 0; j < signal.size(); j++)
            innerProds[2] += signal.get(j)[2] * signal.get(j)[0] / magnitude[j];
        innerProds[2] /= (double) signal.size(); // mean

        return innerProds;
    }
}