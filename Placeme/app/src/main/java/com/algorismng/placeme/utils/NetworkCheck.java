package com.algorismng.placeme.utils;

/**
 * Created by George Chuks on 29/11/2016 at 13:57
 * This class contains methods which perform checks
 * on the state of the devices network and data connection
 */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;

public class NetworkCheck {
    private Context _context; private boolean connectionStatus=false;

    public NetworkCheck(Context context){
        this._context = context;
    }

    public boolean isConnected() {
        ConnectivityManager connectivity = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED)
                    {
                        return true;
                    }
        }
        return false;
    }

    //The method above doesn't actually check for a successful connection to an address.....

        /*
        new CheckConnection().execute();
        return connectionStatus; */
    //}
    //THE INTERNET STATUS IS TESTED BY CONNECTING TO GOOGLE ON A BACKGROUND THREAD
    private class CheckConnection extends AsyncTask<String, Integer, Boolean>{

        @Override
        protected Boolean doInBackground(String... args){
            HttpGet httpGet = new HttpGet("http://www.google.com");
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

            DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
            try {
                Log.d("Internet", "Checking network connection...");
                httpClient.execute(httpGet);
                Log.d("Internet", "Connection OK");
                return true;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("Internet", "Connection unavailable");
            return false;
        }

        protected void onPostExecute(Boolean status){
            if (status)
                connectionStatus=true;
        }
    }
}