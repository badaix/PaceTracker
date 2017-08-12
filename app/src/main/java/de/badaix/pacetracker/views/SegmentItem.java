package de.badaix.pacetracker.views;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.Segment;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.util.Distance;

public class SegmentItem extends LinearLayout implements OnClickListener {
    private TextView tvInstructions;
    private TextView tvDistance;
    private TextView tvName;
    private ImageView ivSign;
    private Segment segment;
    private Context context;

    public SegmentItem(Context context) {
        this(context, null);
    }

    public SegmentItem(Context context, Segment segment) {
        super(context);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.segment_item, this);
        tvInstructions = (TextView) findViewById(R.id.tvInstructions);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        tvName = (TextView) findViewById(R.id.tvName);
        ivSign = (ImageView) findViewById(R.id.ivSign);
        this.context = context;
        this.segment = segment;
        update();
    }

    public void update() {
        if (segment == null)
            return;

        if (TextUtils.isEmpty(segment.getImageUrl()))
            ivSign.setVisibility(View.GONE);
        else {
            ivSign.setVisibility(View.VISIBLE);
            UrlImageViewHelper.getInstance()
                    .setUrlDrawable(ivSign, segment.getImageUrl(), R.drawable.transparent, null);
        }

        String instructions = segment.getInstruction();
        if (TextUtils.isEmpty(instructions))
            instructions = segment.getName();

        tvName.setText(segment.getName());
        tvInstructions.setText(instructions);
        tvDistance.setText(Distance.distanceToString(segment.getDistance(), 2) + " "
                + GlobalSettings.getInstance(context).getDistUnit().toShortString());
    }

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
        update();
    }

    @Override
    public void onClick(View v) {
    }
}
