package edu.northwestern.cbits.purple_robot_manager.tests.models;

import junit.framework.Assert;
import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.models.MatlabForestModel;
import edu.northwestern.cbits.purple_robot_manager.models.Model;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.tests.RobotTestCase;

public class MatlabForestModelTestCase extends RobotTestCase
{
    private static final String MODEL_URI = "file:///android_asset/test_data/matlab-forest.json";

    public MatlabForestModelTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    @Override
    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        ModelManager models = ModelManager.getInstance(this._context);

        models.addModel(MatlabForestModelTestCase.MODEL_URI);

        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {

        }

        Assert.assertNotNull("MATFOREST100",
                models.fetchModelByName(this._context, MatlabForestModelTestCase.MODEL_URI));
        Assert.assertNull("MATFOREST101", models.fetchModelByTitle(this._context, MatlabForestModelTestCase.MODEL_URI));

        Assert.assertNotNull("MATFOREST102", models.fetchModelByTitle(this._context, "Matlab Forest Model Test"));

        Model model = models.fetchModelByName(this._context, MatlabForestModelTestCase.MODEL_URI);

        Assert.assertEquals("MATFOREST103", model.getClass().getCanonicalName(),
                MatlabForestModel.class.getCanonicalName());

        Assert.assertEquals("MATFOREST103", "x17", model.mappedFeatureName("p20featuresprobe_accx_fft_1"));

        // Commented out to test P20 feature extractor...
        // models.deleteModel(MatlabForestModelTestCase.MODEL_URI);
    }

    @Override
    public int estimatedMinutes()
    {
        return 1;
    }

    @Override
    public String name(Context context)
    {
        return context.getString(R.string.name_matlab_forest_model_test);
    }
}