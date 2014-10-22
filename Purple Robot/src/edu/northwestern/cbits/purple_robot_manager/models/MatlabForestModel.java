package edu.northwestern.cbits.purple_robot_manager.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.net.Uri;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.models.trees.LeafNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;
import edu.northwestern.cbits.purple_robot_manager.models.trees.parsers.TreeNodeParser;
import edu.northwestern.cbits.purple_robot_manager.models.trees.parsers.TreeNodeParser.ParserNotFound;

public class MatlabForestModel extends WekaTreeModel {
    public static final String TYPE = "matlab-forest";

    private static final String VOTES = "VOTES";
    private static final String TREE_COUNT = "TOTAL_VOTERS";

    private ArrayList<TreeNode> _trees = new ArrayList<TreeNode>();

    public MatlabForestModel(Context context, Uri uri) {
        super(context, uri);
    }

    protected void generateModel(Context context, Object model) {
        synchronized (this) {
            if (model instanceof JSONArray) {
                JSONArray modelArray = (JSONArray) model;

                for (int i = 0; i < modelArray.length(); i++) {
                    try {
                        Object modelItem = modelArray.get(i);

                        if (modelItem instanceof String) {
                            try {
                                TreeNode tree = TreeNodeParser
                                        .parseString(modelItem.toString());
                                this._trees.add(tree);
                            } catch (ParserNotFound e) {
                                LogManager.getInstance(context).logException(e);
                            } catch (TreeNodeException e) {
                                LogManager.getInstance(context).logException(e);
                            }
                        }
                    } catch (JSONException e) {
                        LogManager.getInstance(context).logException(e);
                    }
                }
            }
        }
    }

    protected Object evaluateModel(Context context, Map<String, Object> snapshot) {
        String maxPrediction = null;
        int maxCount = -1;

        synchronized (this) {
            Map<String, Integer> counts = new HashMap<String, Integer>();

            for (TreeNode tree : this._trees) {
                try {
                    Map<String, Object> prediction = tree
                            .fetchPrediction(snapshot);

                    String treePrediction = prediction.get(LeafNode.PREDICTION)
                            .toString();

                    Integer count = 0;

                    if (counts.containsKey(treePrediction))
                        count = counts.get(treePrediction);

                    count = Integer.valueOf(count.intValue() + 1);
                    counts.put(treePrediction.toString(), count);
                } catch (TreeNode.TreeNodeException e) {
                    // e.printStackTrace();
                } catch (Exception e) {
                    LogManager.getInstance(context).logException(e);
                }
            }

            for (String prediction : counts.keySet()) {
                Integer count = counts.get(prediction);

                if (count.intValue() > maxCount) {
                    maxCount = count.intValue();
                    maxPrediction = prediction;
                }
            }
        }

        HashMap<String, Object> prediction = new HashMap<String, Object>();
        prediction.put(LeafNode.PREDICTION, maxPrediction);
        prediction.put(LeafNode.ACCURACY, (double) maxCount
                / (double) this._trees.size());
        prediction.put(MatlabForestModel.VOTES, maxCount);
        prediction.put(MatlabForestModel.TREE_COUNT, this._trees.size());

        return prediction;
    }

    public String summary(Context context) {
        return context.getString(R.string.summary_model_forest);
    }

    public String modelType() {
        return MatlabForestModel.TYPE;
    }
}
