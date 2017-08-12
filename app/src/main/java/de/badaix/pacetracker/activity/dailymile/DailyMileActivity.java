package de.badaix.pacetracker.activity.dailymile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;

import org.json.JSONException;

import java.util.HashSet;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.activity.VectorFragmentPagerAdapter;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.dailymile.Comment;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;
import de.badaix.pacetracker.views.dailymile.DailyMileComment;
import de.badaix.pacetracker.views.dailymile.DailyMileEntry;
import de.badaix.pacetracker.views.dailymile.DailyMileItem;

interface OnStreamUpdateListener {
    public void onUpdateStart(DailyMileFragment fragment);

    public void onUpdateFinished(DailyMileFragment fragment);
}

interface OnPostListener {
    public void onPostDone();

    public void onPostFailed(Exception e);
}

class CommentTask extends AsyncTask<String, Void, Comment> {
    private ProgressDialog dialog = null;
    private DailyMileEntry dailyMileItem;
    private Exception e = null;
    private Comment result;
    private Context context;
    private OnPostListener listener = null;

    public CommentTask(Context context, DailyMileEntry dailyMileItem, ProgressDialog dialog, OnPostListener listener) {
        this.dialog = dialog;
        this.dailyMileItem = dailyMileItem;
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected Comment doInBackground(String... item) {
        String comment = item[0];
        DailyMile dailyMile = new DailyMile(context);
        try {
//TODO!!!
			result = dailyMile.comment(dailyMileItem.getEntry().getId(), comment);
        } catch (Exception e) {
            this.e = e;
        }

        return result;
    }

    @Override
    protected void onPostExecute(Comment comment) {
        if (dialog != null)
            dialog.dismiss();
        if (e != null) {
            Hint.show(context, e);
            if (listener != null)
                listener.onPostFailed(e);
        } else {
            if (dailyMileItem != null) {
                dailyMileItem.getEntry().getComments().add(comment);
                dailyMileItem.update();
                ItemChangedBroadcast.getInstance().notifyChanged(dailyMileItem.getEntry());
            }
            if (listener != null) {
                listener.onPostDone();
            }
        }
    }
}

public class DailyMileActivity extends AppCompatActivity implements OnStreamUpdateListener, OnItemTouchListener,
        OnClickListener, MenuItem.OnMenuItemClickListener {
    VectorFragmentPagerAdapter mAdapter;
    ViewPager mPager;
    private DailyMileFragmentMe meFragment = null;
    private DailyMileFragmentNearby nearbyFragment = null;
    private DailyMileFragmentPopular popularFragment = null;
    private HashSet<DailyMileFragment> updateSet;
    private PagerTabStrip pagerTabStrip;
    private AlertDialog menuEntryDialog;
    private AlertDialog menuCommentDialog;
    private DailyMileEntry dailyMileEntry = null;
    private DailyMileComment dailyMileComment = null;
    private MenuItem noteItem;
    private MenuItem meItem;
    private MenuItem friendsItem;
    private MenuItem refreshItem;
    private Vector<Fragment> fragments;
    private Button buttonOfflineRefresh;
    private LinearLayout linearLayoutOffline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();

        try {
            super.onCreate(savedInstanceState);

            // if (GlobalSettings.getInstance(this).checkFirstStart())
            // finish();
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            setContentView(R.layout.activity_dailymile);

            // actionBar = (ActionBar) findViewById(R.id.dailyMileActionbar);
            // // actionBar.setHomeLogo(sessionSummary.getDrawable());
//            getSupportActionBar().setLogo(R.drawable.dashboard_button_dailymile);
//            getSupportActionBar().setDisplayUseLogoEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("dailymile");

            updateSet = new HashSet<DailyMileFragment>();

            fragments = new Vector<Fragment>();
            if (LocationUtils.getLastKnownLocation() != null) {
                nearbyFragment = new DailyMileFragmentNearby(getResources().getString(R.string.nearby), this);
                nearbyFragment.setOnItemTouchListener(this);
                fragments.add(nearbyFragment);
            }
            meFragment = new DailyMileFragmentMe(getResources().getString(R.string.meAndFriends), this);
            meFragment.setOnItemTouchListener(this);
            fragments.add(meFragment);
            int meIdx = fragments.size() - 1;

            popularFragment = new DailyMileFragmentPopular(getResources().getString(R.string.popular), this);
            popularFragment.setOnItemTouchListener(this);
            fragments.add(popularFragment);

            pagerTabStrip = (PagerTabStrip) findViewById(R.id.dailyMileIndicator);
            pagerTabStrip.setTextColor(getResources().getColor(R.color.text));
            pagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.orange));

