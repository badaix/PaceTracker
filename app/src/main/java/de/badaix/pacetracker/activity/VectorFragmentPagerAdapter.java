package de.badaix.pacetracker.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.Vector;

public class VectorFragmentPagerAdapter extends FragmentPagerAdapter {
    private Vector<Fragment> guiFragments;

    public VectorFragmentPagerAdapter(FragmentManager fm, Vector<Fragment> fragments) {
        super(fm);
        guiFragments = fragments;
    }

    @Override
    public int getCount() {
        return guiFragments.size();
    }

	/*
     * @Override public void destroyItem(View container, int position, Object
	 * object) { }
	 */

    @Override
    public CharSequence getPageTitle(int position) {
        return guiFragments.get(position).toString();
    }

    @Override
    public Fragment getItem(int position) {
        return guiFragments.get(position);
    }
}
