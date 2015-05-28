package edu.northwestern.cbits.purple_robot_manager.util.sexp;

import java.util.ArrayList;

public class List extends SExpression
{
    private ArrayList<SExpression> _elements = new ArrayList<>();

    public List()
    {

    }

    public List(java.util.List<SExpression> items)
    {
        this._elements.addAll(items);
    }

    public String toString(boolean pretty)
    {
        // TODO: Obey pretty...

        StringBuilder sb = new StringBuilder();

        sb.append("(");

        for (SExpression sexp : this._elements)
        {
            if (sb.length() > 1)
                sb.append(" ");

            sb.append(sexp.toString(pretty));
        }

        sb.append(")");

        return sb.toString();
    }
}
