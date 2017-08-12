package de.badaix.pacetracker.activity.dailymile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
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
import de.badaix.pacetracker.views.dailymile.DailyMileComment;
import de.badaix.pacetracker.views.dailymile.DailyMileEntry;
import de.badaix.pacetracker.views.dailymile.DailyMileItem;

public class DailyMileUserActivity extends AppCompatActivity implements OnStreamUpdateListener,
        OnItemTouchListener, MenuItem.OnMenuItemClickListener {
    VectorFragmentPagerAdapter mAdapter;
    ViewPager mPager;
    View touchView;
    private DailyMileFragmentUser userFragment = null;
    // private ActionBar actionBar;
    private PagerTitleStrip pagerTitleStrip;
    private User user;
    // private ClickAction friendsAction;
    private MenuItem friendsItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_dailymile);

        pagerTitleStrip = (PagerTitleStrip) findViewById(R.id.dailyMileIndicator);
        pagerTitleStrip.setVisibility(View.GONE);

        Bundle extras = getIntent().getExtras();
        if ((extras == null) || !extras.containsKey("json"))
            throw new IllegalArgumentException("no user specified");

        try {
            user = new User(new JSONObject(extras.getString("json")));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e.getMessage());
        }

        // actionBar = (ActionBar) findViewById(R.id.dailyMileActionbar);
        // // actionBar.setHomeLogo(sessionSummary.getDrawable());
        // actionBar.setDisplayHomeAsUpEnabled(false);
        // friendsAction = new ClickAction(this,
        // R.drawable.ic_action_social_group);
        // actionBar.addAction(friendsAction);
        //
        getSupportActionBar().setTitle(user.getDisplayName());
        userFragment = new DailyMileFragmentUser(user.getDisplayName(), user, this);
        Vector<Fragment> fragments = new Vector<Fragment>();
        userFragment.setOnItemTouchListener(this);
        fragments.add(userFragment);

        mAdapter = new VectorFragmentPagerAdapter(getSupportFragmentManager(), fragments);
        mPager = (ViewPager) findViewById(R.id.dailyMilePager);
        mPager.setAdapter(mAdapter);
        ItemChangedBroadcast.getInstance().addListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        friendsItem = menu.add(getString(R.string.friends));
        friendsItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_social_group)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (user != null)
            new ActionbarUrlImage(this, user.getMiniPhotoUrl(), getResources().getDrawable(R.drawable.user_mini));
    }

    @Override
    public void onDestroy() {
        ItemChangedBroadcast.getInstance().removeListener(this);
        super.onDestroy();
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
        if (item instanceof DailyMileEntry) {
            try {
                DailyMileEntry dailyMileEntry = (DailyMileEntry) item;
                Intent intent = new Intent(this, DailyMileEntryActivity.class);
                intent.putExtra("json", dailyMileEntry.getEntry().toJson().toString());
                startActivity(intent);
            } catch (JSONException e) {
                Hint.show(this, e);
                e.printStackTrace();
            }
        } else if (item instanceof DailyMileComment) {
            try {
                DailyMileComment dailyMileComment = (DailyMileComment) item;
                Intent intent = new Intent(this, DailyMileUserActivity.class);
                intent.putExtra("json", dailyMileComment.getComment().getUser().toJson().toString());
                startActivity(intent);
            } catch (JSONException e) {
                Hint.show(this, e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEntryChanged(PersonEntry entry) {
        userFragment.updateEntry(entry);
    }

    @Override
    public boolean onEntryLongClick(DailyMileItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        try {
            Intent intent = new Intent(this, DailyMileFriendsActivity.class);
            intent.putExtra("json", user.toJson().toString());
            this.startActivity(intent);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

}
