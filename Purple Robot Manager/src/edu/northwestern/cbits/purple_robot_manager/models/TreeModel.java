package edu.northwestern.cbits.purple_robot_manager.models;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Id;
import com.alexmerz.graphviz.objects.Node;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

// See: http://www.alexander-merz.com/graphviz/

public class TreeModel extends TrainedModel 
{
	public static final String TYPE = "decision-tree";
	
	private Graph _tree = null;
	
	public TreeModel(Context context, Uri uri) 
	{
		super(context, uri);
	}

	protected void generateModel(Context context, String modelString) 
	{
		StringReader reader = new StringReader(modelString);
		
		Parser parser = new Parser();
		
		try 
		{
			if (parser.parse(reader))
			{
				ArrayList<Graph> graphs = parser.getGraphs();
				
				if (graphs.size() > 0)
					this._tree = graphs.get(0);
			}
		}
		catch (ParseException e) 
		{
			LogManager.getInstance(context).logException(e);
		}
	}

	protected Object evaluateModel(Context context, HashMap<String, Object> snapshot) 
	{
		if (this._tree == null)
			return null;
		
		Id rootId = new Id();
		rootId.setId("N0");
		
		Node root = this._tree.findNode(rootId);
					
		return this.fetchPrediction(root, this._tree.getEdges(), snapshot);
	}

	private String fetchPrediction(Node node, List<Edge> edges, HashMap<String, Object> snapshot) 
	{
		synchronized(this)
		{
			String nodeLabel = node.getAttribute("label").replaceAll("_", "");
	
			List<Edge> testEdges = new ArrayList<Edge>();
			
			for (Edge edge : edges)
			{
				if (edge.getSource().getNode() == node)
					testEdges.add(edge);
			}
			
			if (testEdges.size() == 0)
			{
				int index = nodeLabel.indexOf(" ");

				String prediction = nodeLabel;

				if (index != -1)
					prediction = prediction.substring(0, index);
				
				return prediction;
			}
	
			for (String key : snapshot.keySet())
			{
				String testKey = key.replaceAll("_", "");
				
				if (testKey.equalsIgnoreCase(nodeLabel))
				{
					Object value = snapshot.get(key);

					double testValue = Double.NaN;

					if (value instanceof Integer)
						testValue = ((Integer) value).doubleValue();
					else if (value instanceof Double)
						testValue = ((Double) value).doubleValue();
					else if (value instanceof Float)
						testValue = ((Float) value).doubleValue();
					else if (value instanceof Long)
						testValue = ((Long) value).doubleValue();
					
					Node nextNode = null;

					for (Edge edge : testEdges)
					{
						String edgeLabel = edge.getAttribute("label");
						
						int index = edgeLabel.indexOf(" ");
						
						String operation = edgeLabel.substring(0, index);
						String edgeValue = edgeLabel.substring(index + 1);
						
						if (Double.isNaN(testValue) == false)
						{
							double edgeQuantity = Double.parseDouble(edgeValue);
							
							if ("<=".equals(operation))
							{
								if (testValue <= edgeQuantity)
									nextNode = edge.getTarget().getNode();
							}
							else if (">".equals(operation))
							{
								if (testValue > edgeQuantity)
									nextNode = edge.getTarget().getNode();
							}
							else
								Log.e("PR", "UNKNOWN OP: -" + operation + "-");
						}
						else if ("=".equals(operation))
						{
							String valueString = value.toString().replaceAll("\\.",  "");
							// ^ TODO: Normalize
							
							if (nextNode == null && "= ?".equals(edgeLabel))
								nextNode = edge.getTarget().getNode();
							else if (valueString.equalsIgnoreCase(edgeValue))
								nextNode = edge.getTarget().getNode();
						}
						else
							Log.e("PR", "UNKNOWN OP: -" + operation + "-");
					}
					
					if (nextNode != null)
						return this.fetchPrediction(nextNode, edges, snapshot);
				}
			}
		}		
		
		return null;
	}
	
	public String modelType() 
	{
		return TreeModel.TYPE;
	}
 }
