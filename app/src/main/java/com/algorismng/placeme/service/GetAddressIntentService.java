package com.algorismng.placeme.service;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.algorismng.placeme.R;
import com.algorismng.placeme.utils.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.wearable.DataMap.TAG;
//import info.androidhive.smsverification.helper.PrefManager;


public class GetAddressIntentService extends IntentService {


    protected ResultReceiver mReceiver;

    public GetAddressIntentService() {
        super(GetAddressIntentService.class.getSimpleName());

    }

    /**
     * retrieve the street address from the geocoder, handle any errors that may occur,
     * @param intent
     * and send the results back to the activity that requested the address.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            String errorMessage = "";

            // Get the location passed to this service through an extra.
            Location location = intent.getParcelableExtra(
                    Constants.LOCATION_DATA_EXTRA);


            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        // In this sample, get just a single address.
                        1);
            } catch (IOException ioException) {
                // Catch network or other I/O problems.
                errorMessage = getString(R.string.service_not_available);
                Log.e(TAG, errorMessage, ioException);
            } catch (IllegalArgumentException illegalArgumentException) {
                // Catch invalid latitude or longitude values.
                errorMessage = getString(R.string.invalid_lat_long_used);
                Log.e(TAG, errorMessage + ". " +
                        "Latitude = " + location.getLatitude() +
                        ", Longitude = " +
                        location.getLongitude(), illegalArgumentException);
            }

            // Handle case where no address was found.
            if (addresses == null || addresses.size()  == 0) {
                if (errorMessage.isEmpty()) {
                    errorMessage = getString(R.string.no_address_found);
                    Log.e(TAG, errorMessage);
                }
                deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<String>();

                // Fetch the address lines using getAddressLine,
                // join them, and send them to the thread.
                for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
                Log.i(TAG, getString(R.string.address_found));
                deliverResultToReceiver(Constants.SUCCESS_RESULT,
                        TextUtils.join(System.getProperty("line.separator"), addressFragments));
                Toast.makeText(getApplicationContext(), "Address: "+addressFragments.toString(), Toast.LENGTH_LONG);
            }

        }
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }


}