package de.badaix.pacetracker.views;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;

import de.badaix.pacetracker.views.dailymile.DailyMileEntry;

public class EditTextDialog extends AlertDialog {
    private EditText dialogEditText;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;
    private DailyMileEntry dailyMileEntry;
    private Context context;

    public EditTextDialog(Context context) {
        super(context);
        this.context = context;
        dialogEditText = new EditText(getContext());
        builder = new AlertDialog.Builder(getContext());
        dialog = null;
    }

    public AlertDialog getDialog(DailyMileEntry entry, String title, String message, OnClickListener onClickListener) {
        dailyMileEntry = entry;
        builder.setView(dialogEditText);
        builder.setTitle(title);
        builder.setMessage(message).setCancelable(true)
                .setPositiveButton(context.getResources().getText(android.R.string.ok), onClickListener)
                .setNegativeButton(context.getResources().getText(android.R.string.cancel), null);
        dialogEditText.requestFocus();
        dialog = builder.create();
        return dialog;
    }

    public DailyMileEntry getDailyMileEntry() {
        return dailyMileEntry;
    }

    @Override
    public void show() {
    }

    public String getText() {
        return dialogEditText.getText().toString();
    }

    public void setText(CharSequence text) {
        this.dialogEditText.setText(text);
    }

    public AlertDialog getDialog() {
        return dialog;
    }
}
