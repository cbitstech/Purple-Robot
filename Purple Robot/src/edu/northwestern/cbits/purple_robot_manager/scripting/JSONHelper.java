package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.util.Iterator;

import jsint.Pair;
import jsint.Symbol;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class JSONHelper
{
    public boolean isJson(Object o)
    {
        return (o instanceof JSONObject);
    }

    public boolean isJsonArray(Object o)
    {
        return (o instanceof JSONArray);
    }

    public int jsonArrayLength(JSONArray array)
    {
        return array.length();
    }

    public Object parse(String jsonString) throws JSONException
    {
        if (jsonString.trim().startsWith("{"))
            return new JSONObject(jsonString);
        else if (jsonString.trim().startsWith("["))
            return new JSONArray(jsonString);

        try
        {
            return Integer.parseInt(jsonString);
        }
        catch (NumberFormatException e)
        {

        }

        try
        {
            return Double.parseDouble(jsonString);
        }
        catch (NumberFormatException e)
        {

        }

        return jsonString;
    }

    public Object jsonArrayGet(JSONArray array, int index) throws JSONException
    {
        Object value = array.get(index);

        if (value instanceof JSONArray)
            value = JSONHelper.toList((JSONArray) value);
        else if (value instanceof JSONObject)
            value = JSONHelper.toPairs((JSONObject) value);

        return value;
    }

    private static JSONObject fromPairs(Pair pair) throws JSONException
    {
        JSONObject json = new JSONObject();

        if (pair.isEmpty())
            return json;

        if (pair.isEmpty() == false)
        {
            Object first = pair.getFirst();

            if (first instanceof Pair)
            {
                Pair firstPair = (Pair) first;

                String key = firstPair.first.toString();

                Object value = firstPair.rest();

                if (value instanceof Pair)
                {
                    Pair valuePair = (Pair) value;

                    if (valuePair.first instanceof Pair)
                        value = JSONHelper.fromPairs(valuePair);
                    else
                        value = JSONHelper.listFromPairs(valuePair);

                    value = valuePair.toString();
                }

                json.put(key, value);
            }

            Object rest = pair.getRest();

            if (rest instanceof Pair)
            {
                Pair restPair = (Pair) rest;

                JSONObject restJson = JSONHelper.fromPairs(restPair);

                Iterator<String> keys = restJson.keys();

                while (keys.hasNext())
                {
                    String key = keys.next();

                    json.put(key, restJson.get(key));
                }
            }
        }

        return json;
    }

    private static JSONArray listFromPairs(Pair pairs) throws JSONException
    {
        if (pairs.isEmpty())
            return new JSONArray();

        Object value = pairs.first;

        if (value instanceof Pair)
        {
            Pair valuePair = (Pair) value;

            if (valuePair.first instanceof Pair)
                value = JSONHelper.fromPairs(valuePair);
            else
                value = JSONHelper.listFromPairs(valuePair);
        }

        JSONArray array = new JSONArray();
        array.put(value);

        Object rest = pairs.rest();

        if (rest instanceof Pair)
        {
            JSONArray restArray = JSONHelper.listFromPairs((Pair) rest);

            for (int i = 0; i < restArray.length(); i++)
                array.put(restArray.get(i));
        }

        return array;
    }

    public boolean jsonArrayInsert(JSONArray array, Object value, int index) throws JSONException
    {
        if (value instanceof Pair)
        {
            Pair valuePair = (Pair) value;

            if (valuePair.first instanceof Pair)
                value = JSONHelper.fromPairs(valuePair);
            else
                value = JSONHelper.listFromPairs(valuePair);

            array.put(index, value);

            return true;
        }

        return false;
    }

    public boolean jsonArrayAppend(JSONArray array, Object value) throws JSONException
    {
        if (value instanceof Pair)
        {
            Pair valuePair = (Pair) value;

            if (valuePair.first instanceof Pair)
                value = JSONHelper.fromPairs(valuePair);
            else
                value = JSONHelper.listFromPairs(valuePair);

            array.put(value);

            return true;
        }

        return false;
    }

    public JSONArray jsonArrayRemove(JSONArray array, int index) throws JSONException
    {
        JSONArray newArray = new JSONArray();

        for (int i = 0; i < array.length(); i++)
        {
            if (i != index)
                newArray.put(array.get(i));
        }

        return newArray;
    }

    public JSONArray jsonArrayReplace(JSONArray array, Object value, int index) throws JSONException
    {
        if (value instanceof Pair)
        {
            Pair valuePair = (Pair) value;

            if (valuePair.first instanceof Pair)
                value = JSONHelper.fromPairs(valuePair);
            else
                value = JSONHelper.listFromPairs(valuePair);
        }

        JSONArray newArray = new JSONArray();

        for (int i = 0; i < array.length(); i++)
        {
            if (i != index)
                newArray.put(array.get(i));
            else
                newArray.put(value);
        }

        return newArray;
    }

    public String toString(Object o, int indent) throws JSONException
    {
        if (o instanceof JSONObject && indent > 0)
            return ((JSONObject) o).toString(indent);
        else if (o instanceof JSONArray && indent > 0)
            return ((JSONArray) o).toString(indent);

        return o.toString();
    }

    public Object get(Context context, JSONObject obj, String key)
    {
        try
        {
            Object value = obj.get(key);

            if (value instanceof JSONArray)
                value = JSONHelper.toList((JSONArray) value);
            else if (value instanceof JSONObject)
                value = JSONHelper.toPairs((JSONObject) value);

            return value;
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return null;
    }

    private static Pair toList(JSONArray array) throws JSONException
    {
        Pair pair = Pair.EMPTY;

        for (int i = array.length() - 1; i >= 0; i--)
        {
            Object item = array.get(i);

            if (item instanceof JSONArray)
                item = JSONHelper.toList((JSONArray) item);
            else if (item instanceof JSONObject)
                item = JSONHelper.toPairs((JSONObject) item);

            pair = new Pair(item, pair);
        }

        return pair;
    }

    public Pair keys(JSONObject json) throws JSONException
    {
        return JSONHelper.toList(json.names());
    }

    private static Pair toPairs(JSONObject json) throws JSONException
    {
        Pair list = Pair.EMPTY;

        JSONArray names = json.names();

        for (int i = 0; i < names.length(); i++)
        {
            String name = names.getString(i);
            Object item = json.get(name);

            if (item instanceof JSONArray)
                item = JSONHelper.toList((JSONArray) item);
            else if (item instanceof JSONObject)
                item = JSONHelper.toPairs((JSONObject) item);

            list = new Pair(new Pair(name, item), list);
        }

        return new Pair(new Pair(Symbol.QUOTE, new Pair(list, Pair.EMPTY)), Pair.EMPTY);
    }

    public boolean put(Context context, JSONObject obj, String key, Object value)
    {
        try
        {
            obj.put(key, value);

            return true;
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return false;
    }
}
