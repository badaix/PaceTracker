package de.badaix.pacetracker.preferences;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.util.Helper;

/**
 * The ImageArrayAdapter is the array adapter used for displaying an additional
 * image to a list preference item.
 *
 * @author Casper Wakkers
 */
public class ImageArrayAdapter extends ArrayAdapter<CharSequence> {
    private int index = 0;
    private int[] resourceIds = null;

    /**
     * ImageArrayAdapter constructor.
     *
     * @param context            the context.
     * @param textViewResourceId resource id of the text view.
     * @param objects            to be displayed.
     * @param ids                resource id of the images to be displayed.
     * @param i                  index of the previous selected item.
     */
    public ImageArrayAdapter(Context context, int textViewResourceId, CharSequence[] objects, int[] ids, int i) {
        super(context, textViewResourceId, objects);

        index = i;
        resourceIds = ids;
    }

    public ImageArrayAdapter(Context context, int textViewResourceId, CharSequence[] objects, CharSequence[] ids, int i) {
        super(context, textViewResourceId, objects);

        resourceIds = new int[ids.length];

        for (int id = 0; id < ids.length; id++) {
            String imageName = (String) ids[id];
            imageName = imageName.substring(imageName.lastIndexOf('/') + 1, imageName.lastIndexOf('.'));

            resourceIds[id] = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
        }

        index = i;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        return getView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
        View row = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);

        // ImageView imageView = (ImageView) row.findViewById(R.id.image);
        // imageView.setImageResource(resourceIds[position]);

        CheckedTextView checkedTextView = (CheckedTextView) row.findViewById(android.R.id.text1);
        checkedTextView.setTextAppearance(getContext(), R.style.MediumFont);
        checkedTextView.setText(getItem(position));
        Bitmap bm = BitmapFactory.decodeResource(getContext().getResources(), resourceIds[position]);
        bm = Bitmap.createScaledBitmap(bm, Helper.dipToPix(getContext(), 32), Helper.dipToPix(getContext(), 32), true);
        checkedTextView.setCheckMarkDrawable(new BitmapDrawable(getContext().getResources(), bm));

        if (position == index)
            checkedTextView.setChecked(true);

        return row;
    }
}
