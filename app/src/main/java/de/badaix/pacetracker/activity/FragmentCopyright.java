package de.badaix.pacetracker.activity;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.util.ListTagHandler;

public class FragmentCopyright extends Fragment {

    private String fragmentName = "Copyright";
    private TextView tvAbout;

    public void setTitle(String title) {
        fragmentName = title;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_copyright, container, false);
        tvAbout = (TextView) v.findViewById(R.id.textView);
        tvAbout.setLinksClickable(true);
        tvAbout.setMovementMethod(LinkMovementMethod.getInstance());
        String version = "";
        try {
            version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
        }
        String about = "<strong>PaceTracker</strong><br>Version " + version
                + "<br>Copyright 2013, <a href=\"http://www.linkedin.com/pub/johannes-pohl/65/6a6/253\" target=\"_blank\">Johannes Pohl</a>"
                + "<br>E-mail: <a href=\"mailto:johannes@johannes-pohl.de?subject=PaceTracker\">johannes@johannes-pohl.de</a>"
                + "<br>Google+: <a href=\"https://plus.google.com/102961499560575158465\" rel=\"publisher\">PaceTracker</a>";

//				+ "<br>Copyright 2013, <a href=\"http://badaix.de\" target=\"_blank\">Bad Aix</a>";


        about = about + "<p>" + getResources().getString(R.string.copyright) + "</p>";
        about = about + "<p><strong>Disclaimer</strong><br>" + getResources().getString(R.string.disclaimer)
                + "</br></p>";
        tvAbout.setText(Html.fromHtml(about, null, new ListTagHandler()));
        return v;
    }

    @Override
    public String toString() {
        return fragmentName;
    }
}
