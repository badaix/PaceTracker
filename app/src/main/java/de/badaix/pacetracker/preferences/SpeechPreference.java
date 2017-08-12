package de.badaix.pacetracker.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.VoiceFeedbackSettings;
import de.badaix.pacetracker.util.TextItemPair;

public class SpeechPreference extends ValueDialogPreference implements OnItemSelectedListener {

    private Spinner speakDistance;
    private Spinner speakDuration;
    private CheckBox speakDistTotal;
    private CheckBox speakDistDuration;
    private CheckBox speakDistTotalDuration;

    private CheckBox speakDurationTotal;
    private CheckBox speakDurationDistance;
    private CheckBox speakDurationTotalDistance;

    private ArrayAdapter<TextItemPair<Integer>> distanceAdapter;
    private ArrayAdapter<TextItemPair<Integer>> durationAdapter;

    private VoiceFeedbackSettings feedback;

    public SpeechPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.speech_preference);
    }

    private void init() {
        feedback = GlobalSettings.getInstance().getVoiceFeedback();

        speakDistTotal.setChecked(feedback.speakDistTotal);
        speakDistDuration.setChecked(feedback.speakDistDuration);
        speakDistTotalDuration.setChecked(feedback.speakDistTotalDuration);
        speakDurationTotal.setChecked(feedback.speakDurationTotal);
        speakDurationDistance.setChecked(feedback.speakDurationDistance);
        speakDurationTotalDistance.setChecked(feedback.speakDurationTotalDistance);
        for (int i = 0; i < distanceAdapter.getCount(); ++i) {
            if (distanceAdapter.getItem(i).getItem().intValue() == feedback.distanceInterval) {
                speakDistance.setSelection(i);
                break;
            }
        }
        for (int i = 0; i < durationAdapter.getCount(); ++i) {
            if (durationAdapter.getItem(i).getItem().intValue() == feedback.durationInterval) {
                speakDuration.setSelection(i);
                break;
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
    }

    @Override
    protected void onBindDialogView(View view) {
        speakDistance = (Spinner) view.findViewById(R.id.speakDistance);
        speakDuration = (Spinner) view.findViewById(R.id.speakDuration);
        speakDistance.setOnItemSelectedListener(this);
        speakDuration.setOnItemSelectedListener(this);

        distanceAdapter = new ArrayAdapter<TextItemPair<Integer>>(this.getContext(),
                android.R.layout.simple_spinner_item);
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        String[] text = this.getContext().getResources().getStringArray(R.array.speach_interval_distance);
        String[] values = this.getContext().getResources().getStringArray(R.array.speach_interval_distance_values);
        for (int i = 0; i < text.length; ++i) {
            int value = Integer.valueOf(values[i]);
            if (value != 0)
                distanceAdapter.add(new TextItemPair<Integer>(text[i] + " "
                        + GlobalSettings.getInstance().getDistUnit().toShortString(), value));
            else
                distanceAdapter.add(new TextItemPair<Integer>(text[i], value));
        }
        speakDistance.setAdapter(distanceAdapter);

        durationAdapter = new ArrayAdapter<TextItemPair<Integer>>(this.getContext(),
                android.R.layout.simple_spinner_item);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        text = this.getContext().getResources().getStringArray(R.array.speach_interval_duration);
        values = this.getContext().getResources().getStringArray(R.array.speach_interval_duration_values);
        for (int i = 0; i < text.length; ++i) {
            durationAdapter.add(new TextItemPair<Integer>(text[i], Integer.valueOf(values[i])));
        }
        speakDuration.setAdapter(durationAdapter);

        speakDistTotal = (CheckBox) view.findViewById(R.id.speakDistTotal);
        speakDistDuration = (CheckBox) view.findViewById(R.id.speakDistDuration);
        speakDistTotalDuration = (CheckBox) view.findViewById(R.id.speakDistTotalDuration);
        speakDurationTotal = (CheckBox) view.findViewById(R.id.speakDurationTotal);
        speakDurationDistance = (CheckBox) view.findViewById(R.id.speakDurationDistance);
        speakDurationTotalDistance = (CheckBox) view.findViewById(R.id.speakDurationTotalDistance);

        super.onBindDialogView(view);
        init();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onDialogClosed(boolean positiveResult) {

        if (!positiveResult)
            return;

        if (shouldPersist()) {
            feedback.speakDistTotal = speakDistTotal.isChecked();
            feedback.speakDistDuration = speakDistDuration.isChecked();
            feedback.speakDistTotalDuration = speakDistTotalDuration.isChecked();

            feedback.speakDurationTotal = speakDurationTotal.isChecked();
            feedback.speakDurationDistance = speakDurationDistance.isChecked();
            feedback.speakDurationTotalDistance = speakDurationTotalDistance.isChecked();

            TextItemPair<Integer> item = ((TextItemPair<Integer>) speakDistance.getSelectedItem());
            feedback.distanceInterval = item.getItem();

            item = (TextItemPair<Integer>) speakDuration.getSelectedItem();
            feedback.durationInterval = item.getItem();
            GlobalSettings.getInstance().storeVoiceFeedback();
        }
        super.onDialogClosed(positiveResult);
        this.callChangeListener(feedback);
    }

    private void enableDistance(boolean enabled) {
        speakDistTotal.setEnabled(enabled);
        speakDistDuration.setEnabled(enabled);
        speakDistTotalDuration.setEnabled(enabled);
    }

    private void enableTime(boolean enabled) {
        speakDurationTotal.setEnabled(enabled);
        speakDurationDistance.setEnabled(enabled);
        speakDurationTotalDistance.setEnabled(enabled);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == speakDistance)
            enableDistance(id != 0);
        else if (parent == speakDuration)
            enableTime(id != 0);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

}
