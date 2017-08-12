package de.badaix.pacetracker.social.dailymile;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import net.smartam.leeloo.client.OAuthClient;
import net.smartam.leeloo.client.URLConnectionClient;
import net.smartam.leeloo.client.request.OAuthClientRequest;
import net.smartam.leeloo.client.response.OAuthJSONAccessTokenResponse;
import net.smartam.leeloo.common.exception.OAuthProblemException;
import net.smartam.leeloo.common.exception.OAuthSystemException;
import net.smartam.leeloo.common.message.types.GrantType;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Hint;

interface WebBrowserListener {
    boolean onUrlRequest(String url);
}

public class DailyMileAuth extends AppCompatActivity implements WebBrowserListener {
    private final String REDIRECT_URI = "pacetracker://dailymileoauthresponse";
    private WebView webView;
    private boolean isSigningUp = false;
    private HelloWebViewClient webViewClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dailymile_auth);
        webView = (WebView) findViewById(R.id.webView);
        webViewClient = new HelloWebViewClient(this);
        webView.setWebViewClient(webViewClient);
        webView.clearCache(true);
        getSupportActionBar().setTitle("dailymile login");
//        getSupportActionBar().setLogo(R.drawable.dashboard_button_dailymile);
//        getSupportActionBar().setDisplayUseLogoEnabled(true);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
        try {
            getAccessCode("https://api.dailymile.com/oauth/authorize", GlobalSettings.getInstance().getMetaData("dailymile.Client_ID"));
        } catch (OAuthSystemException e) {
            Hint.show(this, getString(R.string.error) + ": " + e.getMessage());
        }
        Hint.log(this, "onCreate");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Uri uri = intent.getData();

        Hint.log(this, "onNewIntent");

        if (uri != null) {
            Hint.log(this, "Uri: " + uri.toString());
            if (uri.toString().startsWith(REDIRECT_URI)) {
                String code = uri.getQueryParameter("code");
                Hint.log(this, "Code: " + code);
            }
        }
    }

    private void getAccessCode(String location, String clientId) throws OAuthSystemException {
        OAuthClientRequest request = null;
        request = OAuthClientRequest.authorizationLocation(location).setClientId(clientId).setRedirectURI(REDIRECT_URI)
                .buildQueryMessage();

        Hint.log(this, "request: " + request.getLocationUri() + "&response_type=code");
        webView.loadUrl(request.getLocationUri() + "&response_type=code");
    }

    private String getAccessToken(String code) throws OAuthSystemException, OAuthProblemException {
        OAuthClientRequest request = null;
        request = OAuthClientRequest.tokenLocation("https://api.dailymile.com/oauth/token")
                .setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(GlobalSettings.getInstance().getMetaData("dailymile.Client_ID")).setClientSecret(GlobalSettings.getInstance().getMetaData("dailymile.Client_Seccret"))
                .setRedirectURI(REDIRECT_URI).setCode(code).buildBodyMessage();

        OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

        OAuthJSONAccessTokenResponse response = oAuthClient.accessToken(request);
        String token = response.getAccessToken();
        Hint.log(this, "token: " + token);
        return token;
    }

    @Override
    public boolean onUrlRequest(String url) {
        Hint.log(this, "url: " + url);
        if (url.startsWith(REDIRECT_URI)) {
            Uri uri = Uri.parse(url);
            String code = uri.getQueryParameter("code");
            Hint.log(this, "Code: " + code);
            webView.setVisibility(View.GONE);
            GetAccessTokenTask task = new GetAccessTokenTask();
            task.execute(code);
            return false;
        } else if (url.contains("signup")) {
            isSigningUp = true;
        } else if (isSigningUp && url.equals("http://api.dailymile.com")) {
            try {
                getAccessCode("https://api.dailymile.com/oauth/authorize", GlobalSettings.getInstance().getMetaData("dailymile.Client_ID"));
            } catch (OAuthSystemException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If the back Key was pressed, then finish the program.
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            setResult(RESULT_CANCELED);
            finish();
        }

        // else return the normal function of whatever key was pressed
        return super.onKeyDown(keyCode, event);
    }

    private class HelloWebViewClient extends WebViewClient {
        private WebBrowserListener listener;

        HelloWebViewClient(WebBrowserListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (listener != null) {
                if (listener.onUrlRequest(url))
                    view.loadUrl(url);
            }
            return true;
        }
    }

    private class GetAccessTokenTask extends AsyncTask<String, Void, Boolean> {
        private Exception exception = null;

        @Override
        protected Boolean doInBackground(String... code) {
            try {
                String token = getAccessToken(code[0]);
                GlobalSettings.getInstance().put("DailyMileToken", token);
                try {
                    DailyMile dm = new DailyMile(DailyMileAuth.this);
                    User user = dm.getMe();
                    if (user != null)
                        GlobalSettings.getInstance().setMe(user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                exception = e;
                return false;
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Void... progress) {
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (exception != null)
                Toast.makeText(getApplication(), "Exception: " + exception, Toast.LENGTH_LONG).show();
            if (result)
                setResult(RESULT_OK);
            else
                setResult(RESULT_CANCELED);
            finish();
        }
    }

}
