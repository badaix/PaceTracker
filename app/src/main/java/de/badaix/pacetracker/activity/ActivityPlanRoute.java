package de.badaix.pacetracker.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import de.badaix.pacetracker.FindPlaceAdapter;
import de.badaix.pacetracker.R;
import de.badaix.pacetracker.preferences.AdvancedListPreference;
import de.badaix.pacetracker.preferences.AutoCompleteEditTextPreference;
import de.badaix.pacetracker.preferences.IconListPreference;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

public class ActivityPlanRoute extends AppCompatPreferenceActivity implements OnPreferenceChangeListener,
        OnPreferenceClickListener, OnClickListener {
    private AutoCompleteEditTextPreference textPrefFrom;
    private AutoCompleteEditTextPreference textPrefTo;
    private IconListPreference listPrefType;
    private AdvancedListPreference listPrefStrategy;
    private AdvancedListPreference listPrefLocale;
    private Button btnPlanRoute;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences_plan_route);
        setContentView(R.layout.activity_plan_route);

        textPrefFrom = (AutoCompleteEditTextPreference) findPreference("textPrefFrom");
        textPrefFrom.setOnPreferenceChangeListener(this);
        if (LocationUtils.getLastKnownLocation() != null) {
            Address address;
            try {
                address = LocationUtils.getAddressFromLocation(this, LocationUtils.getLastKnownLocation());
                if (address != null) {
                    String text = "";
                    if (address.getThoroughfare() != null)
                        text = address.getThoroughfare() + " ";
                    if (address.getSubThoroughfare() != null)
                        text += address.getSubThoroughfare() + ", ";
                    else
                        text += ", ";
                    if (address.getPostalCode() != null)
                        text += address.getPostalCode() + " ";
                    if (address.getLocality() != null)
                        text += address.getLocality();
                    if (address.getCountryName() != null)
                        text += ", " + address.getCountryName();
                    textPrefFrom.setText(text);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        textPrefTo = (AutoCompleteEditTextPreference) findPreference("textPrefTo");
        textPrefTo.setOnPreferenceChangeListener(this);

        listPrefType = (IconListPreference) findPreference("listPrefRouteType");
        listPrefType.setDefaultValue("bicycle");
        listPrefType.setOnPreferenceChangeListener(this);
        listPrefType.setSummary(listPrefType.getEntry());
        listPrefType.setBackgroundDrawable(R.drawable.history_item_image_background);

        listPrefStrategy = (AdvancedListPreference) findPreference("listPrefRouteStrategy");
        listPrefStrategy.setDefaultValue("DEFAULT_STRATEGY");
        listPrefStrategy.setOnPreferenceChangeListener(this);
        listPrefStrategy.setValueText(listPrefStrategy.getEntry());

        listPrefLocale = (AdvancedListPreference) findPreference("listPrefRouteLocale");
        listPrefLocale.setDefaultValue(Locale.getDefault().toString());
        listPrefLocale.setOnPreferenceChangeListener(this);
        listPrefLocale.setValueText(listPrefLocale.getEntry());

        FindPlaceAdapter adapter = new FindPlaceAdapter(this, android.R.layout.simple_dropdown_item_1line);
        textPrefFrom.getEditText().setAdapter(adapter);
        textPrefTo.getEditText().setAdapter(adapter);
        textPrefTo.setSummary(textPrefTo.getText());
        textPrefFrom.setSummary(textPrefFrom.getText());

        btnPlanRoute = (Button) findViewById(R.id.btnPlanRoute);
        btnPlanRoute.setEnabled((textPrefFrom.getText() != null) && (textPrefTo.getText() != null)
                && !TextUtils.isEmpty(textPrefFrom.getText()) && !TextUtils.isEmpty(textPrefTo.getText()));
        btnPlanRoute.setOnClickListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ((preference == textPrefFrom) || (preference == textPrefTo)) {
            preference.setSummary((String) newValue);
            btnPlanRoute.setEnabled(!TextUtils.isEmpty(textPrefFrom.getSummary())
                    && !TextUtils.isEmpty(textPrefTo.getSummary()));
            return true;
        }

        if (preference == listPrefType) {
            ((AdvancedListPreference) preference).setSummary(((AdvancedListPreference) preference)
                    .getEntry((String) newValue));
            return true;
        } else if (preference instanceof AdvancedListPreference) {
            ((AdvancedListPreference) preference).setValueText(((AdvancedListPreference) preference)
                    .getEntry((String) newValue));
            return true;
        }

        return false;
    }

    @Override
    public void onClick(View view) {
        if (view == btnPlanRoute) {
            ProgressDialog dialog = ProgressDialog.show(this, "", getResources().getString(R.string.planningRoute),
                    true);
            dialog.show();
            PlanRouteTask planRouteTask = new PlanRouteTask(this, dialog);
            planRouteTask.execute(new MapQuestSettings(textPrefFrom.getText(), textPrefTo.getText(), listPrefType
                    .getValue(), listPrefStrategy.getValue(), listPrefLocale.getValue()));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference arg0) {
        return false;
    }

    class MapQuestSettings {
        String from;
        String to;
        String type;
        String strategy;
        String locale;

        MapQuestSettings(String from, String to, String type, String strategy, String locale) {
            this.from = from;
            this.to = to;
            this.type = type;
            this.strategy = strategy;
            this.locale = locale;
        }
    }

    class PlanRouteTask extends AsyncTask<MapQuestSettings, Void, Route> {
        private Context context;
        private Exception exception = null;
        private ProgressDialog progressDialog = null;

        public PlanRouteTask(Context context, ProgressDialog progressDialog) {
            this.context = context;
            this.progressDialog = progressDialog;
        }

        // http://open.mapquestapi.com/directions/v1/route?outFormat=json&routeType=bicycle&from=50.7753455,6.0838868&to=51.5463105,4.1040252&generalize=0.1&shapeFormat=cmp&locale=de_DE

        /**
         * The system calls this to perform work in a worker thread and delivers
         * it the parameters given to AsyncTask.execute()
         */
        protected Route doInBackground(MapQuestSettings... item) {
            MapQuestSettings mapQuestSettings = item[0];

            String url = "http://open.mapquestapi.com/directions/v1/route?outFormat=json&generalize=0.1&shapeFormat=cmp"
                    + "&key=" + GlobalSettings.getInstance().getMetaData("map_quest.api_key")
                    + "&routeType="
                    + mapQuestSettings.type
                    + "&locale="
                    + mapQuestSettings.locale
                    + "&unit=k"
                    + "&roadGradeStrategy=" + mapQuestSettings.strategy;

            try {
                Address address = LocationUtils.getAddressFromName(context, mapQuestSettings.from);
                if (address == null)
                    throw new IOException(getResources().getString(R.string.errorResolvingFrom));
                if ((address.getFeatureName() != null) && (address.getFeatureName().length() >= 5))
                    mapQuestSettings.from = address.getFeatureName();
                else if (address.getThoroughfare() != null)
                    mapQuestSettings.from = address.getThoroughfare();
                url += "&from=" + address.getLatitude() + "," + address.getLongitude();
                address = LocationUtils.getAddressFromName(context, mapQuestSettings.to);
                if (address == null)
                    throw new IOException(getResources().getString(R.string.errorResolvingTo));
                if ((address.getFeatureName() != null) && (address.getFeatureName().length() >= 5))
                    mapQuestSettings.to = address.getFeatureName();
                else if (address.getThoroughfare() != null)
                    mapQuestSettings.to = address.getThoroughfare();
                url += "&to=" + address.getLatitude() + "," + address.getLongitude();
                Hint.log(this, "url: " + url);

                HttpGet httpGet = new HttpGet(url);
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse response = httpClient.execute(httpGet);
                if (response.getStatusLine().getStatusCode() / 100 == 2) {
                    String responseEntity = new java.util.Scanner(response.getEntity().getContent())
                            .useDelimiter("\\A").next();

                    try {
                        // Tranform the string into a json object
                        final JSONObject jResponse = new JSONObject(responseEntity);
                        // Get the route object
                        JSONObject jsonRoute = jResponse.getJSONObject("route");
                        JSONObject jsonMeta = new JSONObject();
                        jsonMeta.put("copyright",
                                "Directions Courtesy of <a href=\"http://www.mapquest.com/\" target=\"_blank\">MapQuest</a>");
                        jsonMeta.put("source", "mapquest:" + url);
                        // jsonRoute.put("type", mapQuestSettings.type);
                        jsonMeta.put("from", mapQuestSettings.from);
                        jsonMeta.put("to", mapQuestSettings.to);
                        jsonRoute.put("meta", jsonMeta);
                        return new Route(jsonRoute);
                    } catch (JSONException e) {
                        this.exception = e;
                        return null;
                    }
                }
            } catch (IOException e) {
                this.exception = e;
                e.printStackTrace();
                return null;
            }

            return null;
        }

        /**
         * The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground()
         */
        protected void onPostExecute(Route route) {
            progressDialog.dismiss();
            if (exception != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(
                        getResources().getString(R.string.errorPlanningRoute) + ":\n" + exception.getMessage())
                        .setCancelable(false)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                // for (GeoPos pos : route.getPositions()) {
                // Hint.log(this, pos.latitude + ", " + pos.longitude + ", " +
                // pos.distance);
                // }
                GlobalSettings.getInstance(getApplicationContext()).route = route;
                Intent intent = new Intent(context, ActivityViewRoute.class);
                intent.putExtra("openIntent", "RoutesActivity");
                intent.putExtra("view", false);
                startActivity(intent);
            }
        }
    }

}
