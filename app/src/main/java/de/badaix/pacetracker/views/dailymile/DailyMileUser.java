package de.badaix.pacetracker.views.dailymile;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.social.dailymile.User;

public class DailyMileUser extends DailyMileItem implements OnClickListener {
    private TextView tvUser;
    private TextView tvGoal;
    private TextView tvLocation;
    private TextView tvFriends;
    private ImageView ivDmUser;
    private User user;

    public DailyMileUser(Context context) {
        this(context, null);
    }

    public DailyMileUser(Context context, User user) {
        super(context);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.view_dailymile_user, this);
        tvUser = (TextView) findViewById(R.id.tvDmUser);
        tvGoal = (TextView) findViewById(R.id.tvDmGoal);
        tvFriends = (TextView) findViewById(R.id.tvDmFriends);
        tvLocation = (TextView) findViewById(R.id.tvDmLocation);
        ivDmUser = (ImageView) findViewById(R.id.ivDmUser);
        setUser(user);
    }

    public void setUser(User user) {
        this.user = user;
        update();
    }

    @Override
    public void update() {
        ivDmUser.setImageResource(R.drawable.user_profile);
        UrlImageViewHelper.getInstance().setUrlDrawable(ivDmUser, user.getProfilePhotoUrl(), R.drawable.user_profile,
                null);
        tvUser.setText(user.getDisplayName());

        if ((user != null) && !TextUtils.isEmpty(user.getGoal())) {
            tvGoal.setText(getResources().getString(R.string.goal) + ": " + user.getGoal());
            tvGoal.setVisibility(View.VISIBLE);
        } else {
            tvGoal.setVisibility(View.GONE);
        }

        if (user.getLocation() != null) {
            tvLocation.setText(getResources().getString(R.string.location) + ": " + user.getLocation());
            tvLocation.setVisibility(View.VISIBLE);
        } else {
            tvLocation.setVisibility(View.GONE);
        }

        if (user.getFriends() != null) {
            if (user.getFriends().size() == 1)
                tvFriends.setText(user.getFriends().size() + " " + getResources().getString(R.string.friend));
            else
                tvFriends.setText(user.getFriends().size() + " " + getResources().getString(R.string.friends));
            tvFriends.setVisibility(View.VISIBLE);
        } else {
            tvFriends.setVisibility(View.GONE);
        }

    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }
}
