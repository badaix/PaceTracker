package de.badaix.pacetracker.views.dailymile;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;

import java.util.HashSet;
import java.util.Set;

import de.badaix.pacetracker.util.Hint;

public class CommentAnimator implements AnimationListener {
    private static CommentAnimator instance = null;
    private Animation fadeIn;
    private Animation fadeOut;
    private AnimationSet animationSet;
    private int fadeInDuration = 500; // Configure time values here
    private int timeBetween = 3000;
    private int fadeOutDuration = 500;
    private Set<DailyMileEntry> entries;

    private CommentAnimator() {
        fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); // add this
        fadeIn.setDuration(fadeInDuration);

        fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); // and this
        fadeOut.setStartOffset(fadeInDuration + timeBetween);
        fadeOut.setDuration(fadeOutDuration);

        animationSet = new AnimationSet(false); // change to false
        animationSet.addAnimation(fadeIn);
        animationSet.addAnimation(fadeOut);
        animationSet.setRepeatCount(1);

        entries = new HashSet<DailyMileEntry>();
    }

    // fadeOut.setAnimationListener(null);
    // animationSet.cancel();

    public static CommentAnimator getInstance() {
        if (instance == null) {
            instance = new CommentAnimator();
        }
        return instance;
    }

    public void start(DailyMileEntry entry) {
        if (entry == null)
            return;
        entries.add(entry);
        entry.cycleComments(animationSet);
        if (!animationSet.hasStarted()) {
            fadeOut.setAnimationListener(this);
            animationSet.startNow();
        }
    }

    public void stop() {
        entries.clear();
        fadeOut.setAnimationListener(null);
        animationSet.cancel();
        instance = null;
        Hint.log(this, "Stopped");
    }

    public void remove(DailyMileEntry entry) {
        entries.remove(entry);
        if (entries.isEmpty()) {
            stop();
        }
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        try {
            Hint.log(this, "onAnimationEnd");
            entries.remove(null);
            if (entries.isEmpty()) {
                instance = null;
                return;
            }

            for (DailyMileEntry entry : entries) {
                entry.cycleComments(animationSet);
            }
            animationSet.startNow();
        } catch (Exception e) {
            Hint.log(this, e);
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // Hint.log(this, "onAnimationRepeat");
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // Hint.log(this, "onAnimationStart");
    }

}
