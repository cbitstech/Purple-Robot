package edu.northwestern.cbits.purple_robot_manager.util.sexp;

public class Value extends SExpression
{
    private Object _value = null;

    public static Value fromDouble(double d)
    {
        Value v = new Value();
        v._value = d;

        return v;
    }

    public static Value fromString(String s)
    {
        Value v = new Value();
        v._value = s;

        return v;
    }

    public static Value fromInteger(int i)
    {
        Value v = new Value();
        v._value = i;

        return v;
    }

    public static Value fromBoolean(boolean b)
    {
        Value v = new Value();
        v._value = b;

        return v;
    }

    public String toString(boolean pretty)
    {
        if (this._value == null)
            return "()";
        else if (this._value instanceof String)
            return "\"" + this._value.toString() + "\"";

        return this._value.toString();
    }
}
