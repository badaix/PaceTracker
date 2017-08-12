package de.badaix.pacetracker.views;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.UrlImageViewHelper;

public class OverviewItem extends LinearLayout {
    protected Context ctx;
    protected String title;
    Vector<Pair<String, String>> vItems;
    int iFirst = 0;
    private TextView tvTitle;
    // private TextView tvLongValue;
    private TextView tvValue;
    private TextView tvSummary;
    private ImageView imageView;

    public OverviewItem(Context context) {
        super(context);
        vItems = new Vector<Pair<String, String>>();
        Init(context);
    }

    public OverviewItem(Context context, String title, String value) {
        super(context);
        this.title = title;
        vItems = new Vector<Pair<String, String>>();
        vItems.add(Pair.create(title, value));
        Init(context);
    }

    public OverviewItem(Context context, Vector<Pair<String, String>> items) {
        super(context);
        if ((items == null) || (items.size() < 1))
            throw new IllegalArgumentException("Items must not be empty");
        this.title = (String) items.firstElement().first;
        vItems = items;
        Init(context);
    }

    public OverviewItem(Context context, String title) {
        super(context);
        this.title = title;
        vItems = new Vector<Pair<String, String>>();
        vItems.add(Pair.create(title, "N/A"));
        Init(context);
    }

    private void inflateUi() {
        LayoutInflater vi = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.overview_item, this);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvValue = (TextView) findViewById(R.id.tvValue);
        tvSummary = (TextView) findViewById(R.id.tvSummary);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setVisibility(View.GONE);
        // tvLongValue = (TextView) findViewById(R.id.tvLongValue);
        update();
    }

    protected void Init(Context context) {
        ctx = context;
        iFirst = 0;
        inflateUi();
        update();
    }

    public void setUrlDrawable(String url) {
        UrlImageViewHelper.getInstance().setUrlDrawable(imageView, url,
                getResources().getDrawable(R.drawable.transparent));
        imageView.setVisibility(View.VISIBLE);
    }

    public void setDrawable(int id) {
        setDrawable(getResources().getDrawable(id));
    }

    public void setDrawable(Drawable drawable) {
        imageView.setImageDrawable(drawable);
        imageView.setVisibility(View.VISIBLE);
    }

    public ImageView getImageView() {
        return this.imageView;
    }

    public void update(Vector<Pair<String, String>> items) {
        this.vItems = items;
        update();
    }

    protected void update() {
        String sSummary = "";
        for (int i = iFirst + 1; i < vItems.size(); ++i)
            sSummary += vItems.get(i).first + ": " + vItems.get(i).second + "\n";

        for (int i = 0; i < iFirst; ++i)
            sSummary += vItems.get(i).first + ": " + vItems.get(i).second + "\n";

        if (vItems.size() > 1)
            sSummary = sSummary.substring(0, sSummary.length() - 1);

        String title = vItems.get(iFirst).first;
        tvTitle.setText(title);
        SpannableString text = new SpannableString(title + "  " + vItems.get(iFirst).second);
        text.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), 0, title.length(), 0);
        tvValue.setText(text, BufferType.SPANNABLE);

        if (!TextUtils.isEmpty(sSummary)) {
            tvSummary.setText(sSummary);
            tvSummary.setVisibility(View.VISIBLE);
        } else
            tvSummary.setVisibility(View.GONE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        update();
    }

    public int getItemCount() {
        return vItems.size();
    }

    public void addItem(String title, String value) {
        vItems.add(Pair.create(title, value));
        update();
    }

    public void addItem(String title) {
        vItems.add(Pair.create(title, "N/A"));
        update();
    }

    public void setValue(int _idx, String value) {
        if (_idx == vItems.size())
            return;

        if (!value.equals(vItems.get(_idx).second)) {
            vItems.set(_idx, Pair.create(vItems.get(_idx).first, value));
            update();
        }
    }

    public void setValue(String title, String value) {
        for (int i = 0; i < vItems.size(); ++i) {
            if (vItems.get(i).first.equals(title)) {
                if (!value.equals(vItems.get(i).second)) {
                    vItems.set(i, Pair.create(title, value));
                    update();
                    return;
                }
            }
        }
    }

    public void setKeyValue(int _idx, String key, String value) {
        if (_idx == vItems.size())
            return;

        if (!key.equals(vItems.get(_idx).first) || !value.equals(vItems.get(_idx).second)) {
            vItems.set(_idx, Pair.create(key, value));
            update();
        }
    }

    public void swapItems() {
        iFirst = (iFirst + 1) % vItems.size();
        update();
    }

    public int getFirstIndex() {
        return iFirst;
    }

    public void setFirstIndex(int index) {
        iFirst = index;
        update();
    }

    public String getTitle() {
        return title;
    }

}
