package edu.northwestern.cbits.purple_robot_manager.models;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Id;
import com.alexmerz.graphviz.objects.Node;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Implemements a trained decision tree model encoded using GraphViz generated 
 * by Weka:
 * 
 * http://www.alexander-merz.com/graphviz/
 * 
 * Note that this class does not train the model, but instead expects the 
 * representation of a model already trained.
 * 
 * A sample tree representation (spaces added for readability:
 * 
 * <pre>{@code
 * digraph J48Tree {
 * N0 [label="telephonyprobe_psc" ]
 * N0->N1 [label="<= 305"]
 *     N1 [label="weatherundergroundfeature_visibility" ]
 *     N1->N2 [label="<= 8"]
 *         N2 [label="colleagues (2.0/1.0)" shape=box style=filled ]
 *     N1->N3 [label="> 8"]
 *         N3 [label="wifiaccesspointsprobe_access_point_count" ]
 *         N3->N4 [label="<= 11"]
 *             N4 [label="sunrisesunsetfeature_sunrise_distance" ]
 *             N4->N5 [label="<= 28478000"]
 *                 N5 [label="family (7.0)" shape=box style=filled ]
 *             N4->N6 [label="> 28478000"]
 *                 N6 [label="weatherundergroundfeature_temperature" ]
 *                 N6->N7 [label="<= 25.3"]
 *                     N7 [label="locationprobe_bearing" ]
 *                     N7->N8 [label="<= 91.699997"]
 *                         N8 [label="friends (3.0/1.0)" shape=box style=filled ]
 *                     N7->N9 [label="> 91.699997"]
 *                         N9 [label="alone (11.0/2.0)" shape=box style=filled ]
 *                 N6->N10 [label="> 25.3"]
 *                     N10 [label="colleagues (2.0)" shape=box style=filled ]
 *         N3->N11 [label="> 11"]
 *             N11 [label="wifiaccesspointsprobe_current_rssi" ]
 *             N11->N12 [label="<= -64"]
 *                  N12 [label="friends (6.0/1.0)" shape=box style=filled ]
 *             N11->N13 [label="> -64"]
 *                  N13 [label="family (6.0)" shape=box style=filled ]
 * N0->N14 [label="> 305"]
 *     N14 [label="other (4.0/1.0)" shape=box style=filled ]}}</pre>
 */

public class TreeModel extends TrainedModel 
{
	public static final String TYPE = "decision-tree";
	
	private Graph _tree = null;
	
	public TreeModel(Context context, Uri uri) 
	{
		super(context, uri);
	}

	
	/**
	 * Parses Graph object from the string provided in model to generate the 
	 * data structure that evaluates data to generate predictions.
	 * 
	 * @see http://www.alexander-merz.com/graphviz/doc/com/alexmerz/graphviz/objects/Graph.html
	 * @see edu.northwestern.cbits.purple_robot_manager.models.TrainedModel#generateModel(android.content.Context, java.lang.Object)
	 */
	
	protected void generateModel(Context context, Object model) 
	{
		StringReader reader = new StringReader(model.toString());
		
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

	
	/**
	 * Finds the root node of the tree and begins evaluating the model.
	 * @see edu.northwestern.cbits.purple_robot_manager.models.TrainedModel#evaluateModel(android.content.Context, java.util.Map)
	 */
	
	protected Object evaluateModel(Context context, Map<String, Object> snapshot) 
	{
		if (this._tree == null)
			return null;
		
		Id rootId = new Id();
		rootId.setId("N0");
		
		Node root = this._tree.findNode(rootId);
					
		return this.fetchPrediction(root, this._tree.getEdges(), snapshot);
	}

	/**
	 * Evaluates the state of the world in relation to the provided node. Based
	 * on the value of the node, recursively passes control to the next node in
	 * the evaluation sequence until encountering a leaf node containing the 
	 * prediction. The prediction value is returned up the tree and becomes the
	 * final prediction for the model. Returns null if an error is encountered
	 * or data needed to evaluate the model is missing.
	 *  
	 * @param node Node object representing the current location in the tree.
	 * @param edges Edges of the graph connecting nodes. Encodes comparison 
	 *              operators.
	 * @param snapshot States used to generate prediction.
	 * 
	 * @return Prediction given states.
	 */
	
	protected String fetchPrediction(Node node, List<Edge> edges, Map<String, Object> snapshot) 
	{
		synchronized(this)
		{
			String nodeLabel = node.getAttribute("label").replaceAll("_", "");
			
			String[] tokens = nodeLabel.split(" ");
			
			nodeLabel = tokens[tokens.length - 1];
	
			List<Edge> testEdges = new ArrayList<Edge>();
			
			for (Edge edge : edges)
			{
				if (edge.getSource().getNode() == node)
					testEdges.add(edge);
			}
			
			if (testEdges.size() == 0)
			{
				String prediction = node.getAttribute("label");
				
				int colonIndex = prediction.indexOf(":");
				
				prediction = prediction.substring(colonIndex + 1).trim();

				int index = prediction.indexOf(" ");

				if (index != -1)
					prediction = prediction.substring(0, index).trim();

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
						String edgeLabel = edge.getAttribute("label").trim();

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
							else if (">=".equals(operation))
							{
								if (testValue >= edgeQuantity)
									nextNode = edge.getTarget().getNode();
							}
							else if (">".equals(operation))
							{
								if (testValue > edgeQuantity)
									nextNode = edge.getTarget().getNode();
							}
							else if ("<".equals(operation))
							{
								if (testValue < edgeQuantity)
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
	
	public String summary(Context context)
	{
		return context.getString(R.string.summary_model_tree);
	}

 }
