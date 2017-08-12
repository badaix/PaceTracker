package de.badaix.pacetracker.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.URL;
import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.maps.BaseTileSource;
import de.badaix.pacetracker.maps.TilePos;
import de.badaix.pacetracker.maps.TileSourceFactory;
import de.badaix.pacetracker.maps.TileSourceFactory.TileSource;
import de.badaix.pacetracker.maps.TileUtils;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.social.dailymile.DailyMileHelper;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Hint;

public class HistoryItem extends LinearLayout {
    Vector<Pair<String, String>> vItems;
    int iFirst = 0;
    private TextView tvName;
    private TextView tvDate;
    private TextView tvDuration;
    private TextView tvDistance;
    private ImageView imageViewMap;
    private ImageView imageViewType;
    private ImageView imageViewDailyMile;
    private ImageView imageViewFacebook;
    private ImageView imageViewGPlus;
    private int id;
    private String filename;
    private SessionSummary sessionSummary;
    private boolean dailyMileCompatible;
    private Context context;

    public HistoryItem(Context context) {
        this(context, null);
    }

    public HistoryItem(Context context, SessionSummary summary)
    // int drawable, String name, String type, Date date, double distance, long
    // duration)
    {
        super(context);
        this.context = context;
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.history_item, this);
        dailyMileCompatible = false;
        tvName = (TextView) findViewById(R.id.tvHistType);
        tvDate = (TextView) findViewById(R.id.tvHistDate);
        tvDuration = (TextView) findViewById(R.id.tvHistDuration);
        tvDistance = (TextView) findViewById(R.id.tvHistDistance);
        imageViewType = (ImageView) findViewById(R.id.imageViewType);
        imageViewMap = (ImageView) findViewById(R.id.imageViewMap);
        imageViewDailyMile = (ImageView) findViewById(R.id.imageViewDailyMile);
        imageViewFacebook = (ImageView) findViewById(R.id.imageViewFacebook);
        imageViewGPlus = (ImageView) findViewById(R.id.imageViewGPlus);

        if (!GlobalSettings.getInstance(context).isFacebookEnabled())
            imageViewFacebook.setVisibility(View.GONE);
        if (!GlobalSettings.getInstance(context).isGplusEnabled())
            imageViewGPlus.setVisibility(View.INVISIBLE);

        this.sessionSummary = summary;
        update();
    }

    public void update() {
        try {
            imageViewType.setImageResource(sessionSummary.getDrawable());
            if (sessionSummary.getStartPos() != null) {
                BaseTileSource tileSource = TileSourceFactory.getTileSource(TileSource.GOOGLE_BITMAP);
                TilePos tilePos = TileUtils.getTileNumber(sessionSummary.getStartPos(), 18);
                URL tileUrl = tileSource.getTileURL(0, (int) tilePos.x, (int) tilePos.y, tilePos.z);
                UrlImageViewHelper.getInstance().setUrlDrawable(imageViewMap, tileUrl.toExternalForm(),
                        R.drawable.transparent, null);
            } else {
                imageViewMap.setImageResource(R.drawable.transparent);
            }

            tvName.setText(sessionSummary.getName(context));
            try {
                tvDate.setText(android.text.format.DateUtils.getRelativeTimeSpanString(sessionSummary.getSessionStart()
                        .getTime(), new Date().getTime(), android.text.format.DateUtils.SECOND_IN_MILLIS));
            } catch (Exception e) {
                tvDate.setText("");
            }
            tvDuration.setText(DateUtils.secondsToHHMMSSString(sessionSummary.getDuration() / 1000));
            this.id = sessionSummary.getId();
            filename = sessionSummary.getFilename();
            tvDistance.setText(Distance.distanceToString(sessionSummary.getDistance(), 2) + " "
                    + GlobalSettings.getInstance(context).getDistUnit().toShortString());

            dailyMileCompatible = (DailyMileHelper.getActivity(sessionSummary) != null);

            if (!dailyMileCompatible)
                imageViewDailyMile.setVisibility(GONE);
            else {
                imageViewDailyMile.setVisibility(VISIBLE);
                if (sessionSummary.getSettings().getDailyMileId() != -1)
                    imageViewDailyMile.setImageDrawable(getResources().getDrawable(R.drawable.dailymile_online));
                else
                    imageViewDailyMile.setImageDrawable(getResources().getDrawable(R.drawable.dailymile_offline));
            }

            if (sessionSummary.getSettings().getGPlusId() != -1)
                imageViewGPlus.setImageDrawable(getResources().getDrawable(R.drawable.gplus_online));
            else
                imageViewGPlus.setImageDrawable(getResources().getDrawable(R.drawable.gplus_offline));

            if (!TextUtils.isEmpty(sessionSummary.getSettings().getFbId()))
                imageViewFacebook.setImageDrawable(getResources().getDrawable(R.drawable.facebook_online));
            else
                imageViewFacebook.setImageDrawable(getResources().getDrawable(R.drawable.facebook_offline));

        } catch (Exception e) {
            Hint.log(this, e);
        }
    }

    public boolean isDailyMileCompatible() {
        return dailyMileCompatible;
    }

    public SessionSummary getSessionSummary() {
        return sessionSummary;
    }

    public void setSessionSummary(SessionSummary sessionSummary) {
        this.sessionSummary = sessionSummary;
        update();
    }

    public int getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getSessionType() {
        return sessionSummary.getType();
    }

}
