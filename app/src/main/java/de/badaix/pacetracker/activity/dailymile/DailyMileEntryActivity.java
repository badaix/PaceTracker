package de.badaix.pacetracker.activity.dailymile;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.activity.VectorFragmentPagerAdapter;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.ActionbarUrlImage;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.views.dailymile.DailyMileComment;
import de.badaix.pacetracker.views.dailymile.DailyMileEntry;
import de.badaix.pacetracker.views.dailymile.DailyMileItem;

public class DailyMileEntryActivity extends AppCompatActivity implements OnStreamUpdateListener,
        OnItemTouchListener, OnClickListener, OnPostListener, MenuItem.OnMenuItemClickListener {
    VectorFragmentPagerAdapter mAdapter;
    ViewPager mPager;
    private DailyMileFragmentEntry entryFragment = null;
    private PersonEntry entry;
    private ImageView sendButton;
    private EditText editText;
    private MenuItem userItem;
    private MenuItem friendsItem;
    private RelativeLayout footer;

    // private ClickAction userAction;
    // private ClickAction friendsAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_dailymile_entry);

        Bundle extras = getIntent().getExtras();
        if ((extras == null) || !extras.containsKey("json"))
            throw new IllegalArgumentException("no entry specified");
        try {
            entry = new PersonEntry(new JSONObject(extras.getString("json")));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e.getMessage());
        }

        footer = (RelativeLayout) findViewById(R.id.footer);
        sendButton = (ImageView) findViewById(R.id.ivSend);
        editText = (EditText) findViewById(R.id.editText);
        if (Build.VERSION.SDK_INT <= 10)
            editText.setTextColor(getResources().getColor(android.R.color.primary_text_light));
        // actionBar = (ActionBar) findViewById(R.id.actionbar);
        getSupportActionBar().setTitle(
                getResources().getString(R.string.entry) + " - " + entry.getUser().getDisplayName());
        //
        // userAction = new ClickAction(this,
        // R.drawable.ic_action_social_person);
        // actionBar.addAction(userAction);
        //
        // friendsAction = new ClickAction(this,
        // R.drawable.ic_action_social_group);
        // actionBar.addAction(friendsAction);

        sendButton.setOnClickListener(this);
        findViewById(R.id.relativeLayout).requestFocus();

        entryFragment = new DailyMileFragmentEntry(entry, this);
        Vector<Fragment> fragments = new Vector<Fragment>();
        entryFragment.setOnItemTouchListener(this);
        fragments.add(entryFragment);

        mAdapter = new VectorFragmentPagerAdapter(getSupportFragmentManager(), fragments);
        mPager = (ViewPager) findViewById(R.id.dailyMilePager);
        mPager.setAdapter(mAdapter);
        ItemChangedBroadcast.getInstance().addListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        footer.setVisibility(DailyMile.hasAccount() ? View.VISIBLE : View.GONE);
        if (entry.getUser() != null)
            new ActionbarUrlImage(this, entry.getUser().getMiniPhotoUrl(), getResources().getDrawable(
                    R.drawable.user_mini));
    }

    @Override
    public void onDestroy() {
        ItemChangedBroadcast.getInstance().removeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        userItem = menu.add(entry.getUser().getDisplayName());
        userItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_social_person)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        friendsItem = menu.add(getString(R.string.friends));
        friendsItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_social_group)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    @Override
    public void onClick(View view) {
        if (editText.getText().length() == 0)
            return;
        ProgressDialog progressDialog = ProgressDialog.show(this, "",
                getResources().getString(R.string.postingComment), true);

        DailyMileEntry item = (DailyMileEntry) entryFragment.getListAdapter().getView(0, null, null);
        CommentTask commentTask = new CommentTask(this.getApplicationContext(), item, progressDialog, this);
        commentTask.execute(editText.getText().toString());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If the back Key was pressed, then finish the program.
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Intent intent = new Intent();
            try {
                intent.putExtra("PersonEntry", entry.toJson().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            setResult(RESULT_OK, intent);
            finish();
        }

        // else return the normal function of whatever key was pressed
        return super.onKeyDown(keyCode, event);
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
    public void onPostDone() {
        editText.setText("");
    }

    @Override
    public void onPostFailed(Exception e) {
    }

    @Override
    public void onEntryClick(DailyMileItem item) {
        if (item instanceof DailyMileComment) {
            try {
                Intent intent = new Intent(this, DailyMileUserActivity.class);
                intent.putExtra("json", ((DailyMileComment) item).getComment().getUser().toJson().toString());
                this.startActivity(intent);
            } catch (JSONException e) {
                Hint.show(this, e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEntryChanged(PersonEntry entry) {
        entryFragment.updateEntry(entry);
    }

    @Override
    public boolean onEntryLongClick(DailyMileItem item) {
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item == friendsItem) {
            try {
                Intent intent = new Intent(this, DailyMileFriendsActivity.class);
                intent.putExtra("json", entry.getUser().toJson().toString());
                this.startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (item == userItem) {
            try {
                Intent intent = new Intent(this, DailyMileUserActivity.class);
                intent.putExtra("json", entry.getUser().toJson().toString());
                startActivity(intent);
            } catch (JSONException e) {
                Hint.show(this, e);
                e.printStackTrace();
            }
        }
        return true;
    }

}
