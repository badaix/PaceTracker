package de.badaix.pacetracker.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.GlobalSettings;

public class ActivityAbout extends AppCompatActivity {
    private FragmentCopyright copyrightFragment = null;
    private FragmentVersionHistory versionHistoryFragment = null;
    private VectorFragmentPagerAdapter mAdapter;
    private ViewPager mPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setTitle(R.string.about);

        Vector<Fragment> fragments = new Vector<Fragment>();

        copyrightFragment = new FragmentCopyright();
        fragments.add(copyrightFragment);

        versionHistoryFragment = new FragmentVersionHistory();
        versionHistoryFragment.setTitle(getString(R.string.version_history));
        fragments.add(versionHistoryFragment);

        PagerTabStrip pagerTabStrip = (PagerTabStrip) findViewById(R.id.pagerTabStrip);
        pagerTabStrip.setTextColor(getResources().getColor(R.color.text));
        pagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.orange));

        mAdapter = new VectorFragmentPagerAdapter(getSupportFragmentManager(), fragments);
        mPager = (ViewPager) findViewById(R.id.viewPager);
        mPager.setAdapter(mAdapter);
    }

}
