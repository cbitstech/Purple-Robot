package edu.northwestern.cbits.purple_robot_manager.models.trees;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class BranchNode extends TreeNode 
{
	public static class MissingValueException extends TreeNodeException 
	{
		public MissingValueException(String message) 
		{
			super(message);
		}

		private static final long serialVersionUID = 7858585916379498941L;
	}

	private ArrayList<Condition> _conditions = new ArrayList<Condition>();
	
	public static enum Operation 
	{
		LESS_THAN,
		LESS_THAN_OR_EQUAL_TO,
		MORE_THAN,
		MORE_THAN_OR_EQUAL_TO,
		EQUALS,
		EQUALS_CASE_INSENSITIVE,
		CONTAINS,
		CONTAINED_BY,
		STARTS_WITH,
		ENDS_WITH,
		DEFAULT
	}
	
	public static class Condition
	{
		public static final int DEFAULT_PRIORITY = 0;
		public static final int LOWEST_PRIORITY = Integer.MIN_VALUE;
		public static final int HIGHEST_PRIORITY = Integer.MAX_VALUE;

		String _feature = null;
		Object _value = null;
		
		BranchNode.Operation _operation = Operation.DEFAULT;
		
		TreeNode _node = null;
		
		int _priority = Condition.DEFAULT_PRIORITY;

		public Condition(Operation op, String feature, Object value, int priority, TreeNode node) 
		{
			this._operation = op;
			this._feature = feature;
			this._value = value;
			this._priority = priority;
			this._node = node;
		}
		
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			
			sb.append(this._feature);
			sb.append(" ");
			sb.append(this.stringForOperation(this._operation));
			sb.append(" ");
			sb.append(this._value);
			sb.append(" (");
			sb.append(this._priority);
			sb.append(")");
			
			return sb.toString();
		}
		
		private Object stringForOperation(Operation operation) 
		{
			return operation.name();
		}

		public boolean evaluate(Map<String, Object> features) throws TreeNodeException
		{
			Object value = features.get(this._feature);
			
			if (value == null && this._operation != Operation.DEFAULT)
				return false;
			
			switch (this._operation)
			{
				case LESS_THAN:
					return Condition.testLessThan(this._value, value);
				case LESS_THAN_OR_EQUAL_TO:
					return Condition.testLessThanOrEqualTo(this._value, value);
				case MORE_THAN:
					return Condition.testMoreThan(this._value, value);
				case MORE_THAN_OR_EQUAL_TO:
					return Condition.testMoreThanOrEqualTo(this._value, value);
				case EQUALS:
					return Condition.testEquals(this._value, value);
				case EQUALS_CASE_INSENSITIVE:
					return Condition.testEqualsCaseInsensitive(this._value, value);
				case CONTAINS:
					return Condition.testContains(this._value, value);
				case CONTAINED_BY:
					return Condition.testEqualsContainedBy(this._value, value);
				case STARTS_WITH:
					return Condition.testStartsWith(this._value, value);
				case ENDS_WITH:
					return Condition.testEndsWith(this._value, value);
				case DEFAULT:
					return true;
			}
			
			return false;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private static boolean testMoreThanOrEqualTo(Object test, Object value) throws TreeNodeException 
		{
			if (test instanceof Comparable<?> == false)
				throw new TreeNode.TreeNodeException("Test does not implement Comparable.");
			if (value instanceof Comparable<?> == false)
				throw new TreeNode.TreeNodeException("Value does not implement Comparable.");

			Comparable testComparable = (Comparable) test;
			Comparable valueComparable = (Comparable) value;
			
			int result = testComparable.compareTo(valueComparable);
			
			return result != 1;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private static boolean testLessThanOrEqualTo(Object test, Object value) throws TreeNodeException 
		{
			if (test instanceof Comparable<?> == false)
				throw new TreeNode.TreeNodeException("Test does not implement Comparable: " + test);
			if (value instanceof Comparable<?> == false)
				throw new TreeNode.TreeNodeException("Value does not implement Comparable: " + value);

			Comparable testComparable = (Comparable) test;
			Comparable valueComparable = (Comparable) value;
			
			int result = testComparable.compareTo(valueComparable);
			
			return result != -1;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private static boolean testMoreThan(Object test, Object value) throws TreeNodeException 
		{
			if (test instanceof Comparable<?> == false)
				throw new TreeNode.TreeNodeException("Test does not implement Comparable.");
			if (value instanceof Comparable<?> == false)
				throw new TreeNode.TreeNodeException("Value does not implement Comparable.");

			Comparable testComparable = (Comparable) test;
			Comparable valueComparable = (Comparable) value;
			
			int result = testComparable.compareTo(valueComparable);
			
			return result == -1;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private static boolean testLessThan(Object test, Object value) throws TreeNodeException 
		{
			if (test instanceof Comparable<?> == false)
				throw new TreeNode.TreeNodeException("Test does not implement Comparable.");
			if (value instanceof Comparable<?> == false)
				throw new TreeNode.TreeNodeException("Value does not implement Comparable.");

			Comparable testComparable = (Comparable) test;
			Comparable valueComparable = (Comparable) value;
			
			int result = testComparable.compareTo(valueComparable);
			
			return result == 1;
		}

		private static boolean testEquals(Object test, Object value) throws TreeNodeException 
		{
			return test.equals(value);
		}

		private static boolean testEndsWith(Object test, Object value) throws TreeNodeException 
		{
			throw new TreeNode.TreeNodeException("Unimplemented comparison.");
		}

		private static boolean testEqualsCaseInsensitive(Object test, Object value) throws TreeNodeException 
		{
			throw new TreeNode.TreeNodeException("Unimplemented comparison.");
		}

		private static boolean testStartsWith(Object test, Object value) throws TreeNodeException 
		{
			throw new TreeNode.TreeNodeException("Unimplemented comparison.");
		}

		private static boolean testContains(Object test, Object value) throws TreeNodeException 
		{
			throw new TreeNode.TreeNodeException("Unimplemented comparison.");
		}
		
		private static boolean testEqualsContainedBy(Object test, Object value) throws TreeNodeException
		{
			throw new TreeNode.TreeNodeException("Unimplemented comparison.");
		}

		public TreeNode getNode() throws TreeNode.TreeNodeException
		{
			if (this._node == null)
				throw new TreeNode.TreeNodeException("Null tree node encountered.");
				
			return this._node;
		}
	}
	
	public void addCondition(Operation op, String feature, Object value, int priority, TreeNode node)
	{
		this._conditions.add(new Condition(op, feature, value, priority, node));
		
		Collections.sort(this._conditions, new Comparator<Condition>()
		{
			public int compare(Condition one, Condition two) 
			{
				if (one._priority > two._priority)
					return -1;
				else if (one._priority < two._priority)
					return 1;

				return one._operation.compareTo(two._operation);
			}
		});
	}

	public Map<String, Object> fetchPrediction(Map<String, Object> features) throws TreeNode.TreeNodeException
	{
		for (Condition condition : this._conditions)
		{
			if (condition.evaluate(features))
				return condition.getNode().fetchPrediction(features);
		}
		
		throw new TreeNode.TreeNodeException("No matching condition for this set of features. Add a DEFAULT condition perhaps?");
	}

	public String toString(int indent) throws TreeNodeException 
	{
		StringBuffer sb = new StringBuffer();
		
		String newline = System.getProperty("line.separator");
		
		for (Condition condition : this._conditions)
		{
			if (sb.length() > 0)
				sb.append(newline);
			
			for (int i = 0; i < indent; i++)
				sb.append("  ");

			sb.append(condition.toString());
			sb.append(newline);
			
			sb.append(condition.getNode().toString(indent + 1));
		}
		
		int index = -1;
		
		while ((index = sb.indexOf(newline + newline)) != -1)
		{
			System.out.println("REPLACING AT" + index);
			
			sb.replace(index, index + 1, "");
		}
		
		return sb.toString();
	}
}
