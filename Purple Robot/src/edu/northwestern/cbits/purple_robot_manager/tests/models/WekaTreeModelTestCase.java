package edu.northwestern.cbits.purple_robot_manager.tests.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.models.trees.LeafNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;
import edu.northwestern.cbits.purple_robot_manager.models.trees.parsers.TreeNodeParser;
import edu.northwestern.cbits.purple_robot_manager.models.trees.parsers.TreeNodeParser.ParserNotFound;
import edu.northwestern.cbits.purple_robot_manager.tests.RobotTestCase;

public class WekaTreeModelTestCase extends RobotTestCase
{
    public WekaTreeModelTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        try
        {
            StringBuilder sb = new StringBuilder();

            InputStream file = this._context.getAssets().open("test_data/weka-tree.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(file, "UTF-8"));

            String line = null;

            while ((line = in.readLine()) != null)
            {
                sb.append(line + "\n");
            }

            in.close();

            TreeNode node = TreeNodeParser.parseString(sb.toString());
            System.out.println(node.toString(0));

            HashMap<String, Object> world = new HashMap<>();

            Map<String, Object> prediction = node.fetchPrediction(world);
            // System.out.println("Expect alone. Got " +
            // prediction.get(LeafNode.PREDICTION) + " // " +
            // prediction.get(LeafNode.ACCURACY) + ".");
            Assert.assertEquals("WTT001", "alone", prediction.get(LeafNode.PREDICTION));

            world.put("robothealthprobe_cpu_usage", 0.1);

            prediction = node.fetchPrediction(world);
            // System.out.println("Expect alone. Got " +
            // prediction.get(LeafNode.PREDICTION) + " // " +
            // prediction.get(LeafNode.ACCURACY) + ".");
            Assert.assertEquals("WTT002", "alone", prediction.get(LeafNode.PREDICTION));

            world.put("wifiaccesspointsprobe_current_ssid", "blerg");
            prediction = node.fetchPrediction(world);
            // System.out.println("Expect partner. Got " +
            // prediction.get(LeafNode.PREDICTION) + " // " +
            // prediction.get(LeafNode.ACCURACY) + ".");
            Assert.assertEquals("WTT003", "partner", prediction.get(LeafNode.PREDICTION));

            world.put("wifiaccesspointsprobe_current_ssid", "0x");
            world.put("wifiaccesspointsprobe_access_point_count", (double) 20);
            prediction = node.fetchPrediction(world);
            // System.out.println("Expect strangers. Got " +
            // prediction.get(LeafNode.PREDICTION) + " // " +
            // prediction.get(LeafNode.ACCURACY) + ".");
            Assert.assertEquals("WTT004", "strangers", prediction.get(LeafNode.PREDICTION));

            world.put("wifiaccesspointsprobe_current_ssid", "northwestern");
            prediction = node.fetchPrediction(world);
            // System.out.println("Expect acquaintances. Got " +
            // prediction.get(LeafNode.PREDICTION) + " // " +
            // prediction.get(LeafNode.ACCURACY) + ".");
            Assert.assertEquals("WTT005", "acquaintances", prediction.get(LeafNode.PREDICTION));

            world.put("runningsoftwareproberunning_tasks_running_tasks_package_name", "comandroidlauncher");
            prediction = node.fetchPrediction(world);
            // System.out.println("Expect alone. Got " +
            // prediction.get(LeafNode.PREDICTION) + " // " +
            // prediction.get(LeafNode.ACCURACY) + ".");
            Assert.assertEquals("WTT006", "alone", prediction.get(LeafNode.PREDICTION));
        }
        catch (ParserNotFound e)
        {
            e.printStackTrace();
            Assert.fail("WTT007");
        }
        catch (TreeNodeException e)
        {
            e.printStackTrace();
            Assert.fail("WTT008");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Assert.fail("WTT009");
        }
    }

    public int estimatedMinutes()
    {
        return 1;
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_weka_tree_model_test);
    }
}
