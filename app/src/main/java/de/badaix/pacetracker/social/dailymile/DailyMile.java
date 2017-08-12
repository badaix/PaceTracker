package de.badaix.pacetracker.social.dailymile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.HttpDownloader;

public class DailyMile {
    public static final int DM_AUTH_REQUEST_CODE = 1234;
    private static User userMe = null;
    private Context context;

    public DailyMile(Context context) {
        this.context = context;
    }

    public static String getToken() {
        return GlobalSettings.getInstance().getString("DailyMileToken", "");
    }

    public static boolean hasAccount() {
        return (!TextUtils.isEmpty(getToken()));
    }

    public void authorize(Activity activity) throws IOException {
        if (!Helper.isOnline(activity))
            throw new IOException("Not connected");
        Intent intent = new Intent(context, DailyMileAuth.class);
        activity.startActivityForResult(intent, DM_AUTH_REQUEST_CODE);
    }

    public void authorize(Fragment fragment) {
        Intent intent = new Intent(fragment.getActivity(), DailyMileAuth.class);
        fragment.startActivityForResult(intent, DM_AUTH_REQUEST_CODE);
    }

    public void authorize() {
        Intent intent = new Intent(context, DailyMileAuth.class);
        context.startActivity(intent);
    }

    public boolean deleteSession(int entryId) throws Exception {
        String token = getToken();
        // TODO: throw if token.isEmpty()
        HttpClient httpClient = new DefaultHttpClient();

        HttpDelete httpDelete = new HttpDelete("https://api.dailymile.com/entries/" + entryId + ".json?oauth_token="
                + token);
        HttpResponse response = httpClient.execute(httpDelete);

        HttpDownloader.logHttpResponse(this, response);
        return true;
        /*
		 * JSONObject jResponse = new JSONObject(responseEntity); return
		 * jResponse.getInt("id");
		 */
    }

    public void updateGpxForEntry(int entryId, String gpx) throws Exception {
        String token = getToken();
        // TODO: throw if token.isEmpty()
        HttpClient httpClient = new DefaultHttpClient();

        HttpPut httpPut = new HttpPut("https://api.dailymile.com/entries/" + entryId + "/track.json?oauth_token="
                + token);
        httpPut.setHeader("content-type", "application/gpx+xml");
        httpPut.setEntity(new StringEntity(gpx));
        HttpResponse response = httpClient.execute(httpPut);
        HttpDownloader.logHttpResponse(this, response);
    }

    public String postSession(PostEntry entry) throws JSONException, IOException {
        HttpResponse response = doAuthenticatedPost("https://api.dailymile.com/entries.json", entry.toJson().toString());

        return HttpDownloader.getResponse(response);
    }

    public String addNoteWithImage(String note, File imageFile, android.location.Location location) throws IOException {
        HttpPost request = new HttpPost("https://api.dailymile.com/entries.json");
        HttpResponse response = null;

        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("media[data]", new FileBody(imageFile, "image/jpeg"));
        mpEntity.addPart("media[type]", new StringBody("image"));
        mpEntity.addPart("message", new StringBody(note));
        if (location != null) {
            mpEntity.addPart("lat", new StringBody(String.valueOf(location.getLatitude())));
            mpEntity.addPart("lon", new StringBody(String.valueOf(location.getLongitude())));
        }
        mpEntity.addPart("[share_on_services][facebook]", new StringBody("false"));
        mpEntity.addPart("[share_on_services][twitter]", new StringBody("false"));
        mpEntity.addPart("oauth_token", new StringBody(getToken()));

        request.setEntity(mpEntity);
        // send the request
        HttpClient httpClient = new DefaultHttpClient();
        response = httpClient.execute(request);
        return HttpDownloader.getResponse(response);
    }

    public Vector<PersonEntry> getStream(String stream) throws JSONException, IOException {
        Hint.log(this, "GetStream: " + stream);
        Vector<PersonEntry> result = new Vector<PersonEntry>();
        HttpGet httpGet = new HttpGet(stream);
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);

