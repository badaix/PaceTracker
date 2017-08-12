package de.badaix.pacetracker.views.dailymile;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageView;

import de.badaix.pacetracker.R;

public class DailyMileNote extends DailyMileItem {
    private EditText editNote;
    private ImageView imageView;
    private boolean hasImage = false;

    public DailyMileNote(Context context) {
        super(context);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.view_dailymile_note, this);

        editNote = (EditText) findViewById(R.id.editNote);
        editNote.clearFocus();

        imageView = (ImageView) findViewById(R.id.imageView);
        hasImage = false;
    }

    public String getNote() {
        return editNote.getText().toString();
    }

    public ImageView getImageView() {
        return imageView;
    }

    public boolean hasImage() {
        return hasImage;
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.content_new_picture));
            hasImage = false;
        } else {
            imageView.setImageBitmap(bitmap);
            hasImage = true;
        }
    }

}
