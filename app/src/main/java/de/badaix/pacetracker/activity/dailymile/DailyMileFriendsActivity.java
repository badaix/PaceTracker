package de.badaix.pacetracker.activity.dailymile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.activity.VectorFragmentPagerAdapter;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.ActionbarUrlImage;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.social.dailymile.User;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.views.dailymile.DailyMileFriend;
import de.badaix.pacetracker.views.dailymile.DailyMileItem;

public class DailyMileFriendsActivity extends AppCompatActivity implements OnStreamUpdateListener,
        OnItemTouchListener {
    VectorFragmentPagerAdapter mAdapter;
    ViewPager mPager;
    View touchView;
    private DailyMileFragmentFriends friendsFragment = null;
    // private ActionBar actionBar;
    private PagerTitleStrip pagerTitleStrip;
    private User user;
    private DailyMileFriend dailyMileFriend = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_dailymile);

        // actionBar = (ActionBar) findViewById(R.id.dailyMileActionbar);
        // // actionBar.setHomeLogo(sessionSummary.getDrawable());
        // actionBar.setDisplayHomeAsUpEnabled(false);

        pagerTitleStrip = (PagerTitleStrip) findViewById(R.id.dailyMileIndicator);
        pagerTitleStrip.setVisibility(View.GONE);

        user = getUser(getIntent());

        createFragment(user);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (user != null)
            new ActionbarUrlImage(this, user.getMiniPhotoUrl(), getResources().getDrawable(R.drawable.user_mini));
    }

    protected User getUser(Intent intent) {
        Bundle extras = getIntent().getExtras();
        if ((extras == null) || !extras.containsKey("json"))
            throw new IllegalArgumentException("no user specified");

        try {
            return new User(new JSONObject(extras.getString("json")));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    protected void createFragment(User user) {
        getSupportActionBar().setTitle(getResources().getString(R.string.friendsOf) + " " + user.getDisplayName());
        friendsFragment = new DailyMileFragmentFriends(user.getDisplayName(), user, this);
        Vector<Fragment> fragments = new Vector<Fragment>();
        friendsFragment.setOnItemTouchListener(this);
        fragments.add(friendsFragment);

        mAdapter = new VectorFragmentPagerAdapter(getSupportFragmentManager(), fragments);
        mPager = (ViewPager) findViewById(R.id.dailyMilePager);
        mPager.setAdapter(mAdapter);
    }

    @Override
    public void onNewIntent(Intent intent) {
        user = getUser(intent);
        createFragment(user);
    }

    @Override
    public void onUpdateStart(DailyMileFragment fragment) {
        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    public void onUpdateFinished(DailyMileFragment fragment) {
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onEntryClick(DailyMileItem item) {
        if (!(item instanceof DailyMileFriend))
            return;
        dailyMileFriend = (DailyMileFriend) item;
        Intent intent = new Intent(this, DailyMileUserActivity.class);
        try {
            intent.putExtra("json", dailyMileFriend.getUser().toJson().toString());
        } catch (JSONException e) {
            Hint.show(this, e);
            e.printStackTrace();
        }
        startActivity(intent);
    }

    @Override
    public void onEntryChanged(PersonEntry entry) {
    }

    @Override
    public boolean onEntryLongClick(DailyMileItem item) {
        return true;
    }

}
