package de.badaix.pacetracker.session;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import de.badaix.pacetracker.R;

public enum Felt {
    GREAT("great", 0), GOOD("good", 1), ALRIGHT("alright", 2), BLAH("blah", 3), TIRED("tired", 4), INJURED("injured", 5), NONE(
            "", 6);

    private String asString = "";
    private int index = 0;

    Felt(String asString, int idx) {
        this.asString = asString;
        index = idx;
    }

    public static Felt fromString(String type) {
        if (type.equalsIgnoreCase(GREAT.toString()))
            return GREAT;
        else if (type.equalsIgnoreCase(GOOD.toString()))
            return GOOD;
        else if (type.equalsIgnoreCase(ALRIGHT.toString()))
            return ALRIGHT;
        else if (type.equalsIgnoreCase(BLAH.toString()))
            return BLAH;
        else if (type.equalsIgnoreCase(TIRED.toString()))
            return TIRED;
        else if (type.equalsIgnoreCase(INJURED.toString()))
            return INJURED;
        else if (type.equalsIgnoreCase(NONE.toString()))
            return NONE;
        throw new IllegalArgumentException(type);
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return asString;
    }

    public String toLocaleString(Context context) {
        switch (this) {
            case ALRIGHT:
                return context.getResources().getString(R.string.feltALRIGHT);
            case BLAH:
                return context.getResources().getString(R.string.feltBLAH);
            case GOOD:
                return context.getResources().getString(R.string.feltGOOD);
            case GREAT:
                return context.getResources().getString(R.string.feltGREAT);
            case INJURED:
                return context.getResources().getString(R.string.feltINJURED);
            case TIRED:
                return context.getResources().getString(R.string.feltTIRED);
            case NONE:
                return "";
            default:
                return toString();
        }
    }

    public Drawable getDrawable(Context context) {
        Resources resources = context.getResources();
        switch (this) {
            case ALRIGHT:
                return resources.getDrawable(R.drawable.smiley_alright);
            case BLAH:
                return resources.getDrawable(R.drawable.smiley_blah);
            case GOOD:
                return resources.getDrawable(R.drawable.smiley_good);
            case GREAT:
                return resources.getDrawable(R.drawable.smiley_great);
            case INJURED:
                return resources.getDrawable(R.drawable.smiley_injured);
            case TIRED:
                return resources.getDrawable(R.drawable.smiley_tired);
            case NONE:
                return resources.getDrawable(R.drawable.transparent);
            default:
                return resources.getDrawable(R.drawable.transparent);
        }
    }
}
