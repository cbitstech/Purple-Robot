package edu.northwestern.cbits.purple_robot_manager.models.trees.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.models.trees.BranchNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.BranchNode.Condition;
import edu.northwestern.cbits.purple_robot_manager.models.trees.BranchNode.Operation;
import edu.northwestern.cbits.purple_robot_manager.models.trees.LeafNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;

public class WekaJ48TreeParser extends TreeNodeParser 
{
	private static final String NUM_INSTANCES = "num_instances";
	private static final String NUM_INCORRECT = "num_incorrect";
	
	ArrayList<String> _lines = new ArrayList<String>();
	
	public static void main(String [] args)
	{
		try 
		{
			TreeNode node = TreeNodeParser.parseString("digraph J48Tree {\nN0 [label=\"wifiaccesspointsprobe_current_ssid\" ]\nN0->N1 [label=\"= ?\"]\nN1 [label=\"alone (0.0)\" shape=box style=filled ]\nN0->N2 [label=\"= penismangina\"]\nN2 [label=\"alone (8.41/2.0)\" shape=box style=filled ]\nN0->N3 [label=\"= 0x\"]\nN3 [label=\"robothealthprobe_cpu_usage\" ]\nN3->N4 [label=\"<= 0.142857\"]\nN4 [label=\"wifiaccesspointsprobe_access_point_count\" ]\nN4->N5 [label=\"<= 17\"]\nN5 [label=\"acquaintances (2.1/1.1)\" shape=box style=filled ]\nN4->N6 [label=\"> 17\"]\nN6 [label=\"strangers (2.1/0.1)\" shape=box style=filled ]\nN3->N7 [label=\"> 0.142857\"]\nN7 [label=\"alone (5.26/2.0)\" shape=box style=filled ]\nN0->N8 [label=\"= blerg\"]\nN8 [label=\"partner (1.05/0.05)\" shape=box style=filled ]\nN0->N9 [label=\"= northwestern\"]\nN9 [label=\"runningsoftwareproberunning_tasks_running_tasks_package_name\" ]\nN9->N10 [label=\"= ?\"]\nN10 [label=\"acquaintances (0.0)\" shape=box style=filled ]\nN9->N11 [label=\"= comcbitsmobilyze_pro\"]\nN11 [label=\"acquaintances (2.1/0.1)\" shape=box style=filled ]\nN9->N12 [label=\"= comandroidlauncher\"]\nN12 [label=\"alone (3.15/1.0)\" shape=box style=filled ]\nN9->N13 [label=\"= edunorthwesterncbitspurple_robot_manager\"]\nN13 [label=\"acquaintances (16.82/7.82)\" shape=box style=filled ]\n}\n");
			System.out.println(node.toString(0));
			
			HashMap<String, Object> world = new HashMap<String, Object>();
			
			Map<String, Object> prediction = node.fetchPrediction(world);
			System.out.println("Expect alone. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 

			world.put("robothealthprobe_cpu_usage", Double.valueOf(0.1));

			prediction = node.fetchPrediction(world);
			System.out.println("Expect alone. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 

			world.put("wifiaccesspointsprobe_current_ssid", "blerg");
			prediction = node.fetchPrediction(world);
			System.out.println("Expect partner. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 

			world.put("wifiaccesspointsprobe_current_ssid", "0x");
			world.put("wifiaccesspointsprobe_access_point_count", Double.valueOf(20));
			prediction = node.fetchPrediction(world);
			System.out.println("Expect strangers. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 

			world.put("wifiaccesspointsprobe_current_ssid", "northwestern");
			prediction = node.fetchPrediction(world);
			System.out.println("Expect acquaintances. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 

			world.put("runningsoftwareproberunning_tasks_running_tasks_package_name", "comandroidlauncher");
			prediction = node.fetchPrediction(world);
			System.out.println("Expect alone. Got " + prediction.get(LeafNode.PREDICTION) + " // " + prediction.get(LeafNode.ACCURACY) + "."); 

		} 
		catch (ParserNotFound e) 
		{
			e.printStackTrace();
		} 
		catch (TreeNodeException e) 
		{
			e.printStackTrace();
		}
	}

	public TreeNode parse(String content) throws TreeNodeException 
	{
		for (String line : content.split("\\r?\\n"))
		{
			if (line.startsWith("digraph J48Tree"))
			{
				// Start
			}
			else if (line.startsWith("}"))
			{
				// End
			}
			else
				this._lines.add(line.trim());
	}
		
		return this.treeForNode("N0");
	}

	private TreeNode treeForNode(String id) throws TreeNodeException 
	{
		for (String line : this._lines)
		{
			if (line.startsWith(id + " [label="))
			{
				if (line.contains("shape=box"))
					return this.leafNodeForLine(line);
				else
				{
					BranchNode branch = new BranchNode();
					
					String[] tokens = line.split("\\\"");

					String feature = tokens[1];
					
					for (String edgeLine : this._lines)
					{
						if (edgeLine.startsWith(id + "->"))
						{
							edgeLine = edgeLine.replace( id + "->", "");
							edgeLine = edgeLine.replace(" [label=\"", "|");
							edgeLine = edgeLine.replace(" [label=\"", "|");
							edgeLine = edgeLine.replace(" ", "|");
							edgeLine = edgeLine.replace("\"]", "|");

							String[] edgeTokens = edgeLine.split("\\|");
							
							String nextId = edgeTokens[0];
							
							TreeNode nextNode = this.treeForNode(nextId);
							String comparison = edgeTokens[1];
							String value = edgeTokens[2];
							
							if ("=".equals(comparison))
							{
								if ("?".equals(value))
									branch.addCondition(Operation.DEFAULT, feature, value, Condition.LOWEST_PRIORITY, nextNode);
								else
									branch.addCondition(Operation.EQUALS, feature, value, Condition.DEFAULT_PRIORITY, nextNode);
							}
							else
							{
								if ("<=".equals(comparison))
									branch.addCondition(Operation.LESS_THAN_OR_EQUAL_TO, feature, Double.valueOf(value), Condition.DEFAULT_PRIORITY, nextNode);
								else if (">".equals(comparison))
									branch.addCondition(Operation.MORE_THAN, feature, Double.valueOf(value), Condition.DEFAULT_PRIORITY, nextNode);
								else if (">=".equals(comparison))
									branch.addCondition(Operation.MORE_THAN_OR_EQUAL_TO, feature, Double.valueOf(value), Condition.DEFAULT_PRIORITY, nextNode);
								else if ("<".equals(comparison))
									branch.addCondition(Operation.LESS_THAN, feature, Double.valueOf(value), Condition.DEFAULT_PRIORITY, nextNode);
							}
						}
					}
					
					return branch;
				}
			}
		}
		
		throw new TreeNode.TreeNodeException("Unable to find definition for node with ID '" + id + "'.");
	}

	private TreeNode leafNodeForLine(String line) 
	{
		String[] tokens = line.split("\\\"");
		
		String label = tokens[1];
		String[] labelTokens = label.split(" \\("); 
		
		HashMap<String, Object> prediction = new HashMap<String, Object>();
		prediction.put(LeafNode.PREDICTION, labelTokens[0]);
		
		String remainder = labelTokens[1].substring(0, labelTokens[1].length() - 1);
		
		double accuracy = 1.0;
		double coverage = 0;
		double incorrect = 0;
				
		if (remainder.contains("/"))
		{
			String[] remainderTokens = remainder.split("/");
			
			coverage = Double.parseDouble(remainderTokens[0]);
			incorrect = Double.parseDouble(remainderTokens[1]);
			
			accuracy = (coverage - incorrect) / coverage; 
		}
		else
			coverage = Double.parseDouble(remainder);
		
		prediction.put(LeafNode.PREDICTION, labelTokens[0]);
		prediction.put(LeafNode.ACCURACY, Double.valueOf(accuracy));
		prediction.put(WekaJ48TreeParser.NUM_INSTANCES, Double.valueOf(coverage));
		prediction.put(WekaJ48TreeParser.NUM_INCORRECT, Double.valueOf(coverage));
		
		return new LeafNode(prediction);
	}
}
