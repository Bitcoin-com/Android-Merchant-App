package com.bitcoin.merchant.app.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.bitcoin.merchant.app.util.AppUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * Implementation of AsyncTask that runs a network operation on a background thread.
 */
public abstract class DownloadTask<R> {
    private Context activity;

    private NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    public DownloadTask(Context activity) {
        this.activity = activity;
    }

    protected boolean isCancelled() {
        NetworkInfo networkInfo = getActiveNetworkInfo();
        // If no connectivity, cancel task and update Callback with null data.
        return networkInfo == null || !networkInfo.isConnected() ||
                (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                        && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE);
    }

    abstract protected void onDownloaded(R result);

    public final void execute() {
        String url = getUrl();
        if (url == null || isCancelled()) {
            try {
                onDownloaded(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        final Thread thread = new Thread(url) {
            @Override
            public void run() {
                R r = null;
                try {
                    try {
                        r = download();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } finally {
                    try {
                        onDownloaded(r);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.setName(url);
        thread.start();
    }

    protected R download() {
        R result = null;
        if (!isCancelled()) {
            try {
                URL url = new URL(getUrl());
                String resultString = downloadUrl(url);
                if (resultString != null) {
                    result = AppUtil.GSON.fromJson(resultString, getReturnClass());
                } else {
                    throw new IOException("No response received.");
                }
            } catch (Exception e) {
                result = null;
            }
        }
        return result;
    }

    protected abstract Class<R> getReturnClass();

    protected abstract String getUrl();

    /**
     * Given a URL, sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in String form. Otherwise,
     * it will throw an IOException.
     */
    private String downloadUrl(URL url) throws IOException {
        InputStream stream = null;
        HttpsURLConnection connection = null;
        String result = null;
        try {
            Log.i("DownloadTask", url.toString());
            connection = (HttpsURLConnection) url.openConnection();
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            if (stream != null) {
                // Converts Stream to String
                result = readStream(stream);
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                stream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    public static String readStream(InputStream stream) throws IOException {
        byte[] tempBuffer = new byte[65536];
        byte[] bytes = loadBytes(stream, tempBuffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] loadBytes(InputStream is, byte[] tempBuffer) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copy(os, is, tempBuffer);
        return os.toByteArray();
    }

    public static int copy(OutputStream os, InputStream is, byte[] tempBuffer) throws IOException {
        int total = 0;
        while (true) {
            int read = is.read(tempBuffer);
            if (read == -1) {
                // end of file reached
                break;
            }
            os.write(tempBuffer, 0, read);
            total += read;
        }
        return total;
    }
}