        String responseEntity = HttpDownloader.getResponse(response);
        JSONObject jResponse = new JSONObject(responseEntity);
        JSONArray jArray = jResponse.getJSONArray("entries");
        for (int i = 0; i < jArray.length(); ++i) {
            PersonEntry entry = new PersonEntry(jArray.getJSONObject(i));
            result.add(entry);
        }

        return result;
    }

    public User getMe() {
        if (userMe != null)
            return userMe;
        if (TextUtils.isEmpty(DailyMile.getToken()))
            return null;

        String token = getToken();
        HttpGet httpGet = new HttpGet("https://api.dailymile.com/people/me.json?oauth_token=" + token);
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = httpClient.execute(httpGet);

            String responseEntity = HttpDownloader.getResponse(response);
            JSONObject jResponse = new JSONObject(responseEntity);
            userMe = new User(jResponse);
        } catch (Exception e) {
            String json = GlobalSettings.getInstance().getString("userMe", "");
            if (TextUtils.isEmpty(json))
                userMe = null;
            else
                try {
                    userMe = new User(new JSONObject(json));
                } catch (JSONException e1) {
                    userMe = null;
                }
        }
        try {
            if (userMe != null)
                GlobalSettings.getInstance().put("userMe", userMe.toJson().toString());
        } catch (Exception e) {
            GlobalSettings.getInstance().put("userMe", "");
        }
        return userMe;
    }

    public User getUser(String username) throws IOException, JSONException {
        HttpGet httpGet = new HttpGet("http://api.dailymile.com/people/" + username + ".json");
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);

        String responseEntity = HttpDownloader.getResponse(response);
        JSONObject jResponse = new JSONObject(responseEntity);
        return new User(jResponse);
    }

    public Vector<User> getFriends(String username) throws IOException, JSONException {
        HttpGet httpGet = new HttpGet("http://api.dailymile.com/people/" + username + "/friends.json");
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);

        String responseEntity = HttpDownloader.getResponse(response);
        JSONObject jResponse = new JSONObject(responseEntity);
        JSONArray jArray = jResponse.getJSONArray("friends");
        Vector<User> friends = new Vector<User>();
        for (int i = 0; i < jArray.length(); ++i) {
            User user = new User(jArray.getJSONObject(i));
            friends.add(user);
        }

        return friends;
    }

    public Vector<Route> getRoutes() throws IOException, JSONException {
        String token = getToken();
        if (TextUtils.isEmpty(token))
            throw new HttpResponseException(401, "not authorized");

        HttpGet httpGet = new HttpGet("https://api.dailymile.com/people/me/routes.json?oauth_token=" + token);
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);

        String responseEntity = HttpDownloader.getResponse(response);
        JSONObject jResponse = new JSONObject(responseEntity);
        JSONArray jArray = jResponse.getJSONArray("routes");
        Vector<Route> routes = new Vector<Route>();
        for (int i = 0; i < jArray.length(); ++i) {
            Route route = new Route(jArray.getJSONObject(i));
            routes.add(route);
        }

        return routes;
    }

    public void like(int entryId) throws IOException, JSONException {
        // JSONObject like = new JSONObject();
        Like like = new Like(new Date(), null);
        // try {
        // like.put("type", "like");
        // } catch (JSONException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        HttpResponse response = doAuthenticatedPost("https://api.dailymile.com/entries/" + entryId + "/likes.json",
                like.toJson().toString());
        HttpDownloader.logHttpResponse(this, response);
    }

    public Comment comment(int entryId, String comment) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        try {
            body.put("body", comment);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        HttpResponse response = doAuthenticatedPost("https://api.dailymile.com/entries/" + entryId + "/comments.json",
                body.toString());

        return new Comment(new JSONObject(HttpDownloader.getResponse(response)));
    }

    public Vector<PersonEntry> getMeAndFriends(int page) throws JSONException, IOException {
        String token = getToken();
        if (TextUtils.isEmpty(token))
            throw new HttpResponseException(401, "not authorized");
        return getStream("https://api.dailymile.com/entries/friends.json?oauth_token=" + token + "&page=" + page);
    }

    public PersonEntry getEntry(int id) throws IOException, JSONException {
        HttpGet httpGet = new HttpGet("http://api.dailymile.com/entries/" + id + ".json");
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);

        String responseEntity = HttpDownloader.getResponse(response);
        JSONObject jResponse = new JSONObject(responseEntity);
        return new PersonEntry(jResponse);
    }

    public Vector<PersonEntry> getMeAndFriends(Date since) throws JSONException, IOException {
        String token = getToken();
        return getStream("https://api.dailymile.com/entries/friends.json?oauth_token=" + token + "&since="
                + since.getTime() / 1000);
    }

    public Vector<PersonEntry> getPopular(int page) throws JSONException, IOException {
        return getStream("http://api.dailymile.com/entries/popular.json?page=" + page);
    }

    public Vector<PersonEntry> getPopular(Date since) throws JSONException, IOException {
        return getStream("http://api.dailymile.com/entries/popular.json?since=" + since.getTime() / 1000);
    }

    public Vector<PersonEntry> getPerson(String username, int page) throws JSONException, IOException {
        return getStream("http://api.dailymile.com/people/" + username + "/entries.json?page=" + page);
    }

    public Vector<PersonEntry> getPerson(String username, Date since) throws JSONException, IOException {
        return getStream("http://api.dailymile.com/people/" + username + "/entries.json?since=" + since.getTime()
                / 1000);
    }

    public Vector<PersonEntry> getNearby(android.location.Location location, int page) throws JSONException,
            IOException {
        return getStream("http://api.dailymile.com/entries/nearby/" + location.getLatitude() + ","
                + location.getLongitude() + ".json?page=" + page);
    }

    public Vector<PersonEntry> getNearby(android.location.Location location, Date since) throws JSONException,
            IOException {
        return getStream("http://api.dailymile.com/entries/nearby/" + location.getLatitude() + ","
                + location.getLongitude() + ".json?since=" + since.getTime() / 1000);
    }

    private HttpResponse doAuthenticatedPost(String url, String body) throws IOException {
        String token = getToken();
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url + "?oauth_token=" + token);
        // if (!TextUtils.isEmpty(body)) {

        HttpEntity entity = null;
        if (!TextUtils.isEmpty(body))
            entity = new StringEntity(body, HTTP.UTF_8);
        httpPost.setEntity(entity);
        // }
        // set the content type to json
        httpPost.setHeader("content-type", "application/json; charset=utf-8");// ;
        // charset=utf-8");
        return httpClient.execute(httpPost);
    }

}

