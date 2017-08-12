package de.badaix.pacetracker.session.post;

import de.badaix.pacetracker.session.SessionSummary;

public interface PostSessionListener {
    void onSessionPostet(SessionSummary sessionSummary);

    void onPostSessionFailed(SessionSummary sessionSummary, Exception exception);
}
