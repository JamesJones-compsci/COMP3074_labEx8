package com.example.labex8;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.labex8.Place;
import com.example.labex8.databinding.ActivityFavoriteListBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Displays all saved favorite places in a ListView
 */
public class FavoriteListActivity extends AppCompatActivity {

    private ActivityFavoriteListBinding binding;
    private List<Place> favoritePlaces = new ArrayList<>();
    private static final String PREFS_NAME = "favorite_places";
    private static final String KEY_PLACES = "places";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoriteListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadPlaces();

        // Convert lat/lng to addresses
        List<String> placeAddresses = getAddressesFromPlaces(favoritePlaces);

        // Populate ListView with addresses
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                placeAddresses
        );
        binding.listView.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
    }

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

    private List<String> getAddressesFromPlaces(List<Place> places) {
        List<String> addressesList = new ArrayList<>();
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        for (Place place : places) {
            try {
                List<Address> addresses = geocoder.getFromLocation(place.getLatitude(), place.getLongitude(), 1);
                if (!addresses.isEmpty()) {
                    // Show full address
                    addressesList.add(place.getTitle() + ": " + addresses.get(0).getAddressLine(0));
                } else {
                    // Fallback to lat/lng if address not found
                    addressesList.add(place.getTitle() + ": " +
                            "Lat: " + place.getLatitude() + ", Lng: " + place.getLongitude());
                }
            } catch (IOException e) {
                e.printStackTrace();
                // Fallback in case of network or geocoder failure
                addressesList.add(place.getTitle() + ": " +
                        "Lat: " + place.getLatitude() + ", Lng: " + place.getLongitude());
            }
        }

        return addressesList;
    }
}