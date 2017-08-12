package de.badaix.pacetracker.views.dailymile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.social.dailymile.User;

public class DailyMileFriend extends DailyMileItem {
    private TextView tvUser;
    private TextView tvLocation;
    private ImageView ivDmUser;
    private User user;

    public DailyMileFriend(Context context) {
        this(context, null);
    }

    public DailyMileFriend(Context context, User user) {
        super(context);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.view_dailymile_friend, this);
        tvUser = (TextView) findViewById(R.id.tvDmUser);
        tvLocation = (TextView) findViewById(R.id.tvDmLocation);
        ivDmUser = (ImageView) findViewById(R.id.ivDmUser);
        setUser(user);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        update();
    }

    @Override
    public void update() {
        if (user == null)
            return;

        // UrlImageViewHelper.discardImageView(ivDmUser, null);
        ivDmUser.setImageDrawable(getResources().getDrawable(R.drawable.user_mini));
        UrlImageViewHelper.getInstance().setUrlDrawable(ivDmUser, user.getAvatarPhotoUrl(), R.drawable.user_mini, null);
        tvUser.setText(user.getDisplayName());

        if (user.getLocation() != null) {
            tvLocation.setText(getResources().getString(R.string.location) + ": " + user.getLocation());
            tvLocation.setVisibility(View.VISIBLE);
        } else {
            tvLocation.setVisibility(View.GONE);
        }
    }

}
