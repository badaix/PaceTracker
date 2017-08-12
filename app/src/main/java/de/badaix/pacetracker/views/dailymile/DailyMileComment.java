package de.badaix.pacetracker.views.dailymile;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.Stack;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.social.dailymile.Comment;

class DailyMileCommentFactory {
    private static DailyMileCommentFactory instance = null;
    private static Context ctx = null;
    private Stack<DailyMileComment> comments = new Stack<DailyMileComment>();

    public static DailyMileCommentFactory getInstance(Context context) {
        if (instance == null) {
            instance = new DailyMileCommentFactory();
        }
        ctx = context;
        return instance;
    }

    public void recycleComment(DailyMileComment comment) {
        // UrlImageViewHelper.discardImageView(comment.getUserImage(),
        // ctx.getResources().getDrawable(R.drawable.user_mini));
        comment.setOnClickListener(null);
        synchronized (comments) {
            comments.push(comment);
        }
        // Hint.log(this, "+Comments: " + comments.size());
    }

    public DailyMileComment createComment(Comment comment) {
        // Hint.log(this, "-Comments: " + comments.size());
        DailyMileComment result = null;
        synchronized (comments) {
            if (!comments.empty()) {
                result = comments.pop();
                result.setComment(comment);
                result.update();
                return result;
            }
        }
        return new DailyMileComment(ctx, comment);
    }
}

public class DailyMileComment extends DailyMileItem {
    private TextView tvUser;
    private TextView tvMessage;
    private TextView tvDate;
    private Comment comment;
    private ImageView ivDmUser;

    public DailyMileComment(Context context) {
        this(context, null);
    }

    public DailyMileComment(Context context, Comment comment) {
        super(context);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.view_dailymile_comment, this);
        tvUser = (TextView) findViewById(R.id.tvDmUser);
        tvMessage = (TextView) findViewById(R.id.tvDmMessage);
        tvDate = (TextView) findViewById(R.id.tvDmDate);
        ivDmUser = (ImageView) findViewById(R.id.ivDmUser);
        this.comment = comment;
        update();
    }

    public ImageView getUserImage() {
        return ivDmUser;
    }

    public void setUserImage(ImageView imageView) {
        this.ivDmUser = imageView;
    }

    public void update() {
        if (comment == null)
            return;

        tvUser.setText(comment.getUser().getDisplayName());
        if (comment.getBody() != null)
            tvMessage.setText(comment.getBody().trim());

        try {
            tvDate.setText(android.text.format.DateUtils.getRelativeTimeSpanString(comment.getCreatedAt().getTime(),
                    new Date().getTime(), android.text.format.DateUtils.SECOND_IN_MILLIS));
            // if (DateUtils.isToday(comment.getCreatedAt()))
            // tvDate.setText(DateUtils.toTimeString(comment.getCreatedAt(),
            // DateFormat.SHORT, Locale.getDefault()));
            // else
            // tvDate.setText(DateUtils.toDateString(comment.getCreatedAt(),
            // DateFormat.SHORT, Locale.getDefault()));
        } catch (Exception e) {
        }

        ivDmUser.setImageDrawable(getResources().getDrawable(R.drawable.user_mini));
        UrlImageViewHelper.getInstance().setUrlDrawable(ivDmUser, comment.getUser().getPhotoUrl(),
                R.drawable.user_mini, null);
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }
}
