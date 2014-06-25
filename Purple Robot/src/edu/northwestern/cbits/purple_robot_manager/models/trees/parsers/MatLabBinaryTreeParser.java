package edu.northwestern.cbits.purple_robot_manager.models.trees.parsers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import edu.northwestern.cbits.purple_robot_manager.models.trees.BranchNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.LeafNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;

public class MatLabBinaryTreeParser extends TreeNodeParser 
{
    private ArrayList<String> _lines = new ArrayList<String>();

	public static void main(String [] args)
	{
		// Assumes Sohrob's MATLAB model. Model file is in assets folder for future testing...
		
		try 
		{
			String content = FileUtils.readFileToString(new File(args[0]));
			
			TreeNode node = TreeNodeParser.parseString(content);
			System.out.println(node.toString(0));
			
			HashMap<String, Object> world = new HashMap<String, Object>();
			
			Map<String, Object> prediction = node.fetchPrediction(world);
			System.out.println("Expect 1. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 

			world.put("x6", Double.valueOf(-1.0));
			world.put("x41", Double.valueOf(-2.0));
			world.put("x91", Double.valueOf(-1.0));
			world.put("x10", Double.valueOf(-1.0));

			prediction = node.fetchPrediction(world);
			System.out.println("Expect 3. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 

			world.put("x30", Double.valueOf(-1.0));
			world.put("x6", Double.valueOf(1.0));
			
			prediction = node.fetchPrediction(world);
			System.out.println("Expect 6. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 
		} 
		catch (ParserNotFound e) 
		{
			e.printStackTrace();
		} 
		catch (TreeNodeException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

    @Override
	public TreeNode parse(String content) throws TreeNodeException 
	{
	    String[] lines = content.split("\\r?\\n");

	    // Add an empty line so that line number = index = node ID.
	    
	    this._lines.add("");
	    
	    for (String line : lines)
	    	this._lines.add(line.trim());

	    return this.treeForNode(1);
	}
    
	private TreeNode treeForNode(int lineNo) throws TreeNodeException 
	{
		String line = this._lines.get(lineNo);

        String[] leaf = line.split(" class = ");

        if (leaf.length == 2) 
        {
            // leaf[1] = prediction, using "1.0" since no accuracy is available.
        	
        	HashMap<String, Object> prediction = new HashMap<String, Object>();
        	prediction.put(LeafNode.ACCURACY, 1.0);
        	prediction.put(LeafNode.PREDICTION, leaf[1]);

            return new LeafNode(prediction);
        } 
        else 
        {
            BranchNode branch = new BranchNode();
            
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
            TreeNode nextIfNode = this.treeForNode(Integer.parseInt(nextIfId));

            // Get the test line components.
            BranchNode.Operation op = BranchNode.Operation.LESS_THAN;
            String feature = ifTokens[0];
            Double value = Double.valueOf(ifTokens[1]);

            branch.addCondition(op, feature, value, BranchNode.Condition.DEFAULT_PRIORITY, nextIfNode);

            // Create a tree node for the destination node.

            String nextElseId = elseTokens[2];

            TreeNode nextElseNode = this.treeForNode(Integer.parseInt(nextElseId));
            
            branch.addDefaultCondition(nextElseNode);
            
            return branch;
        }
	}
}
