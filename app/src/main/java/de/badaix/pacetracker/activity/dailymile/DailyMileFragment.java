package de.badaix.pacetracker.activity.dailymile;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.json.JSONException;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import javax.security.auth.login.LoginException;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.dailymile.DMItem;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.views.dailymile.DailyMileItem;

abstract class DailyMileItemAdapter extends ArrayAdapter<DMItem> implements OnScrollListener {
    protected Context context;
    protected OnItemTouchListener touchListener = null;
    protected Vector<? extends DMItem> entries = null;
    protected Interpolator interpolator;
    protected int size, height, width, previousPostition, lastFirstVisible;
    protected boolean shouldAnimate;
    protected boolean scrollsDown;

    public DailyMileItemAdapter(Context context, int textViewResourceId, OnItemTouchListener touchListener,
                                Vector<DMItem> list) {
        super(context, textViewResourceId, list);
        this.context = context;
        this.touchListener = touchListener;
        this.entries = list;
        interpolator = new DecelerateInterpolator();
        previousPostition = -1;
        lastFirstVisible = -1;
        shouldAnimate = false;
        scrollsDown = true;
    }

    public void addEntry(DMItem newEntry) {
        int pos = getPosition(newEntry);
        if (pos != -1)
            remove(getItem(pos));
        add(newEntry);
    }

    public void mergeEntries(Vector<? extends DMItem> newItems) {
        synchronized (entries) {
            for (int i = 0; i < newItems.size(); ++i) {
                DMItem entry = newItems.get(i);
                addEntry(entry);
            }
            // for (int i=0; i<getCount(); ++i)
            // Hint.log(this, i + ": " + getItem(i).getId() + "  " +
            // getItem(i).getAt());
            Hint.log(this, "Entries in adapter: " + getCount());
        }
    }

    @Override
    public void notifyDataSetChanged() {
        Collections.sort(entries);
        super.notifyDataSetChanged();
    }

    abstract protected View getItemView(int position, View convertView, ViewGroup parent);// {

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DailyMileItem view = (DailyMileItem) getItemView(position, convertView, parent);
        // view.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        view.setOnItemTouchListener(touchListener);
        // Hint.log(this, "Pos: " + position + ", prev: " + previousPostition);
        if (android.os.Build.VERSION.SDK_INT < 11)
            shouldAnimate = false;

        if ((position > previousPostition) && shouldAnimate) {

            view.setTranslationX(0.0F);
            view.setTranslationY(view.getHeight() / 4);
            view.setRotationX(20.0F);
            view.setScaleX(0.9F);
            view.setScaleY(0.8F);

            ViewPropertyAnimator localViewPropertyAnimator = view.animate().rotationX(0.0F).rotationY(0.0F)
                    .translationX(0).translationY(0).setDuration(200).scaleX(1.0F).scaleY(1.0F)
                    .setInterpolator(interpolator);

            localViewPropertyAnimator.setStartDelay(0).start();
            previousPostition = position;
        }
        // if (Math.abs(previousPostition - position) <= 1)
        if ((position < previousPostition) && !scrollsDown)
            previousPostition = position;

        return view;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (lastFirstVisible < firstVisibleItem)
            scrollsDown = true;
        else if (lastFirstVisible > firstVisibleItem)
            scrollsDown = false;
        lastFirstVisible = firstVisibleItem;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        shouldAnimate = (scrollState == SCROLL_STATE_FLING);
    }
}

