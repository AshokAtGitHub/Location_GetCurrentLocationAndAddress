package com.ashok.location_basicexamplefromgoogle;
/**
 * This app show current LOCATION of the device as Latitude, Longituce.
 * It also show the streeet address of the LOCATION found in above line.
 * Implementation notes:
 * (1) This app use Google-Location-API to retrieve the last known location which is
 *     current-location (Latitude, Longitude) of the mobile device.
 * (2) After getting current-location latitude, longitude; It uses "Reverse-Geocoding" to get
 *     street-address of that location. For this part, WiFi or GSM-Data-phone connection is
 *     needed. Else it fails.
 *
 *     ** Notes:
 *        (1) For getting Location, this app use the RUN-TIme-Permission as required by Google
 *            since Android-Marshmallow 6.0 API 23 release.
 *        (2) This sample uses Google Play services (GoogleApiClient) which do not need
 *            to 'authenticate'
 *
 -Ref for Run-Time permission developers.google.com/android/guides/permissions#prerequisites
 - Ref for "future" Authentication
 github.com/googlesamples/android-google-accounts/tree/master/QuickStart
 */
import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.location.Location;
import android.location.Address;
//import android.os.Bundle;
//import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
//import com.google.android.gms.identity.intents.Address;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
                                   ConnectionCallbacks, OnConnectionFailedListener {
    protected static final String TAG = "agk";
    protected static int REQUEST_LOCATION_CODE = 1001;
    protected static int REQUEST_RESOLVE_ERROR = 1002;
    protected static int ERROR_CODE_CONN_FAILED = 1003;

    protected GoogleApiClient mGoogleApiClient;//provide entry point to Google Play Services
    protected Location mLastLocation;//Lat, Longitude of a location

    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;
    protected TextView mTxtAddressLine, mTxtCityAndState, mTxtCountryName, mTxtError;

    boolean mErrorHandlingOnConnectionFailedTryResolving = false;
    //-------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLatitudeText = (TextView) findViewById((R.id.txt_latitude));
        mLongitudeText = (TextView) findViewById((R.id.txt_longitude));
        mTxtAddressLine = (TextView) findViewById(R.id.txt_addressLine);
        mTxtCityAndState = (TextView) findViewById(R.id.txt_cityAndState);
        mTxtCountryName = (TextView) findViewById(R.id.txt_country);
        mTxtError = (TextView) findViewById(R.id.txt_error);
        buildGoogleApiClient();
    }
    //-------------------------------------------------------------
    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    //---------------------------------------------------
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }
    //----------------------------------------------------
    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    //----------------------------------------------------
    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override//auto TODO
    public void onConnected(@Nullable Bundle bundle) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.

        //ak- June 2, 2016
        // AUTO-CODE -- Permission-Check added using Android Stuio suggestion !!! Red-bulb-auto insert
        // Google new policy of Run-time-permission check.
        Log.i(TAG, "Enter:onConnected()");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Log.i(TAG, "Permission-NOT-Granted:onConnected(). Request Permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_CODE);
        } else {
            Log.i(TAG, "Permission-Granted:onConnected()");
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation == null) {//call 1 more time to get location (In case No location was set initially
                Log.i(TAG, "onConnect():Calling getLastLocation() again.");
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            }
            if (mLastLocation != null) {
                displayLatLong_and_streetAddr(mLastLocation);
                //clear Error line
                mTxtError.setText(" ");
            } else {
                Log.i(TAG, "onConnected():mLastLocation = NULL");
                mTxtError.setText("Error:onConnected()-LastLocation=NULL");
                mTxtError.setTextColor(getResources().getColor(R.color.red));
                //Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
            }
        }//permission-granted
    }//onConnected()
    //-----------------------------------------------------------------
    /**
     * Callback-Method for requestPermissions()
     */
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults){
        Log.i(TAG, "Enter:onRequestPermissionsResult()");
        if (requestCode == REQUEST_LOCATION_CODE){
            if ((grantResults.length == 1)
                && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                //can use Location-API here !!
                //mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation != null) {
                    displayLatLong_and_streetAddr(mLastLocation);
                    //clear Error line
                    mTxtError.setText(" ");
                } else {
                    Log.i(TAG, "onRequestPermissionResult():mLastLocation = NULL");
                    mTxtError.setText("onRequestPermissionResult():LastLocation=NULL");
                    mTxtError.setTextColor(getResources().getColor(R.color.red));
                    //Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
                }
            }
            else {
                Log.i(TAG, "Permission-NOT-Granted:onRequestPermissonsResult()");
                mTxtError.setText("Permission-NOT-granted:onRequestsPermissionsResult()");
                mTxtError.setTextColor(getResources().getColor(R.color.red));
            }
        }
    }
    /** ---------------------------------------------------
     * Display Latitute, Longitude and City name using Reverse-GeoCoding
     */
    protected void displayLatLong_and_streetAddr(Location mLastLocation)  {
        Double latitude, longitude;
        String addressLine = "addressLine";
        String cityAndState = "City&stateName";
        String countryName = "CountryName";
        latitude = mLastLocation.getLatitude();
        longitude = mLastLocation.getLongitude();

        //Get City, State anc Country name by using Reverse-Geocoding
        Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
        try {//Reverse GeoCoding - Get street-address from Latitude, Longitude
            List<Address> address = geoCoder.getFromLocation(latitude, longitude,1);
            addressLine = address.get(0).getAddressLine(0);
            cityAndState = address.get(0).getAddressLine(1);
            countryName = address.get(0).getAddressLine(2);
        } catch (IOException ioe) {
            Log.i(TAG,"Error:ReverseGeoCoding:NetworkNotAvailable:display_lat_lng_cityName()");
            mTxtError.setText("Error:ReverseGeoCoding:IOException.NetworkNotAvailable.display_lat_lng_cityName()");
            ioe.printStackTrace();
        } catch (IllegalArgumentException iae){
            Log.i(TAG,"Error:ReverseGeoCoding:IllegalArgs:display_lat_lng_cityName()");
            mTxtError.setText("Error:ReverseGeoCoding:IllegalArgs:display_lat_lng_cityName()");
            iae.printStackTrace();
        }
        mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel,latitude));
        mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel,longitude));
        mTxtAddressLine.setText(addressLine);
        mTxtCityAndState.setText(cityAndState);
        mTxtCountryName.setText(countryName);
    }
    //------------------------------------------------------------
    @Override//auto TODO
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended;Cause="+cause);
        mTxtError.setText("Error:onConnectionSuspended():Cause="+cause);
        mGoogleApiClient.connect();
    }
    //------------------------------------------------------------
    @Override//auto TODO
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        
        int errorCodeForConnectionFailed = connectionResult.getErrorCode();
        String errorCodeStr = getErrorCodeStr_onConnenctionFailed(errorCodeForConnectionFailed);
        Log.i(TAG, "onConnectionFailed():ErrorCode=(" + errorCodeForConnectionFailed + ")"+
                                                                                errorCodeStr);
        mTxtError.setText("onConnectionFailed:ErrCodeStr="+errorCodeStr);
        //ERROR-Handing Ref: developers.google.com/android/guides/permissions#handle_connection_failures
        if (mErrorHandlingOnConnectionFailedTryResolving)
            return;//already resolving
        else if (connectionResult.hasResolution()) {
            try {
                mErrorHandlingOnConnectionFailedTryResolving = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException sie) {
                //error with resolution intent; try again
                mGoogleApiClient.connect();
            }//catch
        }else{
                //show dialog using GooglePlayServiceUtil.getErrorDialog()
                GooglePlayServicesUtil.getErrorDialog(ERROR_CODE_CONN_FAILED, this,
                        REQUEST_RESOLVE_ERROR).show();
                Log.i(TAG,"onConnectionFailed():Just Before Finish()");
                mTxtError.setText("onConnectionFailed():Just Before Finish()");
                finish();
        }
    }
    /** -----------------------------------------------------------------------
     * Error-code when this client fail to connect to Google-Play-Services.
     * These error-codes are used by GoogleApiClient.onConnectionFailed()
     * Ref: developers.google.com/android/reference/com/google/android/gms/common/ConnectionResult#constant-summary
     */
    String getErrorCodeStr_onConnenctionFailed(int errorCode){
        String errorStr;
        switch (errorCode){
            case ConnectionResult.API_UNAVAILABLE: errorStr = "API_UNAVAILABLE";break;
            case ConnectionResult.CANCELED: errorStr = "CANCELLED";break;
            case ConnectionResult.DEVELOPER_ERROR: errorStr = "DEVELOPER_ERROR";break;
            case ConnectionResult.INTERNAL_ERROR: errorStr = "INTERNAL_ERROR";break;
            case ConnectionResult.INTERRUPTED: errorStr = "INTERRUPTED"; break;
            case ConnectionResult.INVALID_ACCOUNT: errorStr = "INVALID_ACCOUNT"; break;
            case ConnectionResult.LICENSE_CHECK_FAILED: errorStr = "LICENSE_CHECK_FAILED"; break;
            case ConnectionResult.NETWORK_ERROR:errorStr = "NETWORK_ERROR"; break;
            case ConnectionResult.RESOLUTION_REQUIRED:errorStr = "RESOLUTION_REQUIRED"; break;
            case ConnectionResult.RESTRICTED_PROFILE:errorStr = "RESTRICTED_PROFILE"; break;
            case ConnectionResult.SERVICE_DISABLED:errorStr = "SERVICE_DISABLED"; break;
            case ConnectionResult.SERVICE_INVALID:errorStr = "SERVICE_INVALID"; break;
            case ConnectionResult.SERVICE_MISSING:errorStr = "SERVICE_MISSING"; break;
            case ConnectionResult.SERVICE_MISSING_PERMISSION:errorStr = "SERVICE_MISSING_PERMISSION"; break;
            case ConnectionResult.SERVICE_UPDATING:errorStr = "SERVICE_UPDATING"; break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:errorStr = "SERVICE_VERSION_UPDATE_REQUIRED"; break;
            case ConnectionResult.SIGN_IN_FAILED:errorStr = "SIGN_IN_FAILED"; break;
            case ConnectionResult.SIGN_IN_REQUIRED:errorStr = "SIGN_IN_REQUIRED"; break;
            case ConnectionResult.SUCCESS:errorStr = "SUCCESS"; break;
            case ConnectionResult.TIMEOUT:errorStr = "TIMEOUT"; break;

            default: errorStr = "UNKNOWN_ERROR!!";
        }
        return errorStr;
    }
}