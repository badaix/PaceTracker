package de.badaix.pacetracker.social;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import de.badaix.pacetracker.util.Helper;

public class ActionbarUrlImage implements UrlImageViewCallback {
    private AppCompatActivity activity;
    private ImageView imageView;
    private int size;

    public ActionbarUrlImage(AppCompatActivity activity, String url, Drawable defaultDrawable) {
        this.activity = activity;
        imageView = new ImageView(activity);
        size = Helper.dipToPix(imageView.getContext(), 48);
        UrlImageViewHelper.getInstance().setUrlDrawable(imageView, url, defaultDrawable, this);
    }

    @Override
    public void onLoaded(ImageView imageView, String url, boolean loadedFromCache) {

        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();

        float scale = (float) size
                / (float) Math.max(bitmapDrawable.getBitmap().getWidth(), bitmapDrawable.getBitmap().getHeight());

        Bitmap bitmap = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), (int) (bitmapDrawable.getBitmap()
                .getWidth() * scale), (int) (bitmapDrawable.getBitmap().getHeight() * scale), true);

//        activity.getSupportActionBar().setLogo(new BitmapDrawable(bitmap));
//        activity.getSupportActionBar().setDisplayUseLogoEnabled(true);
//        activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

}
