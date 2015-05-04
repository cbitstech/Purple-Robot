package edu.northwestern.cbits.purple_robot_manager.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import edu.northwestern.cbits.purple_robot_manager.R;

/**
 * Created by Chris Karr on 5/3/2015.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ScriptingEngineMethod
{
    public String language();
    public String assetPath() default "";
    public int category() default R.string.docs_script_category_unknown;
    public String[] arguments() default { "Unknown" };
}
