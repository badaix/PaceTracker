package de.badaix.pacetracker.session.post;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.preferences.ImageArrayAdapter;
import de.badaix.pacetracker.session.Felt;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.util.Hint;

public class PostSessionDialog extends DialogFragment implements OnItemSelectedListener, OnClickListener {

    private EditText editTextHowDidIt;
    private EditText editTextTitle;
    private CheckBox checkBoxAttachRoute;
    private View contentView;
    private Spinner spinner;
    private SessionSummary sessionSummary = null;
    private PostSessionListener listener = null;

    public static Dialog createDailymileLoginDialog(final Context context) {
        return new AlertDialog.Builder(context).setCancelable(true).setMessage(R.string.loginDailymile)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DailyMile dm = new DailyMile(context);
                        dm.authorize();
                    }
                }).create();
    }

    public void post(Context context, FragmentManager fragmentManager, PostSessionListener listener,
                     SessionSummary sessionSummary) {
        this.listener = listener;
        this.sessionSummary = sessionSummary;
        if (!DailyMile.hasAccount()) {
            createDailymileLoginDialog(context).show();
            return;
        }
        this.show(fragmentManager, "fragment_edit_name");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater factory = LayoutInflater.from(getActivity());
        contentView = factory.inflate(R.layout.fragment_post_session, null);
        spinner = (Spinner) contentView.findViewById(R.id.spinnerIFelt);
        editTextHowDidIt = (EditText) contentView.findViewById(R.id.editTextHowDidIt);
        editTextTitle = (EditText) contentView.findViewById(R.id.editTextTitle);
        checkBoxAttachRoute = (CheckBox) contentView.findViewById(R.id.checkBoxAttachRoute);

        editTextHowDidIt.setText(sessionSummary.getSettings().getComment());
        editTextTitle.setText(sessionSummary.getSettings().getDescription());

        ImageArrayAdapter spinnerAdapter = new ImageArrayAdapter(this.getActivity(),
                R.layout.image_list_item_single_choice, this.getResources().getStringArray(R.array.feelNames), this
                .getResources().getStringArray(R.array.feelImageValues), 2);
        spinner.setAdapter(spinnerAdapter);

        if (sessionSummary.getSettings().getFelt() != null)
            spinner.setSelection(sessionSummary.getSettings().getFelt().getIndex());
        else
            spinner.setSelection(Felt.GOOD.getIndex());

        if (!sessionSummary.getSettings().getPositionProvider().hasLocationInfo()) {
            checkBoxAttachRoute.setVisibility(View.GONE);
            checkBoxAttachRoute.setChecked(false);
        }

        spinner.setOnItemSelectedListener(this);
        spinner.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager inputManager = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(v.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                return false;
            }
        });

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.historyMenuPostDm).setView(contentView)
                .setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this)
                .setInverseBackgroundForced(true);

        AlertDialog dialog = alertDialogBuilder.create();
        return dialog;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String[] listValue = this.getResources().getStringArray(R.array.feelListValues);
        sessionSummary.getSettings().setFelt(Felt.fromString(listValue[(int) id]));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            sessionSummary.getSettings().setDescription(editTextTitle.getText().toString());
            sessionSummary.getSettings().setComment(editTextHowDidIt.getText().toString());

            Hint.log(this, "Felt: " + sessionSummary.getSettings().getFelt());
            Hint.log(this, "Comment: " + sessionSummary.getSettings().getComment());
            Hint.log(this, "Description: " + sessionSummary.getSettings().getDescription());
            Hint.log(this, "Upload: " + checkBoxAttachRoute.isChecked());
            try {
                DailyMile dailyMile = new DailyMile(this.getActivity());
                if (DailyMile.getToken().length() == 0)
                    dailyMile.authorize(this);
                else {
                    new PostSessionTask(this.getActivity(), listener, checkBoxAttachRoute.isChecked())
                            .execute(sessionSummary);
                }
            } catch (Exception e) {
                Hint.show(getActivity(), e);
            }
        }
        this.dismiss();
    }

}
