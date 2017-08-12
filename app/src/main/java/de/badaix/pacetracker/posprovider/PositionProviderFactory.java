package de.badaix.pacetracker.posprovider;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class PositionProviderFactory {
    private static PositionProvider provider;

    private static PositionProvider getProvider(Context context, String name, boolean offline) {
        if (name.equals("GPS") || name.equals(GpsPositionProvider.class.getSimpleName())) {
            if ((provider != null)
                    && provider.getClass().getSimpleName().equals(GpsPositionProvider.class.getSimpleName())
                    && (provider.isOffline() == offline))
                return provider;
            if (provider != null)
                provider.stop(null);
            return GpsPositionProvider.getInstance(context, offline);
        } else if (name.equals("Manual") || name.equals(ManualPositionProvider.class.getSimpleName())) {
            if ((provider != null)
                    && provider.getClass().getSimpleName().equals(ManualPositionProvider.class.getSimpleName())
                    && (provider.isOffline() == offline))
                return provider;
            if (provider != null)
                provider.stop(null);
            return new ManualPositionProvider(context, offline);
        } else if (name.equals("Fake") || name.equals(FakePositionProvider.class.getSimpleName())) {
            if ((provider != null)
                    && provider.getClass().getSimpleName().equals(FakePositionProvider.class.getSimpleName())
                    && (provider.isOffline() == offline))
                return provider;
            if (provider != null)
                provider.stop(null);
            return new FakePositionProvider(context, offline);
        } else if (name.equals("FakeGps") || name.equals(FakeGpsPositionProvider.class.getSimpleName())) {
            if ((provider != null)
                    && provider.getClass().getSimpleName().equals(FakeGpsPositionProvider.class.getSimpleName())
                    && (provider.isOffline() == offline))
                return provider;
            if (provider != null)
                provider.stop(null);
            return new FakeGpsPositionProvider(context, offline);
        } else
            return null;
    }

    public static void stop() {
        if (provider != null)
            provider.stop(null);
    }

    public static PositionProvider getOfflinePosProvider(Context context, String name) {
        provider = getProvider(context, name, true);
        return provider;
    }

    public static PositionProvider getPosProvider(Context context, String name) {
        provider = getProvider(context, name, false);
        return provider;
    }

    public static PositionProvider getOfflinePosProvider(Context context, Class<? extends PositionProvider> name) {
        provider = getProvider(context, name.getSimpleName(), true);
        return provider;
    }

    public static PositionProvider getPosProvider(Context context, Class<? extends PositionProvider> name) {
        provider = getProvider(context, name.getSimpleName(), false);
        return provider;
    }

    public static PositionProvider getOfflinePosProvider(Context context, JSONObject json) {
        try {
            provider = getProvider(context, json.getString("type"), true);
            provider.putSettings(json.getJSONObject("settings"));
        } catch (JSONException e) {
            provider = getPosProvider(context, GpsPositionProvider.class);
            e.printStackTrace();
        }
        return provider;
    }

    public static PositionProvider getPosProvider(Context context, JSONObject json) {
        try {
            provider = getProvider(context, json.getString("type"), false);
            provider.putSettings(json.getJSONObject("settings"));
        } catch (JSONException e) {
            provider = getPosProvider(context, GpsPositionProvider.class);
            e.printStackTrace();
        }
        return provider;
    }

    public static JSONObject toJson(PositionProvider posProvider) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", posProvider.getName());
            json.put("settings", posProvider.getSettings());
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

}
