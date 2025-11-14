package com.example.labex8;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.example.map_demo2.databinding.ActivityFavoriteListBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays all saved favorite places in a ListView.
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

        // Load places from SharedPreferences
        loadPlaces();

        // Populate list view
        ArrayAdapter<Place> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, favoritePlaces);
        binding.listView.setAdapter(adapter);

        // Handle back button
        binding.btnBack.setOnClickListener(v -> finish());
    }

    /** Loads saved places from SharedPreferences */
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
}
