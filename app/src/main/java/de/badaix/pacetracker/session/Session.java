package de.badaix.pacetracker.session;

import android.content.Context;
import android.location.Location;
import android.util.Pair;

import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import de.badaix.pacetracker.PositionListener;
import de.badaix.pacetracker.R;
import de.badaix.pacetracker.posprovider.PositionProvider;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorManager;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorListener;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.SessionSettings;
import de.badaix.pacetracker.util.BoundingBox;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.DoubleBuffer;
import de.badaix.pacetracker.util.Hint;

public abstract class Session extends SessionSummary implements PositionListener, SensorListener {

    public static final int SESSION_COMMAND_START = 0;
    public static final int SESSION_COMMAND_STOP = 1;
    public static final int SESSION_COMMAND_PAUSE = 2;
    public static final int SESSION_COMMAND_RESUME = 3;
    public static final int SESSION_COMMAND_DISCARD = 4;
    public static final int SESSION_COMMAND_CLOSE = 5;

    public static final double MAX_ACCELERATION = 0.02;
    public static final double RESUME_THRESHOLD = 3.0;

    protected Vector<GpsPos> vGpsPos;
    protected Vector<HxmData> vHxmData;
    protected Vector<Pair<Double, Double>> vMET;
    protected GeoPos pausePos = null;

    protected int gpsCount = 0;
    protected boolean resumed = false;
    protected Date pauseBegin;
    protected SessionListener listener = null;
    protected BoundingBox boundingBox = null;
    protected DoubleBuffer speedBuffer = null;
    State state;
    private boolean lastActive = false;
    private boolean lastHasFix = false;
    private int lastFixCount = 0;
    private int lastSatCount = -1;
    private SensorProvider sensorProvider;
    private PositionProvider posProvider;
    private long sessionStartMillis = -1;
    private long hxmSum = 0;
    private long hxmCount = 0;

    public Session(SessionListener listener, SessionSettings settings) {
        super(settings);
        this.listener = listener;
        this.settings = settings;
        initAll();
        if (listener == null)
            setState(State.OFFLINE);
    }

    protected void initMET() {
        vMET = new Vector<Pair<Double, Double>>();
    }

    protected void initAll() {
        initMET();
        posProvider = (settings == null) ? null : settings.getPositionProvider();
        sensorProvider = ((settings == null) || !settings.isOnline()) ? null : SensorManager.getSensorProvider(settings
                .getSensor());
        if (posProvider != null) {
            try {
                posProvider.start(this);
            } catch (Exception e) {
            }
        }
        vGpsPos = new Vector<GpsPos>();
        vHxmData = new Vector<HxmData>();
        boundingBox = new BoundingBox();
        // vSessionEvent = new Vector<SessionEvent>();
        gpsCount = 0;
        hxmSum = 0;
        hxmCount = 0;
        // maxSpeed = 0;
        totalPause = 0;
        sessionStartMillis = -1;
        resumed = false;
        pauseBegin = null;
        pausePos = null;
        speedBuffer = new DoubleBuffer(5);
        state = State.INIT;
        setState(State.INIT);
    }