/*
 * { "id":15073644, "url":"http://www.dailymile.com/entries/15073644",
 * "at":"2012-05-14T08:44:39Z", "comments":[], "likes":[], "location": { "name":
 * "Aachen, DE" }, "user": { "username":"JohannesP",
 * "display_name":"Johannes P.",
 * "photo_url":"http://www.dailymile.com/images/defaults/user_avatar.jpg",
 * "url":"http://www.dailymile.com/people/JohannesP" }, "workout": {
 * "activity_type":"Cycling", "distance": { "value":3.4744, "units":"kilometers"
 * }, "duration":517, "calories":125 } }
 * 
 * 
 * 
 * 
 * 
 * { "entries": [ { "id":15961597,
 * "url":"http://www.dailymile.com/entries/15961597",
 * "at":"2012-06-20T17:31:26Z", "message":"Lousberg Lauf", "comments":[],
 * "likes":[], "location": { "name":"Aachen, DE" }, "user": {
 * "username":"JohannesP", "display_name":"Johannes P.",
 * "photo_url":"http://s3.dmimg.com/pictures/users/321959/1337338226_avatar.jpg"
 * , "url":"http://www.dailymile.com/people/JohannesP" }, "workout": {
 * "activity_type":"Running", "distance": { "value":5.61, "units":"kilometers"
 * }, "duration":1592 } } ] }
 */

