package com.water.app.waterconversation.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.R;

public class LocationFragment extends Fragment
        implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public static boolean isLocationFragment = true;
    private String TAG = "LocationFragment";

    private GoogleMap mMap;
    SupportMapFragment mMapFragment;
    private GoogleApiClient googleApiClient;
    Location myLocation;

    boolean mapReady = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_location, container, false);
        mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);


        googleApiClient = new GoogleApiClient.Builder(getActivity()).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();

        //連接google api
        if (googleApiClient != null) {
            googleApiClient.connect();
//            mMapFragment.getMapAsync(this);
        }

        return v;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        isLocationFragment = false;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

//        LatLng sydney = new LatLng(23, 121);
        LatLng sydney = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        Log.d(TAG, "onMapReady: " + sydney);
        mMap.addMarker(new MarkerOptions().position(sydney)
                .title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15));
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
//        mMap.setMyLocationEnabled(true);
//        mMap.setOnMyLocationButtonClickListener(this);
//        mMap.setOnMyLocationClickListener(this);

    }

    protected synchronized void buildGoogleApi(){
        //連接google api
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(getActivity(), "Current location:\n" + location, Toast.LENGTH_LONG).show();

    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(getActivity(), "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

     @Override
     public void onConnected(@Nullable Bundle bundle) {
//         mMapFragment.getMapAsync(this);
         if (ActivityCompat.checkSelfPermission(getActivity(),
                 Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                 &&  ActivityCompat.checkSelfPermission(getActivity(),
                 Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
             return;
         }

         // Permissions ok, we get last location
         //宣告位置數值
          myLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
         Log.i(TAG, "onConnected: start get gps " + myLocation);
         if (myLocation != null) {
             mMapFragment.getMapAsync(this);
         }
     }

     @Override
     public void onConnectionSuspended(int i) {

     }

     @Override
     public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

     }

     @Override
     public void onLocationChanged(Location location) {
         Log.d(TAG, "onLocationChanged: "+location);
//         LatLng sydney = new LatLng(location.getLatitude(), location.getLongitude());
//         mMap.addMarker(new MarkerOptions().position(sydney)
//                 .title("Marker in Sydney"));
//         mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,10));

//         mMapFragment.getMapAsync(this);
     }
 }