public abstract class DailyMileFragment extends ListFragment implements OnScrollListener, OnItemClickListener,
        OnItemLongClickListener {
    protected DailyMileItemAdapter adapter = null;
    protected int pageToLoad = 1;
    protected String title = "";
    protected ProgressBar progressBar;
    protected Date lastUpdate = null;
    protected OnStreamUpdateListener streamUpdateListener = null;
    protected OnItemTouchListener onItemTouchListener = null;
    protected Vector<DMItem> entries = null;
    protected boolean isRefreshing = false;
    protected Button buttonLogin = null;
    private UpdateTask updateTask = null;
    private ListView listView = null;

    public DailyMileFragment(String title, OnStreamUpdateListener streamUpdateListener) {

        this.title = title;
        this.streamUpdateListener = streamUpdateListener;
        entries = new Vector<DMItem>();
    }

    public void setOnItemTouchListener(OnItemTouchListener touchListener) {
        this.onItemTouchListener = touchListener;
    }

    public abstract Vector<? extends DMItem> getEntries(int page) throws JSONException, IOException, LoginException;

    public abstract Vector<? extends DMItem> getEntries(Date since) throws JSONException, IOException, LoginException;

    public boolean hasMoreEntries() {
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dailymile_list, container, false);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        buttonLogin = (Button) v.findViewById(R.id.buttonLogin);
        buttonLogin.setVisibility(View.GONE);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView = (ListView) this.getListView();
        listView.setSmoothScrollbarEnabled(false);
        listView.setOnScrollListener(this);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        // listView.setOnRefreshListener(this);
    }

    protected void executeUpdate(UpdateTask updateTask) {
        if ((updateTask != null) && (updateTask.getStatus() == AsyncTask.Status.RUNNING))
            updateTask.cancel(true);
        this.updateTask = updateTask;
        streamUpdateListener.onUpdateStart(this);
        if (Build.VERSION.SDK_INT >= 13)
            updateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
        else
            updateTask.execute(this);
    }

    public void refresh() {
        if (isRefreshing || (this.getActivity() == null))
            return;

        isRefreshing = true;
        executeUpdate(new UpdateTask(this.getActivity(), lastUpdate));
    }

    public void onUpdateFinished() {
        // if (isRefreshing)
        // listView.onRefreshComplete();
        isRefreshing = false;
    }

    abstract protected void createAdapter();

    public void updateEntry(DMItem updateItem) {
        createAdapter();
        for (DMItem item : entries) {
            if (item.equals(updateItem)) {
                adapter.addEntry(updateItem);
                adapter.notifyDataSetChanged();
                return;
            }
        }
    }

    public void addEntries(Vector<? extends DMItem> entries) {
        createAdapter();
        if ((entries != null) && !entries.isEmpty()) {
            adapter.mergeEntries(entries);
            if (!adapter.isEmpty()) {
                lastUpdate = new Date(adapter.getItem(0).getAt().getTime());
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStart() {
        Hint.log(this, "onStart");
        super.onStart();
        if ((updateTask != null) && (updateTask.getStatus() == AsyncTask.Status.RUNNING))
            return;
        executeUpdate(new UpdateTask(this.getActivity(), lastUpdate));
    }

    @Override
    public void onStop() {
        Hint.log(this, "onStop");
        if ((updateTask != null) && (updateTask.getStatus() == AsyncTask.Status.RUNNING))
            updateTask.cancel(true);
        super.onStop();
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (adapter != null)
            adapter.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

        // Check if the last view is visible
        if (totalItemCount == 0)
            return;

        if (!Helper.isOnline(this.getActivity()))
            return;
        // Hint.log(this, "onScroll: " + firstVisibleItem + "  " +
        // visibleItemCount + "  " + totalItemCount);

        if (firstVisibleItem + visibleItemCount + 15 >= totalItemCount) {
            if (hasMoreEntries() && ((updateTask == null) || (updateTask.getStatus() != Status.RUNNING))) {
                Hint.log(this, "updating");
                executeUpdate(new UpdateTask(this.getActivity(), null));
            }
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (adapter != null)
            adapter.onScrollStateChanged(view, scrollState);
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (onItemTouchListener != null)
            onItemTouchListener.onEntryClick((DailyMileItem) view);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if ((onItemTouchListener != null) && (view instanceof DailyMileItem)) {
            return onItemTouchListener.onEntryLongClick((DailyMileItem) view);
        }
        return false;
    }

    protected class UpdateTask extends AsyncTask<DailyMileFragment, Vector<? extends DMItem>, Void> {
        private Context ctx;
        private DailyMileFragment dailyMileFragment;
        private Exception exception = null;
        private Date since = null;

        public UpdateTask(Context ctx, Date since) {
            this.ctx = ctx;
            this.since = since;
        }

        /**
         * The system calls this to perform work in a worker thread and delivers
         * it the parameters given to AsyncTask.execute()
         */

        // private void cacheEntries(Vector<PersonEntry> entries) {
        // synchronized (DailyMileEntryCache.getInstance(ctx)) {
        // for (PersonEntry entry : entries) {
        // DailyMileEntryCache.getInstance(ctx).put(entry.getId(), entry);
        // }
        // }
        // }
        @SuppressWarnings("unchecked")
        protected Void doInBackground(DailyMileFragment... item) {
            Vector<? extends DMItem> entries = null;
            Hint.log(this, "start updating");
            dailyMileFragment = item[0];

            if (!Helper.isOnline(this.ctx)) {
                publishProgress(entries);
                return null;
            }

            try {
                if (since != null) {
                    entries = getEntries(since);
                    publishProgress(entries);
                } else {
                    entries = getEntries(pageToLoad);
                    pageToLoad++;
                    publishProgress(entries);
                }
            } catch (LoginException e) {
                entries = null;
            } catch (Exception e) {
                exception = e;
                entries = null;
            }
            Hint.log(this, "finished updating");
            return null;
        }

        protected void onProgressUpdate(Vector<? extends DMItem>... progress) {
            try {
                Vector<? extends DMItem> entries = progress[0];
                if (entries != null)
                    Hint.log(this, "Entries: " + entries.size());
                progressBar.setVisibility(View.GONE);
                dailyMileFragment.addEntries(entries);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected void onCancelled() {
            Hint.log(this, "update cancelled");
        }

        protected void onPostExecute(Void result) {
            progressBar.setVisibility(View.GONE);
            streamUpdateListener.onUpdateFinished(dailyMileFragment);
            if (exception != null) {
                Hint.show(ctx, exception);
            }

            dailyMileFragment.onUpdateFinished();
        }
    }

}
