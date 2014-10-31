package edu.northwestern.cbits.purple_robot_manager.util.sexp;

public class Atom
{
    private String _name = null;

    public Atom(String name)
    {
        this._name = name;
    }

    public String toString(boolean pretty)
    {
        return this._name;
    }
}
