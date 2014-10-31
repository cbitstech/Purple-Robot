package edu.northwestern.cbits.purple_robot_manager.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class DBSCAN
{
    public static final int POPULATION = 10;
    public static final double DISTANCE = 0.001;
    private double _minDistance = 0;
    private int _minPopulation = 0;

    private HashSet<Point> _points = new HashSet<Point>();

    public static class Point
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

    public static class Cluster
    {
        private HashSet<Point> _points = new HashSet<Point>();
        private String _name = null;

        public void addPoint(Point p)
        {
            if (p.getCluster() != null)
                p.getCluster().removePoint(p);

            this._points.add(p);

            p.setCluster(this);
        }

        private void removePoint(Point p)
        {
            this._points.remove(p);
        }

        public int population()
        {
            return this._points.size();
        }

        public void assimilate(Cluster c)
        {
            ArrayList<Point> points = new ArrayList<Point>();
            points.addAll(c.getPoints());

            for (Point p : points)
            {
                this.addPoint(p);
            }
        }

        public Collection<Point> getPoints()
        {
            return this._points;
        }

        public String getName()
        {
            return this._name;
        }

        public void setName(String name)
        {
            this._name = name;
        }

        public JSONObject getJSON() throws JSONException
        {
            if (this._name != null)
            {
                JSONObject json = new JSONObject();

                json.put("name", this._name);

                JSONArray points = new JSONArray();

                for (Point point : this._points)
                {
                    JSONArray pointArray = new JSONArray();

                    pointArray.put(point._x);
                    pointArray.put(point._y);

                    points.put(pointArray);
                }

                json.put("points", points);

                return json;
            }

            return null;
        }

        public Cluster()
        {
            super();
        }

        public Cluster(JSONObject json) throws JSONException
        {
            super();

            if (json.has("name"))
                this._name = json.getString("name");

            JSONArray points = json.getJSONArray("points");

            for (int i = 0; i < points.length(); i++)
            {
                JSONArray point = points.getJSONArray(i);

                Point p = new Point(point.getDouble(0), point.getDouble(1));

                this.addPoint(p);
            }
        }

        public Collection<Point> getPoints(int count)
        {
            if (this._points.size() <= count)
                return this._points;

            ArrayList<Point> shuffled = new ArrayList<Point>();
            shuffled.addAll(this._points);

            Collections.shuffle(shuffled, new SecureRandom());

            return shuffled.subList(0, count);
        }
    }

    public DBSCAN(Context context, double distance, int population)
    {
        this._minDistance = distance;
        this._minPopulation = population;

        Collection<Cluster> clusters = DBSCAN.fetchClusters(context);

        for (Cluster c : clusters)
        {
            for (Point p : c.getPoints())
                this._points.add(p);
        }
    }

    public void addPoint(Point p)
    {
        this._points.add(p);
    }

    public Collection<Cluster> calculate(Context context)
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

    private static File getClusterFile(Context context)
    {
        File dataDir = context.getFilesDir();
        File clusterDir = new File(dataDir, "Cluster Data");

        if (clusterDir.exists() == false)
            clusterDir.mkdirs();

        File clusterFile = new File(clusterDir, "cluster.json");

        return clusterFile;

    }

    public static void persistClusters(Context context, ArrayList<Cluster> clusters, double distance, double population)
    {
        File clusterFile = DBSCAN.getClusterFile(context);

        try
        {
            JSONObject toWrite = new JSONObject();
            toWrite.put("distance", distance);
            toWrite.put("population", population);

            JSONArray clusterList = new JSONArray();

            for (Cluster cluster : clusters)
            {
                JSONObject clusterJSON = cluster.getJSON();

                if (clusterJSON != null)
                    clusterList.put(cluster.getJSON());
            }

            toWrite.put("clusters", clusterList);

            FileOutputStream fout = new FileOutputStream(clusterFile);

            fout.write(toWrite.toString().getBytes(Charset.defaultCharset().name()));

            fout.flush();
            fout.close();
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }
        catch (FileNotFoundException e)
        {
            LogManager.getInstance(context).logException(e);
        }
        catch (IOException e)
        {
            LogManager.getInstance(context).logException(e);
        }
    }

    public static String inCluster(Context context, double latitude, double longitude)
    {
        Point point = new Point(latitude, longitude);

        Collection<Cluster> clusters = DBSCAN.fetchClusters(context);

        for (Cluster cluster : clusters)
        {
            if (cluster.getName() != null)
            {
                for (Point p : cluster.getPoints())
                {
                    if (point.distanceFrom(p) <= DBSCAN.DISTANCE)
                        return cluster.getName();
                }
            }
        }

        return null;
    }

    private static Collection<Cluster> fetchClusters(Context context)
    {
        HashSet<Cluster> clusters = new HashSet<Cluster>();

        File clusterFile = DBSCAN.getClusterFile(context);

        if (clusterFile.exists())
        {
            try
            {
                FileInputStream fin = new FileInputStream(clusterFile);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int read = 0;

                while ((read = fin.read(buffer, 0, buffer.length)) != -1)
                {
                    baos.write(buffer, 0, read);
                }

                fin.close();

                String fileString = new String(baos.toByteArray(), Charset.defaultCharset().name());

                JSONObject clusterJson = new JSONObject(fileString);

                JSONArray clusterList = clusterJson.getJSONArray("clusters");

                for (int i = 0; i < clusterList.length(); i++)
                {
                    clusters.add(new Cluster(clusterList.getJSONObject(i)));
                }
            }
            catch (FileNotFoundException e)
            {
                LogManager.getInstance(context).logException(e);
            }
            catch (IOException e)
            {
                LogManager.getInstance(context).logException(e);
            }
            catch (JSONException e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }

        return clusters;
    }
}
