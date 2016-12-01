package com.algorismng.placeme;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.algorismng.placeme.service.GetAddressIntentService;
import com.algorismng.placeme.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by George Chuks at 10:57 a.m 29/11/2016
 * Initialize navigation drawer
 * initializes map fragment
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    //Declare UI elements to be interacted with
    DrawerLayout drawer;
    FloatingActionButton fab;
    Toolbar toolbar;
    NavigationView navigationView;

    //Material dialog imported from git
    MaterialDialog dialog;

    GoogleMap mGoogleMap;

    private static final String TAG = MainActivity.class.getSimpleName();
    private CameraPosition mCameraPosition;

    private AccountManager manager;

    // The entry point to Google Play services, used by the Places API and Fused Location Provider.
    private GoogleApiClient mGoogleApiClient;
    // A request object to store parameters for requests to the FusedLocationProviderApi.
    private LocationRequest mLocationRequest;
    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 15000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent
    // than this value.
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // The geographical location where the device is currently located.
    private Location mCurrentLocation;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(6.5762764, 3.3467113); //default location to show when location permission hasn't been granted
    private static final int DEFAULT_ZOOM = 16;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    //
    protected Location mLastLocation;
    private AddressResultReceiver mResultReceiver;
    private boolean mAddressRequested;
    private String mAddressOutput;

    private TextView mTitle;
    BottomSheetBehavior mBottomSheetBehavior;

    TextView mNearby, mdate, mCoordinates, mAddress, mMail, mStateCountry;

    //Initialize layout
    //Call function to initialize user interface(UI) elements
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the content view which renders the map and the frames for other fragments.
        setContentView(R.layout.activity_main);

        setupLayoutElements();


        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Build the Play services client for use by the Fused Location Provider and the Places API.
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        //startIntentService();

        /*SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);*/
    }

    //initializes UI elements
    public void setupLayoutElements() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_nav);
        // Remove default title text
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setTitle(getResources().getString(R.string.title_current_location));
        getSupportActionBar().setTitle(getResources().getString(R.string.title_current_location));
        // Get access to the custom title view
        mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText(R.string.title_current_location);

        CoordinatorLayout coordinatorLayout = (CoordinatorLayout)findViewById(R.id.coordinator_layout);

        final View mLayoutBottomSheet = coordinatorLayout.findViewById(R.id.bottom_sheet);

        //get bottom sheet behavior from bottom sheet view
        mBottomSheetBehavior = BottomSheetBehavior.from(mLayoutBottomSheet);

        //to expand the bottom sheet
        //mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        //instantiate all the textviews  mNearby, mdate, mCoordinates, mAddress, mMail, mStateCountry;
        mNearby = (TextView) toolbar.findViewById(R.id.location_nearby);
        mdate = (TextView) toolbar.findViewById(R.id.date_day);
        mCoordinates = (TextView) toolbar.findViewById(R.id.geo_coordinates);
        mAddress = (TextView) toolbar.findViewById(R.id.address);
        mMail = (TextView) toolbar.findViewById(R.id.user_mail);
        mStateCountry = (TextView) toolbar.findViewById(R.id.state_country);

        //get google account on phone
        mMail.setText(getUsername());

        //get date
        Calendar c = Calendar.getInstance();

        DateFormat fromFormat = new SimpleDateFormat("dd/MM/yyyy");
        fromFormat.setLenient(false);
        DateFormat toFormat = new SimpleDateFormat("dd/MMM/yyyy");
        toFormat.setLenient(false);
        Date datt = null;
        try {
            datt = fromFormat.parse(c.get(Calendar.DAY_OF_MONTH)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.YEAR));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //TODO: implement logic to save location in an array list
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Current location has been saved", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    /**
     * This function handles back pressed events
     * It overrides a the onBackPressed function in its superclass
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        //if navigation drawer is open, close drawer
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment fragment_about = getSupportFragmentManager().findFragmentByTag(getString(R.string.frag_about));

            if (fragment_about != null) {//if fragment about is in view, remove
                getSupportFragmentManager().beginTransaction().remove(fragment_about).commit();
            }else if (fragment_about != null) {//if fragment about is in view, remove
                getSupportFragmentManager().beginTransaction().remove(fragment_about).commit();
            } else {
                //confirm user exit action
                confirmExit();
            }
        }
    }

    public void fetchAddress() {
        // Only start the service to fetch the address if GoogleApiClient is
        // connected.
        if (mGoogleApiClient.isConnected() && mLastLocation != null) {
            startIntentService();
        }
        // If GoogleApiClient isn't connected, process the user's request by
        // setting mAddressRequested to true. Later, when GoogleApiClient connects,
        // launch the service to fetch the address. As far as the user is
        // concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
    }

    /**
     * Get the device location and nearby places when the activity is restored after a pause.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            getDeviceLocation();
        }
        updateMarkers();
    }

    /**
     * Stop location updates when the activity is no longer in focus, to reduce battery consumption.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            mGoogleApiClient.disconnect();//disconnect our google api client
        }
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mGoogleMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mGoogleMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mCurrentLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Gets the device's current location and builds the map
     * when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        getDeviceLocation();
        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Refer to the reference doc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Play services connection suspended");
    }

    /**
     * Handles the callback when location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        updateMarkers();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;//instantiate the public googlemap variable

    /*    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setMinZoomPreference(4);
        mGoogleMap.setMaxZoomPreference(8);
        mGoogleMap.setTrafficEnabled(true);
        mGoogleMap.setIndoorEnabled(true);
        mGoogleMap.setBuildingsEnabled(true);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);*/

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        // Add markers for nearby places.
        updateMarkers();

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents, null);

                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });
        /*
         * Set the map's camera position to the current location of the device.
         * If the previous state was saved, set the position to the saved state.
         * If the current location is unknown, use a default position and zoom value.
         */
        if (mCameraPosition != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mCurrentLocation != null) {

            double currentLatitude = mCurrentLocation.getLatitude();
            double currentLongitude = mCurrentLocation.getLongitude();
            LatLng latLng = new LatLng(currentLatitude, currentLongitude);

            getCorrespondingLocationAddress(mCurrentLocation);

            //clear googlemap markers
            mGoogleMap.clear();
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title("I am here!");
            mGoogleMap.addMarker(options);
            mGoogleMap.setContentDescription("You are here!");

            // Instantiates a new CircleOptions object around user position and defines the center and radius
            CircleOptions circleOptions = new CircleOptions()
                    .center(latLng)
                    .radius(100)
                    .strokeWidth(5)
                    .strokeColor(Color.GREEN)
                    .fillColor(R.color.Aquamarine); // In meters

            // Get back the mutable Circle
            //Circle circle = myMap.addCircle(circleOptions);

            mGoogleMap.addCircle(circleOptions);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
           /* mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mCurrentLocation.getLatitude(),
                            mCurrentLocation.getLongitude()), DEFAULT_ZOOM));*/

            Toast.makeText(this, "Latitude: " + currentLatitude + " >> Longitude: " + currentLongitude + " >>", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

    }

    /**
     * Builds a GoogleApiClient.
     * Uses the addApi() method to request the Google Places API and the Fused Location Provider.
     */
    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        /*
         * Sets the desired interval for active location updates. This interval is
         * inexact. You may not receive updates at all if no location sources are available, or
         * you may receive them slower than requested. You may also receive updates faster than
         * requested if other applications are requesting location at a faster interval.
         */
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        /*
         * Sets the fastest rate for active location updates. This interval is exact, and your
         * application will never receive updates faster than this value.
         */
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Gets the current location of the device and starts the location update notifications.
     */
    private void getDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         * Also request regular updates about the device location.
         */
        if (mLocationPermissionGranted) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Adds markers for places nearby the device and turns the My Location feature on or off,
     * provided location permission has been granted.
     */
    private void updateMarkers() {
        if (mGoogleMap == null) {
            return;
        }

        if (mLocationPermissionGranted) {
            // Get the businesses and other points of interest located
            // nearest to the device's current location.
            @SuppressWarnings("MissingPermission")
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(mGoogleApiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {
                    /*//clear googlemap markers
                    mGoogleMap.clear();*/

                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        // Add a marker for each place near the device's current location, with an
                        // info window showing place information.
                        String attributions = (String) placeLikelihood.getPlace().getAttributions();
                        String snippet = (String) placeLikelihood.getPlace().getAddress();
                        if (attributions != null) {
                            snippet = snippet + "\n" + attributions;
                        }

                        mGoogleMap.addMarker(new MarkerOptions()
                                .position(placeLikelihood.getPlace().getLatLng())
                                .title((String) placeLikelihood.getPlace().getName())
                                .snippet(snippet));
                        Toast.makeText(MainActivity.this, "Cahak place likelyhood :: "+placeLikelihood.getPlace().getName(), Toast.LENGTH_LONG).show();
                        getCorrespondingLocationAddress(mCurrentLocation);
                    }
                    // Release the place likelihood buffer.
                    likelyPlaces.release();
                }
            });
        } else {
            mGoogleMap.addMarker(new MarkerOptions()
                    .position(mDefaultLocation)
                    .title(getString(R.string.default_info_title))
                    .snippet(getString(R.string.default_info_snippet)));
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    @SuppressWarnings("MissingPermission")
    private void updateLocationUI() {
        if (mGoogleMap == null) {
            return;
        }

        if (mLocationPermissionGranted) {
            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mGoogleMap.setMyLocationEnabled(false);
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
            mCurrentLocation = null;
        }
    }

    /**
     * retrieve the street address from the geocoder, handle any errors that may occur,
     * and send the results back to the activity that requested the address.
     */
    protected void getCorrespondingLocationAddress(Location location) {//

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            String errorMessage = "";

            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        // get single address with this
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
                Toast.makeText(MainActivity.this, Constants.FAILURE_RESULT+ "Err: "+errorMessage, Toast.LENGTH_LONG).show();
            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<String>();

                // Fetch the address lines using getAddressLine,
                // join them, and send them to the thread.
                for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
                Log.e(TAG, getString(R.string.address_found)+" :: "+address.toString());
                Log.e(TAG, getString(R.string.address_found)+" :: "+address.toString());
                Log.e(TAG, getString(R.string.address_found)+" :: "+addressFragments.toString());
                Log.e(TAG, getString(R.string.address_found)+" :: "+addressFragments.toString());

                Toast.makeText(MainActivity.this, Constants.SUCCESS_RESULT+" :: "+ TextUtils.join(System.getProperty("line.separator"), addressFragments), Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(), "Address: "+addressFragments.toString(), Toast.LENGTH_LONG).show();

            }

    }

    protected void startIntentService() {
        Intent intent = new Intent(this, GetAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void confirmExit() {
        dialog = new MaterialDialog.Builder(this)
                .title(R.string.exitTitle)
                .content(R.string.exitString)
                .positiveText(R.string.exitPositive)
                .negativeText(R.string.exitNegative)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        //close app
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            finishAffinity();
                        } else finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        //Dismiss dialog and remain on page
                        dialog.dismiss();
                    }
                })
                .show();
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.home) {
            // Do nothing

        } else if (id == R.id.my_locs) {
            //Load Location Fragment

        } else if (id == R.id.trending) {
            //Load Trending Fragment

        } else if (id == R.id.about) {
            //hide bottomsheet
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            //Load About Fragment
            Fragment frag = new AboutFragment();

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.map, frag, getString(R.string.frag_about));
            mTitle.setText(R.string.title_about_fragment);//change the actionbar title text
            fragmentTransaction.commit();
        } else if (id == R.id.exit) {
            //Exit the app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                finishAffinity();
            } else finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //methoid to retrieve google account tied to phone and username
    private String getUsername() {
        manager = AccountManager.get(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return TODO;
        }
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type
            // values.
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");
            if (parts.length > 0 && parts[0] != null)
                return email;//parts[0];
            else
                return null;
        } else
            return null;
    }

    //get user mail linked to phone
    private String getUserMail() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return TODO;
        }
        Account[] accounts = AccountManager.get(this).getAccounts();
        Log.e("", "Size: " + accounts.length);
        String strGmail = null;
        for (Account account : accounts) {

            String possibleEmail = account.name;
            String type = account.type;

            if (type.equals("com.google")) {
                strGmail = possibleEmail;

                Log.e("", "Emails: " + strGmail);
                break;
            }
        }
        return strGmail;
    }


    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            //displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                Toast.makeText(MainActivity.this, getString(R.string.address_found), Toast.LENGTH_LONG).show();
            }

        }
    }

    //Username
    //User google account mail
    //Latitude
    //Longitude
    //country
    //state
    //Local government,
    //other identifiers
    //street address + house number

    //Places Nearby

}
