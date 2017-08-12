package de.badaix.pacetracker.views.dailymile;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.social.dailymile.Comment;
import de.badaix.pacetracker.social.dailymile.DailyMileHelper;
import de.badaix.pacetracker.social.dailymile.Distance.DistanceUnit;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Distance.Unit;

public class DailyMileEntry extends DailyMileItem implements OnClickListener {
    public LinearLayout linearLayout;
    protected PersonEntry entry;
    private TextView tvDistTime;
    private TextView tvUser;
    private TextView tvDesctiption;
    private TextView tvMessage;
    private TextView tvActivity;
    private TextView tvLocation;
    private TextView tvDate;
    private TextView tvCommentCount;
    private View vCommentSeperator;
    private View vSeperator;
    private boolean collapsed;
    private ImageView ivDmUser;
    // private LinearLayout llDmEntry;
    // private LinearLayout llHeader;
    private LinearLayout llDmAddEntries;
    private LinearLayout llCommentCount;
    private ImageView imageViewComment;
    private TextView textViewComment;
    private LinearLayout linearLayoutComment;
    private int commentIdx;
    private int commentCount;

    public DailyMileEntry(Context context) {
        super(context);
    }

    public DailyMileEntry(Context context, PersonEntry entry, boolean collapsed) {
        this(context);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.view_dailymile_item, this);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        llCommentCount = (LinearLayout) findViewById(R.id.llCommentCount);
        tvCommentCount = (TextView) findViewById(R.id.tvCommentCount);
        tvUser = (TextView) findViewById(R.id.tvDmUser);
        tvActivity = (TextView) findViewById(R.id.tvDmActivity);
        tvDistTime = (TextView) findViewById(R.id.tvDmDistTime);
        tvDesctiption = (TextView) findViewById(R.id.tvDmDesc);
        tvMessage = (TextView) findViewById(R.id.tvDmMessage);
        tvLocation = (TextView) findViewById(R.id.tvDmLocation);
        tvDate = (TextView) findViewById(R.id.tvDmDate);
        ivDmUser = (ImageView) findViewById(R.id.ivDmUser);
        vSeperator = (View) findViewById(R.id.seperator);
        vCommentSeperator = (View) findViewById(R.id.commentSeperator);
        imageViewComment = (ImageView) findViewById(R.id.imageViewComment);
        textViewComment = (TextView) findViewById(R.id.textViewComment);
        // llDmEntry = (LinearLayout) findViewById(R.id.llDmEntry);
        // llHeader = (LinearLayout) findViewById(R.id.llHeader);
        llDmAddEntries = (LinearLayout) findViewById(R.id.llDmAddEntries);
        linearLayoutComment = (LinearLayout) findViewById(R.id.linearLayoutComment);
        this.collapsed = collapsed;

        // setOnClickListener(null);
        // llHeader.setOnClickListener(this);

