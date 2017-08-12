package de.badaix.pacetracker.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;

import de.badaix.pacetracker.util.Hint;

public class FragmentGms extends SupportMapFragment {

    public static FragmentGms newInstance() {
        Hint.log("FragmentGms", "newInstance");
        return (FragmentGms) SupportMapFragment.newInstance();
    }

    public static SupportMapFragment newInstance(GoogleMapOptions options) {
        Hint.log("SupportMapFragment", "newInstance");
        return (FragmentGms) SupportMapFragment.newInstance(options);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup view, Bundle savedInstance) {
        View layout = super.onCreateView(inflater, view, savedInstance);
        Hint.log(this, "onCreateView");

        FrameLayout frameLayout = new FrameLayout(getActivity());
        frameLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        ((ViewGroup) layout).addView(frameLayout, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        return layout;
    }
}
