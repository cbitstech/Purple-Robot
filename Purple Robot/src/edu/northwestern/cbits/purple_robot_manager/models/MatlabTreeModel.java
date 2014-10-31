package edu.northwestern.cbits.purple_robot_manager.models;

import java.util.Map;

import android.content.Context;
import android.net.Uri;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;
import edu.northwestern.cbits.purple_robot_manager.models.trees.parsers.MatLabBinaryTreeParser;

public class MatlabTreeModel extends TrainedModel
{
    public static final String TYPE = "matlab-decision-tree";

    private TreeNode _tree = null;

    public MatlabTreeModel(Context context, Uri uri)
    {
        super(context, uri);
    }

    protected void generateModel(Context context, Object model) throws Exception
    {
        this._tree = MatLabBinaryTreeParser.parseString(model.toString());
    }

    protected Object evaluateModel(Context context, Map<String, Object> snapshot)
    {
        try
        {
            return this._tree.fetchPrediction(snapshot);
        }
        catch (TreeNodeException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return null;
    }

    public String modelType()
    {
        return MatlabTreeModel.TYPE;
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_model_tree);
    }

}
