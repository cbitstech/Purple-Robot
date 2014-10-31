package edu.northwestern.cbits.purple_robot_manager.models;

import java.security.SecureRandom;
import java.util.Map;

import android.content.Context;
import android.net.Uri;
import edu.northwestern.cbits.purple_robot_manager.R;

/**
 * Simple model that generates random numbers when a prediction is requested.
 * Primarily used as a testing class for the model processing and data
 * pipelines.
 */

public class NoiseModel extends Model
{
    protected static final String NOISE_VALUE = "NOISE_VALUE";
    public static final String TYPE = "noise";

    public NoiseModel(Context context, Uri uri)
    {
        // No initialization needed.
    }

    public String getPreferenceKey()
    {
        return NoiseModel.TYPE;
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_noise_model);
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_noise_model_desc);
    }

    /**
     * Generates and transmits a random prediction for the predicted value and
     * accuracy.
     * 
     * @see edu.northwestern.cbits.purple_robot_manager.models.Model#predict(android.content.Context,
     *      java.util.Map)
     */

    public void predict(final Context context, Map<String, Object> snapshot)
    {
        final NoiseModel me = this;

        Runnable r = new Runnable()
        {
            public void run()
            {
                SecureRandom random = new SecureRandom();
                me.transmitPrediction(context, random.nextDouble(), random.nextDouble());
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.models.NoiseModel";
    }

    public String modelType()
    {
        return NoiseModel.TYPE;
    }
}
