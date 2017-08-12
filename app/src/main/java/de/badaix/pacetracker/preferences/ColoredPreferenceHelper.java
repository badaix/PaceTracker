package de.badaix.pacetracker.preferences;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ColoredPreferenceHelper {
    static View findSubViewById(View view, int id) {
        if (view.getId() == id)
            return view;
        else if (view instanceof ViewGroup) {
            ViewGroup grp = (ViewGroup) view;
            for (int index = 0; index < grp.getChildCount(); index++) {
                View subView = grp.getChildAt(index);
                if ((subView = findSubViewById(subView, id)) != null)
                    return subView;
            }
        }

        return null;
    }

    static View findParentView(View view, int childId) {
        if (view instanceof ViewGroup) {
            ViewGroup grp = (ViewGroup) view;
            for (int index = 0; index < grp.getChildCount(); index++) {
                View subView = grp.getChildAt(index);
                if (subView.getId() == childId)
                    return view;
                else if (findParentView(subView, childId) != null)
                    return subView;
            }
        }

        return null;
    }

    static void setTextColor(View view, int color, int secondaryColor) {
        if (view == null)
            return;

        TextView tv;
        if ((tv = (TextView) findSubViewById(view, android.R.id.title)) != null) {
            tv.setTextColor(color);
            if ((tv = (TextView) findSubViewById(view, android.R.id.summary)) != null)
                tv.setTextColor(secondaryColor);
        }
    }

    static void setTextColor(View view, int color) {
        setTextColor(view, color, color);
    }
}
