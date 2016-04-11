package edu.northwestern.cbits.purple_robot_manager.util;

import android.annotation.SuppressLint;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

public class Slugify
{
    public static String slugify(String input)
    {
        String ret = StringUtils.trim(input);
        if (StringUtils.isBlank(input))
        {
            return "";
        }

        ret = normalize(ret);
        ret = removeDuplicateWhiteSpaces(ret);
        return ret.replace(" ", "-").toLowerCase(Locale.getDefault());
    }

    @SuppressLint("InlinedApi")
    private static String normalize(String input)
    {
        String ret = StringUtils.trim(input);

        if (StringUtils.isBlank(ret))
        {
            return "";
        }

        // ret = ret.replace( "ß", "ss" );
        return Normalizer.normalize(ret, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9 ]", "");
    }

    private static String removeDuplicateWhiteSpaces(String input)
    {
        String ret = StringUtils.trim(input);
        if (StringUtils.isBlank(ret))
        {
            return "";
        }

        return ret.replaceAll("\\s+", " ");
    }
}