        this.entry = entry;
        update();
    }

    public ImageView getUserImage() {
        return ivDmUser;
    }

    public void setUserImage(ImageView imageView) {
        this.ivDmUser = imageView;
    }

    public void addChildView(View view) {
        llDmAddEntries.addView(view);
        llDmAddEntries.setVisibility(VISIBLE);
    }

    public void cycleComments(Animation animation) {
        if (!collapsed || entry.getComments().isEmpty()) {
            linearLayoutComment.setAnimation(null);
            return;
        }

        Comment comment = entry.getComments().get(commentIdx);
        textViewComment.setText(comment.getBody());
        textViewComment.setSelected(true);
        UrlImageViewHelper.getInstance().setUrlDrawable(imageViewComment, comment.getUser().getPhotoUrl(),
                R.drawable.user_mini, null);

        // Hint.log(this, "commentIdx: " + commentIdx);
        commentIdx = (commentIdx + 1) % entry.getComments().size();
        // Hint.log(this, "next commentIdx: " + commentIdx);

        if (entry.getComments().size() > 1)
            linearLayoutComment.setAnimation(animation);
        else
            linearLayoutComment.setAnimation(null);
    }

    public void update() {
        for (int i = 0; i < llDmAddEntries.getChildCount(); ++i)
            if (llDmAddEntries.getChildAt(i) instanceof DailyMileComment)
                DailyMileCommentFactory.getInstance(this.getContext()).recycleComment(
                        (DailyMileComment) llDmAddEntries.getChildAt(i));

        llDmAddEntries.removeAllViews();

        tvMessage.setMovementMethod(null);
        commentCount = entry.getComments().size();
        String comments = tvCommentCount.getContext().getString(R.string.comments);
        if (commentCount == 1)
            comments = tvCommentCount.getContext().getString(R.string.comment);
        tvCommentCount.setText(commentCount + " " + comments);

        if (!collapsed || entry.getComments().isEmpty()) {
            vCommentSeperator.setVisibility(GONE);
            llCommentCount.setVisibility(GONE);
        } else {
            vCommentSeperator.setVisibility(VISIBLE);
            llCommentCount.setVisibility(VISIBLE);
        }

        commentIdx = 0;
        cycleComments(null);
        CommentAnimator.getInstance().remove(this);
        if (entry.getComments().size() > 1)
            CommentAnimator.getInstance().start(this);

        if (!collapsed) {
            tvMessage.setEllipsize(null);
            tvMessage.setMaxLines(Integer.MAX_VALUE);
            tvMessage.post(null);
        } else {
            tvMessage.setEllipsize(null);
            // tvMessage.setEllipsize(TextUtils.TruncateAt.END);
            tvMessage.setMaxLines(12);
            tvMessage.post(new Runnable() {
                @Override
                public void run() {
                    if (tvMessage.getLineCount() > 11) {
                        int lineEndIndex = tvMessage.getLayout().getLineEnd(11);
                        String text = tvMessage.getText().subSequence(0, lineEndIndex - 3) + "...";
                        tvMessage.setText(text);
                    }
                }
            });
        }

        ivDmUser.setImageResource(R.drawable.user_mini);
        if (entry.getUser() != null) {
            tvUser.setText(entry.getUser().getDisplayName());
            tvUser.setVisibility(View.VISIBLE);
            UrlImageViewHelper.getInstance().setUrlDrawable(ivDmUser, entry.getUser().getPhotoUrl(),
                    R.drawable.user_mini, null);
        } else {
            tvUser.setVisibility(View.GONE);
        }

        try {
            tvDate.setText(android.text.format.DateUtils.getRelativeTimeSpanString(entry.getAt().getTime(),
                    new Date().getTime(), android.text.format.DateUtils.SECOND_IN_MILLIS));
            // if (DateUtils.isToday(entry.getAt()))
            // tvDate.setText(DateUtils.toTimeString(entry.getAt(),
            // DateFormat.SHORT, Locale.getDefault()));
            // else
            // tvDate.setText(DateUtils.toDateString(entry.getAt(),
            // DateFormat.SHORT, Locale.getDefault()));
        } catch (Exception e) {
        }

        if (entry.getLocation() != null)
            tvLocation.setText(entry.getLocation().getName());
        else
            tvLocation.setText("");

        if (entry.getWorkout() != null) {
            String activity = "";
            activity += entry.getWorkout().getActivityType().toLocaleString(getContext());
            tvActivity.setText(activity);
            tvActivity.setVisibility(VISIBLE);
        } else
            tvActivity.setVisibility(GONE);

        tvDistTime.setText("");
        tvDesctiption.setText("");
        try {
            String text = "";
            if (entry.getWorkout() != null) {
                if (entry.getWorkout().getDistance() != null) {
                    double distance = entry.getWorkout().getDistance().getValue();
                    DistanceUnit dmUnit = entry.getWorkout().getDistance().getUnit();
                    Distance.Unit ptUnit = DailyMileHelper.getDistanceUnit(dmUnit);
                    distance *= ptUnit.getFactor();
                    if (GlobalSettings.getInstance(context).getDistSystem() == Distance.System.METRIC) {
                        if (ptUnit == Unit.MILES)
                            ptUnit = Unit.KILOMETERS;
                        else if (ptUnit == Unit.YARDS)
                            ptUnit = Unit.METER;
                    } else {
                        if (ptUnit == Unit.KILOMETERS)
                            ptUnit = Unit.MILES;
                        else if (ptUnit == Unit.METER)
                            ptUnit = Unit.YARDS;
                    }

                    text = Distance.distanceToString(distance, ptUnit, 2) + " "
                            + ptUnit.toLocaleString(getContext(), false) + " / ";
                } else
                    text += entry.getWorkout().getActivityType() + " / ";
                text += DateUtils.secondsToHHMMSSString(entry.getWorkout().getDuration());
                tvDistTime.setText(text);
                tvDesctiption.setText(entry.getWorkout().getTitle().trim());
            }
        } catch (Exception e) {
        }

        if (TextUtils.isEmpty(tvDistTime.getText()))
            tvDistTime.setVisibility(GONE);
        else
            tvDistTime.setVisibility(VISIBLE);

        if (TextUtils.isEmpty(tvDesctiption.getText()))
            tvDesctiption.setVisibility(GONE);
        else
            tvDesctiption.setVisibility(VISIBLE);

        if (entry.getMessage() != null) {
            tvMessage.setText(entry.getMessage().trim());
            tvMessage.setVisibility(VISIBLE);
        } else
            tvMessage.setVisibility(GONE);

        for (int i = 0; i < entry.getMedia().size(); ++i) {
            DailyMileMedia media = new DailyMileMedia(getContext(), entry.getMedia().get(i));
            addChildView(media);
        }

        if ((llDmAddEntries.getChildCount() == 0) && (tvDistTime.getVisibility() == GONE)
                && (tvDesctiption.getVisibility() == GONE) && (tvMessage.getVisibility() == GONE)
                && entry.getMedia().isEmpty()) {
            vSeperator.setVisibility(GONE);
            llDmAddEntries.setVisibility(GONE);
        } else {
            vSeperator.setVisibility(VISIBLE);
            llDmAddEntries.setVisibility(VISIBLE);
        }

        if (!collapsed) {
            for (int i = 0; i < entry.getComments().size(); ++i) {
                DailyMileComment comment = DailyMileCommentFactory.getInstance(this.getContext()).createComment(
                        entry.getComments().get(i));
                addChildView(comment);
                comment.setOnClickListener(this);
            }
        }
    }

    public PersonEntry getEntry() {
        return entry;
    }

    public void setEntry(PersonEntry personEntry, boolean collapsed) {
        if (personEntry.equals(entry) && (personEntry.getComments().size() == commentCount)
                && (this.collapsed == collapsed))
            return;

        this.collapsed = collapsed;
        this.entry = personEntry;
        update();
    }

    @Override
    public void onClick(View v) {
        if (!(v instanceof DailyMileItem))
            return;
        if (onItemTouchListener != null)
            onItemTouchListener.onEntryClick((DailyMileItem) v);
    }
}
