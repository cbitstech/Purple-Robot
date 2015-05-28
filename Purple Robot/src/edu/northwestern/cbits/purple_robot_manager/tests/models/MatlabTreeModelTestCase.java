package edu.northwestern.cbits.purple_robot_manager.tests.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.models.Model;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.models.trees.LeafNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;
import edu.northwestern.cbits.purple_robot_manager.models.trees.parsers.TreeNodeParser;
import edu.northwestern.cbits.purple_robot_manager.models.trees.parsers.TreeNodeParser.ParserNotFound;
import edu.northwestern.cbits.purple_robot_manager.tests.RobotTestCase;

public class MatlabTreeModelTestCase extends RobotTestCase
{
    private static final String MODEL_URI = "file:///android_asset/test_data/matlab-model.json";

    public MatlabTreeModelTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        try
        {
            ArrayList<String> lines = new ArrayList<>();

            StringBuilder sb = new StringBuilder();

            InputStream file = this._context.getAssets().open("test_data/matlab-tree.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(file, "UTF-8"));

            String line = null;

            while ((line = in.readLine()) != null)
            {
                sb.append(line + "\n");
                lines.add(line);
            }

            in.close();

            TreeNode node = TreeNodeParser.parseString(sb.toString());

            HashMap<String, Object> world = new HashMap<>();

            // Outputs class at line 508.
            world.put("x10", -1.0);
            world.put("x91", -1.0);
            world.put("x41", -2.0);
            world.put("x6", -1.0);

            Map<String, Object> prediction = node.fetchPrediction(world);
            Assert.assertEquals("MATLAB1", "3", prediction.get(LeafNode.PREDICTION));

            world.put("x6", 0.0);
            world.put("x30", -1.0);

            prediction = node.fetchPrediction(world);
            Assert.assertEquals("MATLAB2", "6", prediction.get(LeafNode.PREDICTION));

            Assert.assertEquals("MATLAB3",
                    "   1  if x10<-0.464422 then node 2 elseif x10>=-0.464422 then node 3 else 8", lines.get(0));
            Assert.assertEquals("MATLAB4", "1815  class = 1", lines.get(1814));
            Assert.assertEquals("MATLAB5",
                    "1455  if x18<0.26036 then node 1560 elseif x18>=0.26036 then node 1561 else 4", lines.get(1454));
            Assert.assertEquals("MATLAB6",
                    " 894  if x100<1.42992 then node 1092 elseif x100>=1.42992 then node 1093 else 6", lines.get(893));
            Assert.assertEquals("MATLAB7", "   3  if x3<0.392665 then node 6 elseif x3>=0.392665 then node 7 else 8",
                    lines.get(2));
            Assert.assertEquals("MATLAB8",
                    "   9  if x108<-0.0457584 then node 18 elseif x108>=-0.0457584 then node 19 else 6", lines.get(8));

            world.put("x91", 0.0);
            world.put("x98", 0.0);
            world.put("x6", -2.0);
            world.put("x103", -1.0);

            prediction = node.fetchPrediction(world);

            Assert.assertEquals("MATLAB9", "2", prediction.get(LeafNode.PREDICTION));

            world.clear();

            try
            {
                prediction = node.fetchPrediction(world);

                // Should throw exception before getting here...
                Assert.fail("MATLAB100");
            }
            catch (TreeNodeException e)
            {

            }
        }
        catch (ParserNotFound e)
        {
            e.printStackTrace();

            Assert.fail("MATLAB100");
        }
        catch (TreeNodeException e)
        {
            e.printStackTrace();

            Assert.fail("MATLAB101");
        }
        catch (IOException e)
        {
            e.printStackTrace();

            Assert.fail("MATLAB102");
        }

        ModelManager models = ModelManager.getInstance(this._context);

        models.addModel(MatlabTreeModelTestCase.MODEL_URI);

        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {

        }

        Assert.assertNotNull("MATLAB200", models.fetchModelByName(this._context, MatlabTreeModelTestCase.MODEL_URI));
        Assert.assertNull("MATLAB201", models.fetchModelByTitle(this._context, MatlabTreeModelTestCase.MODEL_URI));
        Assert.assertNotNull("MATLAB202", models.fetchModelByTitle(this._context, "Matlab Tree Model Test"));

        HashMap<String, Object> world = new HashMap<>();

        // Outputs class at line 508.
        world.put("x10", -1.0);
        world.put("x91", -1.0);
        world.put("x41", -2.0);
        world.put("x6", -1.0);

        Model matlab = models.fetchModelByTitle(this._context, "Matlab Tree Model Test");

        matlab.predict(this._context, world);

        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {

        }

        Assert.assertEquals("MATLAB203", "3", matlab.latestPrediction(this._context).get(LeafNode.PREDICTION));

        models.deleteModel(MatlabTreeModelTestCase.MODEL_URI);
    }

    public int estimatedMinutes()
    {
        return 1;
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_matlab_tree_model_test);
    }
}