package edu.northwestern.cbits.purple_robot_manager.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

public class DBSCAN 
{
	private double _minDistance = 0;
	private int _minPopulation = 0;
	
	private HashSet<Point> _points = new HashSet<Point>();
	
	public class Point
	{
		private double _x = 0;
		private double _y = 0;
		private Cluster _cluster = null;
		
		public Point(double x, double y)
		{
			this._x = x;
			this._y = y;
		}
		
		public double x()
		{
			return this._x;
		}
		
		public double y()
		{
			return this._y;
		}

		public int hashCode() 
	    {
	    	return (this._x + "." + this._y).hashCode();
	    }
	    
	    public boolean equals(Object o)
	    {
	    	if (o instanceof Point)
	    	{
	    		Point p = (Point) o;
	    		
	    		return (p._x == this._x) && (p._y == this._y);
	    	}
	    	
	    	return false;
	    }

		public void setCluster(Cluster cluster) 
		{
			this._cluster = cluster;
		}
		
		public double distanceFrom(Point p)
		{
			double xDelta = p._x - this._x;
			double yDelta = p._y - this._y;
			
			return Math.sqrt((xDelta * xDelta) + (yDelta * yDelta));
		}
		
		public Cluster getCluster()
		{
			return this._cluster;
		}

		public Cluster cluster(Point other) 
		{
			Cluster cluster = null;
			
			if (this._cluster == null && other._cluster == null)
			{
				cluster = new Cluster();
				cluster.addPoint(this);
				cluster.addPoint(other);
			}
			else if (this._cluster != null && other._cluster != null)
			{
				cluster = this._cluster;
				
				if (this._cluster != other._cluster)
					this._cluster.assimilate(other._cluster);
			}
			else if (this._cluster != null)
			{
				cluster = this._cluster;
				cluster.addPoint(other);
			}
			else if (other._cluster != null)
			{
				cluster = other._cluster;
				cluster.addPoint(this);
			}
			
			return cluster;
		}
	}
	
	public class Cluster
	{
		private HashSet<Point> _points = new HashSet<Point>();
		private String _name = null;
		
		public void addPoint(Point p)
		{
			this._points.add(p);
			
			p.setCluster(this);
		}
		
		public int population()
		{
			return this._points.size();
		}
		
		public void assimilate(Cluster c)
		{
			for (Point p : c.getPoints())
			{
				p.setCluster(this);
			}
			
			c.clear();
		}
		
		public Collection<Point> getPoints()
		{
			return this._points;
		}
		
		public void clear()
		{
			this._points.clear();
		}

		public String getName() 
		{
			return this._name;
		}

		public void setName(String name) 
		{
			this._name = name;
		}
	}
	
	public DBSCAN(double distance, int population)
	{
		this._minDistance = distance;
		this._minPopulation = population;
	}
	
	public void addPoint(Point p)
	{
		this._points.add(p);
	}
	
	public Collection<Cluster> calculate()
	{
		HashSet<Cluster> clusters = new HashSet<Cluster>();
		
		Point[] xPoints = this._points.toArray(new Point[0]);
		Point[] yPoints = this._points.toArray(new Point[0]);
		
		Arrays.sort(xPoints, new Comparator<Point>()
		{
			public int compare(Point one, Point two) 
			{
				if (one._x > two._x)
					return 1;
				else if (one._x < two._x)
					return -1;

				return 0;
			}
		});
		
		for (int i = 0; i < xPoints.length; i++)
		{
			Point here = xPoints[i];
			Point back = null;
			Point next = null;
			
			if (i > 0)
				back = xPoints[i - 1];
			
			if (i < xPoints.length - 1)
				next = xPoints[i + 1];
			
			if (back != null && Math.abs(here._x - back._x) < this._minDistance)
			{
				if (here.distanceFrom(back) < this._minDistance)
					clusters.add(here.cluster(back));
			}

			if (next != null && Math.abs(here._x - next._x) < this._minDistance)
			{
				if (here.distanceFrom(next) < this._minDistance)
					clusters.add(here.cluster(next));
			}
		}
		
		Arrays.sort(yPoints, new Comparator<Point>()
		{
			public int compare(Point one, Point two) 
			{
				if (one._y > two._y)
					return 1;
				else if (one._y < two._y)
					return -1;

				return 0;
			}
		});
		
		for (int i = 0; i < yPoints.length; i++)
		{
			Point here = yPoints[i];
			Point back = null;
			Point next = null;
			
			if (i > 0)
				back = yPoints[i - 1];
			
			if (i < yPoints.length - 1)
				next = yPoints[i + 1];
			
			if (back != null && Math.abs(here._y - back._y) < this._minDistance)
			{
				if (here.distanceFrom(back) < this._minDistance)
					clusters.add(here.cluster(back));
			}

			if (next != null && Math.abs(here._y - next._y) < this._minDistance)
			{
				if (here.distanceFrom(next) < this._minDistance)
					clusters.add(here.cluster(next));
			}
		}
		
		HashSet<Cluster> toReturn = new HashSet<Cluster>();
		
		for (Cluster c : clusters)
		{
			if (c.population() >= this._minPopulation)
				toReturn.add(c);
		}
		
		return toReturn;
	}
}
