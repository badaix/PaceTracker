package de.badaix.pacetracker.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.SessionPersistance;
import de.badaix.pacetracker.session.Exporter;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.session.SessionReader;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.session.post.PostGPlus;
import de.badaix.pacetracker.session.post.PostSessionDialog;
import de.badaix.pacetracker.session.post.PostSessionListener;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.SessionSettings;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.util.FileUtils;
import de.badaix.pacetracker.util.GoogleEarth;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.views.HistoryItem;

@SuppressLint("ValidFragment")
public class FragmentHistoryList extends ListFragment implements FragmentHistory, OnItemSelectedListener,
        OnClickListener, PostSessionListener {
    private HistoryItemAdapter adapter;
    private AlertDialog deleteDialog;
    private ExportDialog exportDialog;
    private HistoryItem selectedItem;
    private MenuItem menuExport;
    private MenuItem menuEarth;
    // private MenuItem menuReplay;
    private MenuItem menuPostDm;
    private MenuItem menuPostGp;
//    private MenuItem menuPostFb;
    private MenuItem menuOpen;
    private MenuItem menuDelete;
    private Context context;
    private ActivitySessionHistory parent;
    private SessionPersistance sessionPersistance;
    private Intent intent;
    private String title;
    private PostGPlus postGPlus;

    // private TextView textView;

    // private HistoryItemAdapter adapter;

    public FragmentHistoryList(String title, ActivitySessionHistory parent, Context context) {
        this.context = context;
        this.parent = parent;
        this.title = title;
        this.adapter = new HistoryItemAdapter(context, null, 0);
        setListAdapter(adapter);
        sessionPersistance = SessionPersistance.getInstance(getActivity());
    }

    @Override
    public void onUpdateGui() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onUpdate(String filter) {
        String[] fields = {"*"};
        adapter.swapCursor(sessionPersistance.querySessions(fields, filter, null, "", null));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(getListView());
    }

    @Override
    public void onPause() {
        adapter.swapCursor(null);
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history_list, container, false);
        // textView = (TextView) v.findViewById(R.id.textView);
        // textView.setVisibility(View.GONE);
        // ListView lv = (ListView) v.findViewById(android.R.id.list);
        // View spacer = new View(getActivity());
        // spacer.setMinimumHeight(Helper.dipToPix(getActivity(), 5));
        // spacer.setBackgroundColor(Color.TRANSPARENT);
        // lv.addHeaderView(spacer, null, false);
        // lv.addFooterView(spacer, null, false);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        exportDialog = null;
        deleteDialog = null;
        selectedItem = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectedItem = (HistoryItem) v;
        GlobalSettings.getInstance(getActivity()).setSessionSummary(selectedItem.getSessionSummary());
        // SessionSettings.getInstance().setSession(null);
        intent = new Intent(this.context, ActivityViewSession.class);
        startActivity(intent);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if ((exportDialog != null) && (dialog == exportDialog.getDialog())) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                SharedPreferences.Editor editor = GlobalSettings.getInstance(getActivity()).getPrefs().edit();
                // editor.putString("exportMail", exportDialog.getRecipient());
                editor.putBoolean("exportToKml", exportDialog.exportToKml());
                // editor.putBoolean("exportToKmlTour",
                // exportDialog.exportToKmlTour());
                editor.putBoolean("exportToGpx", exportDialog.exportToGpx());
                editor.putBoolean("exportToCsv", exportDialog.exportToCsv());
                editor.commit();

                if (exportDialog.exportToKml()
                        // || exportDialog.exportToKmlTour()
                        || exportDialog.exportToGpx() || exportDialog.exportToCsv()) {
                    ProgressDialog progressDialog = ProgressDialog.show(getActivity(), "",
                            getResources().getString(R.string.historyExportSession), true);
                    new ExportSessionTask(progressDialog).execute(exportDialog);
                }
            }
        } else if (dialog == deleteDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (selectedItem != null) {
                    sessionPersistance.deleteSession(selectedItem.getId());
                    parent.updateHistory();
                    // ((SessionHistoryActivity)getActivity()).updateHistory();
                }
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedItem = (HistoryItem) info.targetView;
        if (selectedItem != null) {
            menu.setHeaderTitle(selectedItem.getSessionSummary().getName(context));
        }
        menuOpen = menu.add(getResources().getString(R.string.historyMenuOpen));
        // menuReplay =
        // menu.add(getResources().getString(R.string.historyMenuReplay));
        boolean hasLocationInfo = selectedItem.getSessionSummary().getSettings().getPositionProvider()
                .hasLocationInfo();
        if (hasLocationInfo) {
            menuExport = menu.add(getResources().getString(R.string.historyMenuExport));
        }
        if ((selectedItem != null) && selectedItem.isDailyMileCompatible()
                && (selectedItem.getSessionSummary().getSettings().getDailyMileId() == -1))
            menuPostDm = menu.add(getResources().getString(R.string.historyMenuPostDm));
        if ((selectedItem != null) && (selectedItem.getSessionSummary().getSettings().getGPlusId() == -1))
            menuPostGp = menu.add(getResources().getString(R.string.historyMenuPostGp));
//		if ((selectedItem != null) && TextUtils.isEmpty(selectedItem.getSessionSummary().getSettings().getFbId()))
//        menuPostFb = menu.add(getResources().getString(R.string.historyMenuPostFb));
        if (hasLocationInfo) {
            menuEarth = menu.add(getResources().getString(R.string.viewEarth));
        }
        menuDelete = menu.add(getResources().getString(R.string.historyMenuDelete));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DailyMile.DM_AUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED)
                Hint.show(getActivity(), getString(R.string.not_authorized));
        } else if ((requestCode == PostGPlus.GPLUS_REQUEST_CODE) && (resultCode == Activity.RESULT_OK)) {
            try {
                postGPlus.updateSession();
                menuPostGp.setVisible(false);
            } catch (IOException e) {
                Hint.show(getActivity(), e);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item == menuOpen) {
            Log.d("PaceTracker", "Export clicked");
            GlobalSettings.getInstance(getActivity()).setSessionSummary(selectedItem.getSessionSummary());
            intent = new Intent(this.context, ActivityViewSession.class);
            startActivity(intent);
        } else if (item == menuPostDm) {
            PostSessionDialog postSessionDialog = new PostSessionDialog();
            postSessionDialog.post(this.getActivity(), getActivity().getSupportFragmentManager(), this,
                    selectedItem.getSessionSummary());
        } else if (item == menuPostGp) {
            postGPlus = new PostGPlus(getActivity());
            postGPlus.post(this.getActivity(), selectedItem.getSessionSummary());
//		} else if (item == menuPostFb) {
//			final FragmentFacebookShare facebookFragment = new FragmentFacebookShare();
//			facebookFragment.setSession(selectedItem.getSessionSummary());
//			facebookFragment.setListener(new OnShareListener() {
//
//				@Override
//				public void onSuccess(SessionSummary session, String postId) {
//					session.getSettings().setFbId(postId);
//					SessionWriter sessionWriter = new SessionWriter(context);
//					try {
//						sessionWriter.updateSession(session);
//						((ActivitySessionHistory) getActivity()).updateHistory();
//					} catch (IOException e) {
//						Hint.log(this, e);
//					}
//					facebookFragment.dismiss();
//				}
//
//				@Override
//				public void onException(SessionSummary session, Exception exception) {
//					Hint.log(this, exception);
//					facebookFragment.dismiss();
//				}
//			});
//			facebookFragment.show(this.getChildFragmentManager(),
//					"facebookFragment");
        } else if (item == menuExport) {
            SharedPreferences prefs = GlobalSettings.getInstance(getActivity()).getPrefs();
            exportDialog = new ExportDialog(getActivity(), selectedItem, this);
            // exportDialog.setRecipient(prefs.getString("exportMail", ""));
            exportDialog.doExportToGpx(prefs.getBoolean("exportToGpx", false));
            exportDialog.doExportToKml(prefs.getBoolean("exportToKml", true));
            // exportDialog.doExportToKmlTour(prefs.getBoolean("exportToKmlTour",
            // false));
            exportDialog.doExportToCsv(prefs.getBoolean("exportToCsv", false));
            exportDialog.getDialog().show();
        } else if (item == menuDelete) {
            Log.d("PaceTracker", "Delete clicked");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.confirm_delete_session).setCancelable(false)
                    .setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this);
            deleteDialog = builder.create();
            deleteDialog.show();
        } else if (item == menuEarth) {
            GoogleEarth.playTrack(getActivity(), selectedItem.getSessionSummary());
        }

        return true;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public void onSessionPostet(SessionSummary sessionSummary) {
        ((ActivitySessionHistory) getActivity()).updateHistory();
        // adapter.getCursor().requery();
        // onUpdateGui();
        // getListView().invalidate();
    }

    @Override
    public void onPostSessionFailed(SessionSummary sessionSummary, Exception exception) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.error_posting_session) + ":\n" + exception.getMessage())
                .setCancelable(false).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private class HistoryItemAdapter extends CursorAdapter {

        public HistoryItemAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // if (view == null) {
            // view = newView(context, cursor, null);
            // return;
            // }

            try {
                SessionSummary sessionSummary = sessionPersistance.getSummary(cursor);
                ((HistoryItem) view).setSessionSummary(sessionSummary);
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            try {
                SessionSummary sessionSummary = sessionPersistance.getSummary(cursor);
                return new HistoryItem(context, sessionSummary);
            } catch (Exception e) {
                Hint.log(this, e);
                return null;
            }
        }
    }

    private class ExportSessionTask extends AsyncTask<ExportDialog, Void, Void> {
        private Session session;
        private Context context;
        private List<String> fileList;
        private ExportDialog exportDialog;
        private ProgressDialog progressDialog;
        private Time time;
        private Exception exception = null;

        public ExportSessionTask(ProgressDialog progressDialog) {
            this.progressDialog = progressDialog;
            progressDialog.show();
            context = progressDialog.getContext();
        }

        /**
         * The system calls this to perform work in a worker thread and delivers
         * it the parameters given to AsyncTask.execute()
         */
        protected Void doInBackground(ExportDialog... item) {
            exportDialog = item[0];
            session = SessionFactory.getInstance().getSessionByType(exportDialog.histItem.getSessionType(), null,
                    new SessionSettings(false));
            SessionReader reader = new SessionReader();
            try {
                reader.readSessionFromFile(exportDialog.histItem.getFilename(), session);
            } catch (Exception e) {
                exception = e;
                return null;
            }
            session.setFilename(exportDialog.histItem.getFilename());

            time = new Time();
            time.set(session.getSessionStart().getTime());
            File file = FileUtils.getFilename(context, session.getName(context) + "_" + time.format2445() + ".kml",
                    "export");
            if (file != null) {
                try {
                    fileList = new Vector<String>();
                    if (exportDialog.exportToKml()) {
                        fileList.add(file.getAbsolutePath());
                        FileWriter writer;
                        writer = new FileWriter(file);
                        Hint.log(this, "exporter start");
                        Exporter.toKml(session, writer);
                        Hint.log(this, "exporter finish");
                        writer.close();
                    }
                    // if (exportDialog.exportToKmlTour()) {
                    // file = FileUtils.getFilename(
                    // context,
                    // session.getName(context) + "_"
                    // + time.format2445() + ".tour.kml",
                    // "export");
                    // fileList.add(file.getAbsolutePath());
                    // FileWriter writer;
                    // writer = new FileWriter(file);
                    // Hint.log(this, "exporter start");
                    // Exporter.toKmlTour(context, session, writer, 4.0f);
                    // Hint.log(this, "exporter finish");
                    // writer.close();
                    // }
                    if (exportDialog.exportToGpx()) {
                        file = FileUtils.getFilename(context, session.getName(context) + "_" + time.format2445()
                                + ".gpx", "export");
                        fileList.add(file.getAbsolutePath());
                        FileWriter writer;
                        writer = new FileWriter(file);
                        Exporter.toGpx(session, writer, 3.0f);
                        writer.close();
                    }
                    if (exportDialog.exportToCsv()) {
                        file = FileUtils.getFilename(context, session.getName(context) + "_" + time.format2445()
                                + ".csv", "export");
                        fileList.add(file.getAbsolutePath());
                        FileWriter writer;
                        writer = new FileWriter(file);
                        Exporter.toCsv(session, writer);
                        writer.close();
                    }
                } catch (IOException e) {
                    exception = e;
                }
            }

            return null;
        }

        /**
         * The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground()
         */
        protected void onPostExecute(Void result) {
            progressDialog.cancel();
            if (exception != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.error_exporting_session) + ":\n" + exception.getMessage())
                        .setCancelable(false)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else
                Helper.SendMail(context, ""/* exportDialog.getRecipient() */, "", session.getName(context) + ": "
                        + time.format2445(), "", fileList);
        }
    }

    class ExportDialog implements OnMultiChoiceClickListener {
        private Context context;
        private AlertDialog dialog;
        // private EditText emailText;
        private OnClickListener onClickListener;
        private boolean[] activeItems = {false, false, false};
        private Vector<CharSequence> exportItems;
        private View view;
        private HistoryItem histItem;

        public ExportDialog(Context context, HistoryItem histItem, OnClickListener onClickListener) {
            // GlobalSettings.getInstance(context).getPrefs().
            exportItems = new Vector<CharSequence>();
            exportItems.add("GPX");
            exportItems.add("KML");
            // exportItems.add("KML tour");
            if (GlobalSettings.getInstance().isDebug())
                exportItems.add("CSV");

            this.context = context;
            this.onClickListener = onClickListener;
            this.histItem = histItem;
            dialog = null;
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.export_dialog, null);
            // emailText = (EditText) view.findViewById(R.id.etRecipient);
        }

        public HistoryItem getHistoryItem() {
            return histItem;
        }

        // public String getRecipient() {
        // return emailText.getText().toString();
        // }
        //
        // public void setRecipient(String recipient) {
        // emailText.setText(recipient);
        // }

        public void doExportToGpx(boolean export) {
            activeItems[0] = export;
        }

        public void doExportToKml(boolean export) {
            activeItems[1] = export;
        }

        // public void doExportToKmlTour(boolean export) {
        // activeItems[2] = export;
        // }

        public void doExportToCsv(boolean export) {
            activeItems[2] = export;
        }

        public boolean exportToGpx() {
            return activeItems[0];
        }

        public boolean exportToKml() {
            return activeItems[1];
        }

        // public boolean exportToKmlTour() {
        // return activeItems[2];
        // }

        public boolean exportToCsv() {
            return activeItems[2];
        }

        AlertDialog getDialog() {
            if (dialog == null) {
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.doExport)
                        .setCancelable(false)
                        .setView(view)
                        .setPositiveButton(R.string.doExport, this.onClickListener)
                        .setNegativeButton(android.R.string.cancel, this.onClickListener)
                        .setMultiChoiceItems(exportItems.toArray(new CharSequence[exportItems.size()]), activeItems,
                                this);
                dialog = builder.create();
            }

            return dialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            activeItems[which] = isChecked;
        }
    }

}
