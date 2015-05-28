package edu.northwestern.cbits.purple_robot_manager.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import edu.northwestern.cbits.purple_robot_manager.R;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ScriptingEngineMethod
{
    String language();
    String assetPath() default "";
    int category() default R.string.docs_script_category_unknown;
    String[] arguments() default { "Unknown" };
}
