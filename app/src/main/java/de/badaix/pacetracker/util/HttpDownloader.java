package de.badaix.pacetracker.util;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

public class HttpDownloader {

    public static String getResponse(HttpResponse response) throws IOException {
        String result = "";
        result = convertStreamToString(response.getEntity().getContent());
        int status = response.getStatusLine().getStatusCode();
        if (status / 100 != 2)
            throw new HttpResponseException(response.getStatusLine().getStatusCode(), response.getStatusLine()
                    .getReasonPhrase());
        return result;
    }

    public static String convertStreamToString(java.io.InputStream is) {
        try {
            return new java.util.Scanner(is).useDelimiter("\\A").next();
        } catch (java.util.NoSuchElementException e) {
            return "";
        }
    }

    public static void logHttpResponse(Object who, HttpResponse response) {
        String responseEntity;
        try {
            responseEntity = convertStreamToString(response.getEntity().getContent());
            Hint.log(who, "response: " + response.getStatusLine().toString());
            Hint.log(who, "Entity: " + responseEntity);
            HeaderIterator iterator = response.headerIterator();
            while (iterator.hasNext()) {
                Header header = iterator.nextHeader();
                Hint.log(who, "header name: " + header.getName());
                Hint.log(who, "header value: " + header.getValue());
            }
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public InputStream getStream(URL url) throws IOException {
        try {
            HttpGet httpRequest = new HttpGet(url.toURI());
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
            Hint.log(this, "Status: " + response.getStatusLine().getStatusCode() + ", url: " + url.toString());
            if (response.getStatusLine().getStatusCode() / 100 != 2)
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), response.getStatusLine()
                        .getReasonPhrase());
            HttpEntity entity = response.getEntity();
            BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
            InputStream instream = bufHttpEntity.getContent();
            return instream;
            // HttpURLConnection connection = (HttpURLConnection)
            // url.openConnection();
            // return new BufferedInputStream(connection.getInputStream());
        } catch (FileNotFoundException e) {
            throw new HttpResponseException(404, e.getMessage());
        } catch (URISyntaxException e) {
            Hint.log(this, e);
            return null;
        }
    }

    public byte[] getBytes(URL url) throws IOException {
        InputStream input = getStream(url);
        if (input == null)
            return null;

        byte data[] = null;
        ByteArrayOutputStream bos = null;
        try {
            // download the file
            bos = new ByteArrayOutputStream();
            data = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                bos.write(data, 0, count);
            }
            return bos.toByteArray();
        } finally {
            input.close();
            if (bos != null)
                bos.close();
        }
    }

}
