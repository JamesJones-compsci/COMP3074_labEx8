package com.example.labex8;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.Toast;

import com.example.labex8.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient mClient;
    private LocationRequest request;
    private LocationCallback callback;
    private static final int REQUEST_CODE = 1;

    // List to store all favorite places
    private List<Place> favoritePlaces = new ArrayList<>();

    // SharedPreferences name
    private static final String PREFS_NAME = "favorite_places";
    private static final String KEY_PLACES = "places";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize location client
        mClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Navigate to the favorites list screen
        binding.btnViewFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, FavoriteListActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Load saved markers
        loadPlaces();
        showSavedMarkers();

        // Check permissions
        if (!isGrantedLocationPermission()) {
            requestLocationPermission();
        } else {
            updateLocation();
        }

        // Long press listener to add favorite place
        mMap.setOnMapLongClickListener(latLng -> {
            addFavoritePlace(latLng);
        });
    }

    /** Adds a new favorite place and saves it persistently */
    private void addFavoritePlace(LatLng latLng) {
        String title = "Favorite Place " + (favoritePlaces.size() + 1);
        Place newPlace = new Place(title, latLng.latitude, latLng.longitude);
        favoritePlaces.add(newPlace);

        // Save updated list
        savePlaces();

        // Add marker on map
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        Toast.makeText(this, "Added: " + title, Toast.LENGTH_SHORT).show();
    }

    /** Save all favorite places to SharedPreferences */
    private void savePlaces() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(favoritePlaces);
        editor.putString(KEY_PLACES, json);
        editor.apply();
    }

    /** Load favorite places from SharedPreferences */
    private void loadPlaces() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_PLACES, null);
        Type type = new TypeToken<ArrayList<Place>>() {}.getType();
        if (json != null) {
            favoritePlaces = gson.fromJson(json, type);
        } else {
            favoritePlaces = new ArrayList<>();
        }
    }

    /** Display saved markers on the map */
    private void showSavedMarkers() {
        for (Place place : favoritePlaces) {
            LatLng position = new LatLng(place.getLatitude(), place.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(place.getTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
    }

    private boolean isGrantedLocationPermission() {
        return ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE
        );
    }

    private void updateLocation() {
        request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .build();

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14));
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mClient.requestLocationUpdates(request, callback, null);
    }

    /** Handle location permission result */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateLocation();
        }
    }
}
