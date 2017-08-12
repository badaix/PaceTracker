package de.badaix.pacetracker.util;

import android.text.Editable;
import android.text.Html.TagHandler;

import org.xml.sax.XMLReader;

public class ListTagHandler implements TagHandler {
    boolean first = true;

    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
        if (tag.equals("li")) {
            char lastChar = 0;
            if (output.length() > 0)
                lastChar = output.charAt(output.length() - 1);
            if (first) {
                if (lastChar == '\n')
                    output.append("\t•  ");
                else
                    output.append("\n\t•  ");
                first = false;
            } else {
                first = true;
            }
        }
    }
}