    public void updateBoundingBox() {
        this.boundingBox = new BoundingBox(getGpsPos());
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public State getState() {
        return state;
    }

    @Override
    public void setSessionStart(Date sessionStart) {
        super.setSessionStart(sessionStart);
        sessionStartMillis = System.currentTimeMillis();
    }

    protected State setState(State newState) {
        switch (newState) {
            case AUTOPAUSED: {
                if (state == State.RUNNING) {
                    pauseBegin = new Date();
                    // newEvent(SESSION_EVENT_AUTOPAUSED);
                }
                break;
            }

            case INIT:
                break;

            case PAUSED: {
                if (state == State.RUNNING) {
                    pauseBegin = new Date();
                    // newEvent(SESSION_EVENT_PAUSED);
                } else if (state == State.AUTOPAUSED) {
                    // newEvent(SESSION_EVENT_PAUSED);
                }
                break;
            }

            case RUNNING: {
                if (state == State.STOPPED) {
                    newState = State.STOPPED;
                } else {
                    if (state == State.INIT) {
                        setSessionStart(new Date());
                        // newEvent(SESSION_EVENT_STARTED);
                    } else if (state == State.WAITSTART) {
                        // newEvent(SESSION_EVENT_STARTED);
                    } else if ((state == State.AUTOPAUSED) || (state == State.PAUSED)) {
                        resumed = true;
                        // newEvent(SESSION_EVENT_RESUMED);
                    }

                    if (pauseBegin != null)
                        totalPause += DateUtils.getTimeSince(pauseBegin);

                    pauseBegin = null;
                }
                break;
            }

            case STOPPED: {
                if ((state != State.STOPPED) && (getSessionStop() == null)) {
                    if (pauseBegin != null)
                        totalPause += DateUtils.getTimeSince(pauseBegin);
                    pauseBegin = null;
                    setSessionStop(new Date());
                    setDuration(getDuration());
                    // newEvent(SESSION_EVENT_STOPPED);
                }
                break;
            }

            case WAITSTART: {
                if (state != State.WAITSTART) {
                    setSessionStart(new Date());
                    pauseBegin = new Date();
                    // newEvent(SESSION_EVENT_WAITING_FOR_START);
                }
                break;
            }
        }

        Hint.log(this, "OldState: " + state + "  NewState: " + newState);

        State oldState = state;
        this.state = newState;

        if ((listener != null) && (newState != oldState))
            listener.onStateChanged(oldState, newState);

        return this.state;
    }

    @Override
    public String getName(Context context) {
        return getType().replace("Session", "");
    }

    @Override
    public String getVerb(Context context) {
        return getName(context).toLowerCase(Locale.getDefault());
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public int getDrawable() {
        return R.drawable.icon;
    }

    @Override
    public int getLightDrawable() {
        return getDrawable();
    }

    public SensorProvider getSensorProvider() {
        return sensorProvider;
    }

    public void pause() {
        pause(false);
    }

    // protected void newEvent(int event) {
    // newEvent(event, "");
    // }

    // public void newEvent(int event, String description) {
    // if (listener != null)
    // listener.onSessionEvent(event, description);
    // vSessionEvent.add(new SessionEvent(event, vGpsPos.size() - 1,
    // description));
    // }

    protected void pause(boolean autoPause) {
        if (!isStarted())
            return;

        if ((listener != null) && !autoPause)
            listener.onSessionCommand(SESSION_COMMAND_PAUSE);

        if (autoPause)
            setState(State.AUTOPAUSED);
        else
            setState(State.PAUSED);
    }

    public void resume() {
        if (!isStarted())
            return;

        setState(State.RUNNING);

        if (listener != null)
            listener.onSessionCommand(SESSION_COMMAND_RESUME);
    }

    public long getTotalDuration() {
        if (state == State.INIT)
            return 0;

        if (getSessionStart() == null)
            return 0;

        if ((state == State.STOPPED) || (state == State.OFFLINE))
            return (getSessionStop().getTime() - getSessionStart().getTime());

        return System.currentTimeMillis() - sessionStartMillis;
    }

    @Override
    public long getDuration() {
        if ((state == State.OFFLINE) && super.getDuration() > 0)
            return super.getDuration();

        long activePause = 0;
        if (pauseBegin != null)
            activePause = DateUtils.getTimeSince(pauseBegin);

        long duration = getTotalDuration() - totalPause - activePause;
        // Hint.log(this, "Duration: " + duration);
        return duration;
    }

    public double getMET() {
        double met = 0;
        double speed = getAvgSpeed();
        if (vMET.isEmpty())
            return 0;
        if (vMET.size() == 1)
            met = vMET.get(0).second;
        else if (vMET.firstElement().first >= speed)
            met = vMET.firstElement().second;
        else if (vMET.lastElement().first <= speed)
            met = vMET.lastElement().second;
        else {
            for (int i = 1; i < vMET.size(); ++i) {
                Pair<Double, Double> firstPair = vMET.get(i - 1);
                Pair<Double, Double> secondPair = vMET.get(i);
                if ((firstPair.first <= speed) && (secondPair.first >= speed)) {
                    double factor = (speed - firstPair.first) / (secondPair.first - firstPair.first);
                    met = firstPair.second + (secondPair.second - firstPair.second) * factor;
                }
            }
        }
        return met;
    }

    protected int getCaloriesByMET() {
        return (int) (getMET() * GlobalSettings.getInstance().getUserWeight() * ((double) getDuration() / 1000.0 / 3600.0));
    }

    protected abstract int getCaloriesInternal();

    public float getCurrentSpeed() {
        return (float) speedBuffer.getAverage() * 3.6f;
    }

    public boolean hasGpsInfo() {
        return (!vGpsPos.isEmpty());
    }

    public boolean hasHxmInfo() {
        return (!vHxmData.isEmpty());
    }

    public boolean isStarted() {
        return ((state == State.RUNNING) || (state == State.PAUSED) || (state == State.AUTOPAUSED));
        // (state == State.WAITSTART));
    }

    public boolean isStopped() {
        return ((state == State.STOPPED) || (state == State.OFFLINE));
    }

    public void stopSession() {
        setState(State.STOPPED);
        SensorManager.stopSensor();
        if (posProvider != null)
            posProvider.stop(null);

        if (listener != null)
            listener.onSessionCommand(SESSION_COMMAND_STOP);

    }

    public void closeSession(boolean discard) {
        SensorManager.stopSensor();
        if (listener != null) {
            listener.onSessionCommand(SESSION_COMMAND_CLOSE);
            if (discard)
                listener.onSessionCommand(SESSION_COMMAND_DISCARD);
        }

        setState(State.STOPPED);
    }

    public void startSession() {
        try {
            if (settings.getPulseSettings().isSensorEnabled())
                sensorProvider.start(this);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (listener != null)
            listener.onSessionCommand(SESSION_COMMAND_START);

        if (!settings.isAutoStart())
            setState(State.RUNNING);
        else
            setState(State.WAITSTART);
    }

    @Override
    public int getCalories() {
        setCalories(getCaloriesInternal());
        return super.getCalories();
    }

    protected void addGpsPos(Location location, double addDistance, float speed) {
        if (state == State.WAITSTART)
            setState(State.RUNNING);

        setCalories(getCaloriesInternal());

        location.setSpeed(speed);
        double distance = getDistance() + addDistance;
        GpsPos newPos = new GpsPos(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                location.getTime(), location.getSpeed(), location.getBearing(), getDuration(), distance);
        vGpsPos.add(newPos);
        boundingBox.add(newPos);
        setDistance(distance);
        if (listener != null)
            listener.onFilteredLocationChanged(newPos);
    }

    protected boolean checkForAutoResume(Location location) {
        if (!state.isAutoPaused())
            return false;

        if (pausePos == null)
            pausePos = new GeoPos(location.getLatitude(), location.getLongitude());

        double distSincePause = Distance.calculateDistance(pausePos,
                new GeoPos(location.getLatitude(), location.getLongitude()));
        Hint.log(this, "distance since pause: " + distSincePause);

        if (distSincePause >= RESUME_THRESHOLD) {
            setState(State.RUNNING);
            return true;
        }

        return false;
    }

    public boolean newGpsPos(Location location) {
        if (state == State.STOPPED)
            return false;

        checkForAutoResume(location);

        if (state != State.RUNNING)
            return false;

        if (vGpsPos.isEmpty()) {
            addGpsPos(location, 0, location.getSpeed());
            setStartPos(new GeoPos(location.getLatitude(), location.getLongitude()));
            return true;
        }

        if (location.getSpeed() == 0.0f) {
            if (settings.isAutoPause()) {
                speedBuffer.reset();
                pause(true);
            }
            return false;
        }

        gpsCount++;
        double dTmpDist = Distance.calculateDistance(vGpsPos.lastElement().latitude, vGpsPos.lastElement().longitude,
                location.getLatitude(), location.getLongitude(), Distance.METERS);
        if (!Double.isNaN(dTmpDist) && !Double.isInfinite(dTmpDist) && (dTmpDist < 100000.)) {
            float currSpeed = (float) (dTmpDist / (location.getTime() - vGpsPos.lastElement().time) * 1000.f);
            boolean validSpeed = isValidSpeed(location.getTime(), currSpeed, vGpsPos.lastElement().time,
                    vGpsPos.lastElement().speed);

            if (validSpeed) {
                speedBuffer.setNext(currSpeed);
                setMaxSpeed(Math.max(getMaxSpeed(), getCurrentSpeed()));

                if (resumed) {
                    speedBuffer.reset();
                    addGpsPos(location, 0, currSpeed);
                    resumed = false;
                    return true;
                } else if ((gpsCount >= 2) || (dTmpDist >= 5.)) {
                    addGpsPos(location, dTmpDist, currSpeed);
                    gpsCount = 0;
                    return true;
                }
            } else if (gpsCount > 10) {
                addGpsPos(location, dTmpDist, currSpeed);
                gpsCount = 0;
                return true;
            }
        }

        return false;
    }

    public Vector<GpsPos> getGpsPos() {
        return vGpsPos;
    }

    public Vector<HxmData> getHxmData() {
        return vHxmData;
    }

    @Override
    public void onLocationChanged(Location location) {
        newGpsPos(location);
        if (listener != null)
            listener.onLocationChanged(location);
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {

        if (!hasFix && lastHasFix && settings.isAutoPause() && state.equals(State.RUNNING))
            setState(State.AUTOPAUSED);

        if ((lastSatCount == -1) || (lastSatCount != satCount) || (lastActive != active) || (lastHasFix != hasFix)
                || (lastFixCount != fixCount)) {
            lastSatCount = satCount;
            lastActive = active;
            lastHasFix = hasFix;
            lastFixCount = fixCount;

            if (listener != null)
                listener.onGpsStatusChanged(active, hasFix, fixCount, satCount);
        }
    }

    @Override
    public float getHrMean() {
        if (state != State.OFFLINE)
            setHrMean(hxmSum / (float) hxmCount);
        return super.getHrMean();
    }

    private void newHxmData(SensorData newData) {
        if (!newData.hasHeartRate() && !newData.hasCadence())
            return;

        if (newData.getHeartRate() == 0)
            return;

        hxmSum += newData.getHeartRate();
        ++hxmCount;
        if (vHxmData.isEmpty()) {
            setHrMax(newData.getHeartRate());
            setHrMin(newData.getHeartRate());
        }
        setHrMax((short) Math.max(newData.getHeartRate(), getHrMax()));
        setHrMin((short) Math.min(newData.getHeartRate(), getHrMin()));

        HxmData data = null;
        if (vHxmData.isEmpty()) {
            data = new HxmData(newData, getDuration(), getDistance());
            vHxmData.add(data);
            if (listener != null)
                listener.onSensorDataChanged(data);
            return;
        }

        HxmData last = vHxmData.lastElement();
        if ((last.heartRate != newData.getHeartRate()) || (last.cadence != newData.getCadence())) {
            if (newData.getCreationTime().getTime() - last.time > 1000) {
                data = new HxmData(newData, getDuration(), getDistance());
                vHxmData.add(data);
                if (listener != null)
                    listener.onSensorDataChanged(data);
            }
        }
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        newHxmData(sensorData);
        if (listener != null)
            listener.onSensorData(provider, sensorData);
    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
        if (listener != null)
            listener.onSensorStateChanged(provider, active, sensorState);
    }

    /**
     * Returns true if the speed is valid.
     *
     * @param time              the time
     * @param speed             the speed
     * @param lastLocationTime  the last location time
     * @param lastLocationSpeed the last location speed
     */
    private boolean isValidSpeed(long time, double speed, long lastLocationTime, double lastLocationSpeed) {

		/*
         * There are a lot of noisy speed readings. Do the cheapest checks
		 * first, most expensive last.
		 */
        if (speed == 0) {
            return false;
        }

		/*
		 * The following code will ignore unlikely readings. 128 m/s seems to be
		 * an internal android error code.
		 */
        if (Math.abs(speed - 128) < 1) {
            return false;
        }

		/*
		 * See if the speed seems physically likely. Ignore any speeds that
		 * imply acceleration greater than 2g.
		 */
        long timeDifference = time - lastLocationTime;
        double speedDifference = Math.abs(lastLocationSpeed - speed);
        if (speedDifference > MAX_ACCELERATION * timeDifference) {
            return false;
        }

		/*
		 * Only check if the speed buffer is full. Check that the speed is less
		 * than 10X the smoothed average and the speed difference doesn't imply
		 * 2g acceleration.
		 */
        if (!speedBuffer.isFull()) {
            return true;
        }
        double average = speedBuffer.getAverage();
        double diff = Math.abs(average - speed);
        return (speed < average * 10) && (diff < MAX_ACCELERATION * timeDifference);
    }

    public enum State {
        INIT(0), WAITSTART(1), RUNNING(2), STOPPED(3), PAUSED(4), AUTOPAUSED(5), OFFLINE(6);

        private int intValue;

        private State(int intValue) {
            this.intValue = intValue;
        }

        static public State fromInt(int intValue) {
            if (INIT.intValue == intValue)
                return INIT;
            else if (WAITSTART.intValue == intValue)
                return WAITSTART;
            else if (RUNNING.intValue == intValue)
                return RUNNING;
            else if (STOPPED.intValue == intValue)
                return STOPPED;
            else if (PAUSED.intValue == intValue)
                return PAUSED;
            else if (AUTOPAUSED.intValue == intValue)
                return AUTOPAUSED;
            else if (OFFLINE.intValue == intValue)
                return OFFLINE;
            else
                throw new IllegalArgumentException("unknown state for " + intValue);
        }

        public boolean isAutoPaused() {
            return (this.equals(WAITSTART) || this.equals(AUTOPAUSED));
        }

        public String toString(Context context) {
            if (this.equals(INIT))
                return "Init";
            else if (this.equals(WAITSTART))
                return "Waiting for start";
            else if (this.equals(RUNNING))
                return "Running";
            else if (this.equals(STOPPED))
                return "Stopped";
            else if (this.equals(PAUSED))
                return "Paused";
            else if (this.equals(AUTOPAUSED))
                return "Auto-paused";
            else if (this.equals(OFFLINE))
                return "Offline";
            else
                return "";
        }

        public String toActionString(Context context) {
            if (this.equals(WAITSTART))
                return "Waiting for start";
            else if (this.equals(STOPPED))
                return "Stopped";
            else if (this.equals(PAUSED))
                return "Paused";
            else if (this.equals(AUTOPAUSED))
                return "Auto-paused";
            else if (this.equals(OFFLINE))
                return "Offline";
            else
                return "";
        }

        public int asInt() {
            return intValue;
        }
    }
}
