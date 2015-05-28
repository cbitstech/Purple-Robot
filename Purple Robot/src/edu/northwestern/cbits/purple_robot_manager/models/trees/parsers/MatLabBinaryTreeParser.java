package edu.northwestern.cbits.purple_robot_manager.models.trees.parsers;

import java.util.ArrayList;
import java.util.HashMap;

import edu.northwestern.cbits.purple_robot_manager.models.trees.BranchNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.LeafNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;

public class MatLabBinaryTreeParser extends TreeNodeParser
{
    private ArrayList<String> _lines = new ArrayList<>();

    @Override
    public TreeNode parse(String content) throws TreeNodeException
    {
        String[] lines = content.split("\\r?\\n");

        // Add an empty line so that line number = index = node ID.

        this._lines.add("");

        for (String line : lines)
            this._lines.add(line.trim());

        return this.treeForNode(1, "");
    }

    private TreeNode treeForNode(int lineNo, String prefix) throws TreeNodeException
    {
        String line = this._lines.get(lineNo);

        String[] leaf = line.split(" class = ");

        if (leaf.length == 2)
        {
            // leaf[1] = prediction, using "1.0" since no accuracy is available.

            HashMap<String, Object> prediction = new HashMap<>();
            prediction.put(LeafNode.ACCURACY, 1.0);
            prediction.put(LeafNode.PREDICTION, leaf[1]);

            return new LeafNode(prefix + "->" + lineNo, prediction);
        }
        else
        {
            BranchNode branch = new BranchNode(prefix + "->" + lineNo);

            // split the non-leaf line into the two conditions
            String[] branchLines = line.substring(6).split(" else");

            String ifString = branchLines[0];
            String elseString = branchLines[1];

            // At this point, we have 2 strings that look like:
            // "if x10<-0.999999 then node 4"

            ifString = ifString.replace("if ", "");
            ifString = ifString.replace("<", "|");
            ifString = ifString.replace(" then node ", "|");

            // After the above, we should have something like
            // "x10|-0.999999|4"

            elseString = elseString.replace("if ", "");
            elseString = elseString.replace(">=", "|");
            elseString = elseString.replace(" then node ", "|");

            String[] ifTokens = ifString.split("\\|");
            String[] elseTokens = elseString.split("\\|");

            // Turn into conditions & next nodes...
            String nextIfId = ifTokens[2];

            // Create a tree node for the destination node.
            TreeNode nextIfNode = this.treeForNode(Integer.parseInt(nextIfId), prefix + "->" + lineNo);

            // Get the test line components.
            BranchNode.Operation op = BranchNode.Operation.LESS_THAN;
            String feature = ifTokens[0];
            Double value = Double.valueOf(ifTokens[1]);

            branch.addCondition(op, feature.trim(), value, BranchNode.Condition.DEFAULT_PRIORITY, nextIfNode);

            // Create a tree node for the destination node.

            String nextElseId = elseTokens[2];

            TreeNode nextElseNode = this.treeForNode(Integer.parseInt(nextElseId), prefix + "->" + lineNo);

            // Get the test line components.
            BranchNode.Operation elseOp = BranchNode.Operation.MORE_THAN_OR_EQUAL_TO;
            feature = elseTokens[0];
            value = Double.valueOf(elseTokens[1]);

            branch.addCondition(elseOp, feature.trim(), value, BranchNode.Condition.LOWEST_PRIORITY, nextElseNode);

            return branch;
        }
    }
}
