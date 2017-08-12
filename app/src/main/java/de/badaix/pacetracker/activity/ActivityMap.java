package de.badaix.pacetracker.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.util.Hint;

public class ActivityMap extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hint.log(this, "onCreate");
        setContentView(R.layout.activity_map);
        FragmentSessionGmsMap mapFragment = new FragmentSessionGmsMap();
        this.getSupportActionBar().setTitle(R.string.map);
        mapFragment.setOffline(false);
        mapFragment.setTitle(getResources().getString(R.string.map));
        mapFragment.getMyLocation(true);
        mapFragment.lock(true);
        getSupportFragmentManager().beginTransaction().add(R.id.map_parent, mapFragment).commit();
    }
}
