package com.example.labex8;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.labex8.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main map activity that:
 *   - Shows user's location
 *   - Allows long press to save a favorite place
 *   - Saves favorites with SharedPreferences (JSON)
 *   - Loads and displays all saved markers
 *   - Navigates to FavoriteListActivity
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private FusedLocationProviderClient mClient;
    private LocationCallback locationCallback;

    private boolean hasCenteredOnce = false;

    private static final String PREFS_NAME = "favorite_places";
    private static final String KEY_PLACES = "places";

    private List<Place> favoritePlaces = new ArrayList<>();

    private static final int LOCATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using ViewBinding for UI access
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Map Fragment
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup location services
        mClient = LocationServices.getFusedLocationProviderClient(this);

        // View Favorites button → open screen 2
        binding.btnViewFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, FavoriteListActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable built-in UI zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Load saved places and show them as markers
        loadPlaces();
        showSavedMarkers();

        // Setup long-press listener for creating favorites
        mMap.setOnMapLongClickListener(latLng -> {
            addFavoritePlace(latLng);
        });

        // Check permissions and start location
        checkPermissionsAndStartLocation();
    }


    private void checkPermissionsAndStartLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );
            return;
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);

        LocationRequest request = new LocationRequest.Builder(5000)
                .setMinUpdateIntervalMillis(3000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();

                if (location != null) {
                    LatLng userPos = new LatLng(location.getLatitude(), location.getLongitude());

                    // Center the camera only once (fix zoom snap-back issue)
                    if (!hasCenteredOnce) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userPos, 15));
                        hasCenteredOnce = true;
                    }
                }
            }
        };

        mClient.requestLocationUpdates(request, locationCallback, null);
    }


    private void addFavoritePlace(LatLng latLng) {

        // Try to convert to address using Geocoder
        String address = getAddressFromLatLng(latLng);

        // Create a new Place object
        Place place = new Place(address, latLng.latitude, latLng.longitude);
        favoritePlaces.add(place);

        // Save to SharedPreferences
        savePlaces();

        // Add marker to map
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(address)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        Toast.makeText(this, "Added: " + address, Toast.LENGTH_SHORT).show();
    }


    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> results = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);

            if (results != null && !results.isEmpty()) {
                return results.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e("Geocoder", "Error: " + e.getMessage());
        }

        // If geocoder fails → fallback
        return "Pinned Location (" + latLng.latitude + ", " + latLng.longitude + ")";
    }


    private void savePlaces() {
        Gson gson = new Gson();
        String json = gson.toJson(favoritePlaces);

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_PLACES, json)
                .apply();
    }


    private void loadPlaces() {
        String json = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_PLACES, null);

        if (json != null) {
            Type type = new TypeToken<ArrayList<Place>>() {}.getType();
            favoritePlaces = new Gson().fromJson(json, type);
        } else {
            favoritePlaces = new ArrayList<>();
        }
    }


    private void showSavedMarkers() {
        for (Place p : favoritePlaces) {
            LatLng pos = new LatLng(p.getLatitude(), p.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(p.getTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            checkPermissionsAndStartLocation();
        }
    }
}
