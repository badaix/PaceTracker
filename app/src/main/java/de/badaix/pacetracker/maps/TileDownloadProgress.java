package de.badaix.pacetracker.maps;

public class TileDownloadProgress {
    private int done;
    private int total;
    private int failed;
    private TileDownloadState state;
    private String url;
    private Exception exception;
    public TileDownloadProgress(int done, int failed, int total, TileDownloadState state, String url,
                                Exception exception) {
        this.done = done;
        this.total = total;
        this.failed = failed;
        this.url = url;
        this.state = state;
        this.exception = exception;
    }

    public TileDownloadState getState() {
        return state;
    }

    public Exception getException() {
        return exception;
    }

    public int getDone() {
        return done;
    }

    public int getFailed() {
        return failed;
    }

    public int getTotal() {
        return total;
    }

    public String getUrl() {
        return url;
    }

    enum TileDownloadState {
        NULL, DOWNLOADING, DONE, FAILED, ABORTED;

        public boolean isFinished() {
            return (this.equals(DONE) || this.equals(ABORTED));
        }
    }

}
