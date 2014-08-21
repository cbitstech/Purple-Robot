package edu.northwestern.cbits.purple_robot_manager.models;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.net.Uri;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Id;
import com.alexmerz.graphviz.objects.Node;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class MatlabForestModel extends WekaTreeModel
{
	// TODO
	
	public static final String TYPE = "matlab-forest";

	private Map<String, Graph> _trees = new HashMap<String, Graph>();

	public MatlabForestModel(Context context, Uri uri) 
	{
		super(context, uri);
	}

	private String findRootId(String model) 
	{
		String rootId = null;
		
		for (String line : model.split("[\r\n]+"))
		{
			if (line.contains("[label=\"1: "))
			{
				String[] tokens = line.split(" ");
				if (tokens.length > 1)
					return tokens[0];
			}
		}
		
		return rootId;
	}

	protected void generateModel(Context context, Object model) 
	{
		synchronized(this)
		{
			if (model instanceof JSONArray)
			{
				JSONArray modelArray = (JSONArray) model;
				
				for (int i = 0; i < modelArray.length(); i++)
				{
					try 
					{
						Object modelItem = modelArray.get(i);
	
						if (modelItem instanceof String)
						{
							String root = this.findRootId(modelItem.toString());
							
							StringReader reader = new StringReader(modelItem.toString());
							
							Parser parser = new Parser();
							
							try 
							{
								if (parser.parse(reader))
								{
									ArrayList<Graph> graphs = parser.getGraphs();
									
									if (graphs.size() > 0)
									{
										Graph tree = graphs.get(0);
										
										this._trees.put(root, tree);
									}
								}
							}
							catch (ParseException e) 
							{
								LogManager.getInstance(context).logException(e);
							}
						}
					} 
					catch (JSONException e) 
					{
						LogManager.getInstance(context).logException(e);
					}
				}
			}
		}
	}

	protected Object evaluateModel(Context context, HashMap<String, Object> snapshot) 
	{
		String maxPrediction = null;

		synchronized(this)
		{
			Map<String, Integer> counts = new HashMap<String, Integer>();
			
			for (String id : this._trees.keySet())
			{
				Graph tree = this._trees.get(id);
				
				Id rootId = new Id();
				rootId.setId(id);
				
				Node root = tree.findNode(rootId);
				
				Object treePrediction = this.fetchPrediction(root, tree.getEdges(), snapshot);
				
				if (treePrediction != null)
				{
					Integer count = 0;
					
					if (counts.containsKey(treePrediction))
						count = counts.get(treePrediction);
					
					count = Integer.valueOf(count.intValue() + 1);
					counts.put(treePrediction.toString(), count);
				}
			}
			
			int maxCount = -1;
	
			for (String prediction : counts.keySet())
			{
				Integer count = counts.get(prediction);
				
				if (count.intValue() > maxCount)
				{
					maxCount = count.intValue();
					maxPrediction = prediction;
				}
			}
		}
		
		return maxPrediction;
	}
	
	public String summary(Context context)
	{
		return context.getString(R.string.summary_model_forest);
	}

	public String modelType() 
	{
		return MatlabForestModel.TYPE;
	}
}