            mAdapter = new VectorFragmentPagerAdapter(getSupportFragmentManager(), fragments);
            mPager = (ViewPager) findViewById(R.id.dailyMilePager);
            mPager.setAdapter(mAdapter);
            mPager.setCurrentItem(meIdx);
            mPager.setOffscreenPageLimit(2);

            buttonOfflineRefresh = (Button) findViewById(R.id.buttonOfflineRefresh);
            linearLayoutOffline = (LinearLayout) findViewById(R.id.linearLayoutOffline);
            linearLayoutOffline.setVisibility(View.GONE);
            buttonOfflineRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refresh();
                }
            });

            ItemChangedBroadcast.getInstance().addListener(this);
            // AdMobHelper adMobHelper =
        } catch (Exception e) {
            Hint.log(this, e);
            finish();
        }
    }

    private void refreshOptions() {
        final boolean isOnline = Helper.isOnline(getApplicationContext());
        final boolean hasAccount = !TextUtils.isEmpty(DailyMile.getToken());
        final boolean visible = (isOnline && hasAccount);

        meItem.setVisible(visible);
        friendsItem.setVisible(visible);
        noteItem.setVisible(visible);
    }

    private void refresh() {
        try {
            refreshOptions();
        } catch (Exception e) {
        }

        final boolean isOnline = Helper.isOnline(getApplicationContext());
        if (!isOnline) {
            linearLayoutOffline.setVisibility(View.VISIBLE);
            return;
        }

        linearLayoutOffline.setVisibility(View.GONE);
        for (Fragment fragment : fragments)
            ((DailyMileFragment) fragment).refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroy() {
        ItemChangedBroadcast.getInstance().removeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean hasAccount = DailyMile.hasAccount();
        if (GlobalSettings.getInstance(this).getMe() != null)
            meItem = menu.add(GlobalSettings.getInstance(this).getMe().getDisplayName());
        else
            meItem = menu.add(R.string.me);

        meItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_social_person).setVisible(hasAccount)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        friendsItem = menu.add(getString(R.string.friends));
        friendsItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_social_group).setVisible(hasAccount)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        noteItem = menu.add(getString(R.string.note));
        noteItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_content_edit).setVisible(hasAccount)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        refreshItem = menu.add(getString(R.string.refresh));
        refreshItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_refresh)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        refreshOptions();
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item == noteItem) {
            Intent intent = new Intent(this, DailyMilePostNoteActivity.class);
            startActivity(intent);
        } else if (item == friendsItem) {
            try {
                Intent intent = new Intent(this, DailyMileFriendsActivity.class);
                DailyMile dm = new DailyMile(this);
                intent.putExtra("json", dm.getMe().toJson().toString());
                this.startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (item == meItem) {
            try {
                Intent intent = new Intent(this, DailyMileUserActivity.class);
                DailyMile dm = new DailyMile(this);
                intent.putExtra("json", dm.getMe().toJson().toString());
                startActivity(intent);
            } catch (JSONException e) {
                Hint.show(this, e);
                e.printStackTrace();
            }
        } else if (item == refreshItem) {
            try {
                ((DailyMileFragment) mAdapter.getItem(mPager.getCurrentItem())).refresh();
            } catch (Exception e) {
            }
        }
        return true;
    }

    @Override
    public void onUpdateStart(DailyMileFragment fragment) {
        updateSet.add(fragment);
        Hint.log(this, "onUpdateStart name: " + fragment.toString() + ", count: " + updateSet.size());
        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    public void onUpdateFinished(DailyMileFragment fragment) {
        updateSet.remove(fragment);
        Hint.log(this, "onUpdateFinished name: " + fragment.toString() + ", count: " + updateSet.size());
        if (updateSet.isEmpty())
            setProgressBarIndeterminateVisibility(false);
    }

    @Override
    protected void onPause() {
        if (updateSet != null) {
            updateSet.clear();
            setProgressBarIndeterminateVisibility(false);
        }
        super.onPause();
    }

    @Override
    public void onClick(DialogInterface dialog, int item) {
        if (dialog == menuEntryDialog) {
            try {
                if (item == 0) {
                    Intent intent = new Intent(this, DailyMileEntryActivity.class);
                    intent.putExtra("json", dailyMileEntry.getEntry().toJson().toString());
                    this.startActivity(intent);
                } else if (item == 1) {
                    Intent intent = new Intent(this, DailyMileUserActivity.class);
                    intent.putExtra("json", dailyMileEntry.getEntry().getUser().toJson().toString());
                    this.startActivity(intent);
                } else if (item == 2) {
                    Intent intent = new Intent(this, DailyMileFriendsActivity.class);
                    intent.putExtra("json", dailyMileEntry.getEntry().getUser().toJson().toString());
                    this.startActivity(intent);
                }
            } catch (JSONException e) {
                Hint.show(this, e);
                e.printStackTrace();
            }
        } else if (dialog == menuCommentDialog) {
            try {
                if (item == 0) {
                    Intent intent = new Intent(this, DailyMileUserActivity.class);
                    intent.putExtra("json", dailyMileComment.getComment().getUser().toJson().toString());
                    this.startActivity(intent);
                }
            } catch (JSONException e) {
                Hint.show(this, e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEntryClick(DailyMileItem item) {
        if (item instanceof DailyMileEntry) {
            try {
                dailyMileEntry = (DailyMileEntry) item;
                Intent intent = new Intent(this, DailyMileEntryActivity.class);
                intent.putExtra("json", dailyMileEntry.getEntry().toJson().toString());
                this.startActivity(intent);
            } catch (JSONException e) {
                Hint.show(this, e);
                e.printStackTrace();
            }
        } else if (item instanceof DailyMileComment) {
            dailyMileComment = (DailyMileComment) item;
            final CharSequence[] items = {dailyMileComment.getComment().getUser().getDisplayName()};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            menuCommentDialog = builder.setItems(items, this).setCancelable(true).create();
            menuCommentDialog.show();
        }
    }

    @Override
    public boolean onEntryLongClick(DailyMileItem item) {
        if (item instanceof DailyMileEntry) {
            dailyMileEntry = (DailyMileEntry) item;
            final CharSequence[] items = {
                    getResources().getString(R.string.open),
                    dailyMileEntry.getEntry().getUser().getDisplayName(),
                    getResources().getString(R.string.friendsOf) + " "
                            + dailyMileEntry.getEntry().getUser().getDisplayName(),};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            menuEntryDialog = builder.setItems(items, this).setCancelable(true).create();
            menuEntryDialog.show();
        } else if (item instanceof DailyMileComment) {
            dailyMileComment = (DailyMileComment) item;
            final CharSequence[] items = {dailyMileComment.getComment().getUser().getDisplayName()};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            menuCommentDialog = builder.setItems(items, this).setCancelable(true).create();
            menuCommentDialog.show();
        }
        return true;
    }

    @Override
    public void onEntryChanged(PersonEntry entry) {
        for (int i = 0; i < fragments.size(); ++i) {
            DailyMileFragment fragment = (DailyMileFragment) fragments.get(i);
            fragment.updateEntry(entry);
        }
    }
}
