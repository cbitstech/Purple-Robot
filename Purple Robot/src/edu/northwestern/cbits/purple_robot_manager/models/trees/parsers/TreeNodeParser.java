package edu.northwestern.cbits.purple_robot_manager.models.trees.parsers;

import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;

/**
 * Abstract superclass responsible for mapping parsers to input content.
 */

public abstract class TreeNodeParser
{
    /**
     * Exception class for capturing parser-related errors and exceptions.
     */

    public static class ParserNotFound extends Exception
    {
        public ParserNotFound(String message)
        {
            super(message);
        }

        private static final long serialVersionUID = 3671610661977748070L;
    }

    /**
     * Inspects model content and generates the TreeNode (and descendants)
     * corresponding to the model's content.
     * 
     * @param content
     *            String representation of the decision tree.
     * 
     * @return Root TreeNode of the generated decision tree.
     * 
     * @throws ParserNotFound
     *             Thrown on parser error.
     * 
     * @throws TreeNodeException
     *             Thrown on tree errors.
     */

    public static TreeNode parseString(String content) throws ParserNotFound, TreeNodeException
    {
        TreeNodeParser parser = null;

        if (content.startsWith("digraph J48Tree"))
            parser = new WekaJ48TreeParser();
        else if (content.trim().startsWith("1 "))
            parser = new MatLabBinaryTreeParser();

        if (parser == null)
            throw new TreeNodeParser.ParserNotFound("Unable to find parser for content.");

        return parser.parse(content);
    }

    /**
     * Abstract method implemented by specific parsers to generate a decision
     * tree.
     * 
     * @param content
     *            String representation of the decision tree.
     * @return Root TreeNode of the generated decision tree.
     * @throws TreeNodeException
     *             Thrown on tree errors.
     */

    public abstract TreeNode parse(String content) throws TreeNodeException;
}
