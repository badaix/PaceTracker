package de.badaix.pacetracker.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.badaix.pacetracker.R;

public class VersionItem extends LinearLayout {
    private TextView tvVersion;
    private TextView tvVersionInfo;

    public VersionItem(Context context) {
        this(context, null, null);
    }

    public VersionItem(Context context, String version, String versionInfo) {
        super(context);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.version_item, this);
        tvVersion = (TextView) findViewById(R.id.tvVersion);
        tvVersionInfo = (TextView) findViewById(R.id.tvVersionInfo);
        setVersion(version, versionInfo);
    }

    public void setVersion(String version, String versionInfo) {
        tvVersion.setText("Version " + version);
        tvVersionInfo.setText(versionInfo);
    }

}
