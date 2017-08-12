package de.badaix.pacetracker.views.dailymile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.UrlImageViewCallback;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.social.dailymile.Media;

public class DailyMileMedia extends DailyMileItem implements OnClickListener, UrlImageViewCallback {
    private Media media;
    private ImageView ivDmMedia;
    private ProgressBar pbDmLoading;

    // private LinearLayout rlDmMedia;

    public DailyMileMedia(Context context) {
        this(context, null);
    }

    public DailyMileMedia(Context context, Media media) {
        super(context);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.view_dailymile_media, this);
        ivDmMedia = (ImageView) findViewById(R.id.ivDmMedia);
        pbDmLoading = (ProgressBar) findViewById(R.id.pbDmLoading);
        // rlDmMedia = (LinearLayout) findViewById(R.id.rlDmMedia);

        this.media = media;
        update();
    }

    public void update() {
        if (media == null)
            return;

        pbDmLoading.setVisibility(VISIBLE);
        ivDmMedia.setVisibility(GONE);
        String imageUrl = "";

        if ((media.getContent() != null) && (media.getContent().getType() == Media.Type.IMAGE)
                && (media.getContent().getUrl() != null))
            imageUrl = media.getContent().getUrl();
        else if ((media.getPreview() != null) && (media.getPreview().getUrl() != null))
            imageUrl = media.getPreview().getUrl();
        else if ((media.getContent() != null) && (media.getContent().getUrl() != null))
            imageUrl = media.getContent().getUrl();
        else {
            pbDmLoading.setVisibility(GONE);
            ivDmMedia.setVisibility(GONE);
            return;
        }

        if ((media.getContent().getType() == Media.Type.VIDEO) && (media.getContent().getUrl() != null))
            ivDmMedia.setOnClickListener(this);

        UrlImageViewHelper.getInstance().setUrlDrawable(ivDmMedia, imageUrl, null, this);
    }

    public Media getMedia() {
        return media;
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(media.getContent().getUrl()));
        getContext().startActivity(intent);
    }

    @Override
    public void onLoaded(ImageView imageView, String url, boolean loadedFromCache) {
        // TODO Auto-generated method stub
        pbDmLoading.setVisibility(GONE);
        ivDmMedia.setVisibility(VISIBLE);
    }
}
