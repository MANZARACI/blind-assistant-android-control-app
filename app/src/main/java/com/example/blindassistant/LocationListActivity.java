package com.example.blindassistant;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import model.Location;
import model.LocationRecyclerViewInterface;
import ui.LocationRecyclerAdapter;
import util.Settings;

public class LocationListActivity extends AppCompatActivity implements LocationRecyclerViewInterface {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference mDatabase, mLocationsRef, mDeviceIdRef;
    private ValueEventListener deviceIdListener, locationsListener;
    private String currentDeviceId;
    private List<Location> locationList;
    private RecyclerView recyclerView;
    private LocationRecyclerAdapter locationRecyclerAdapter;
    private TextView infoText;
    private ProgressBar progressBar;
    private Button requestLocationBtn;
    private RequestQueue queue;
    private static final String requestLocationUrl = BuildConfig.BACKEND_URL + "/set-location-request";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_list);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        requestLocationBtn = findViewById(R.id.requestLocationBtn);
        infoText = findViewById(R.id.infoText);
        progressBar = findViewById(R.id.locationListLoadingBar);
        recyclerView = findViewById(R.id.recyclerView);
        BottomNavigationView bottomBar = findViewById(R.id.bottomNavigationView);

        bottomBar.getMenu().findItem(R.id.locationListPage).setEnabled(false);

        bottomBar.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.facesPage) {
                startActivity(new Intent(LocationListActivity.this, FacesActivity.class));
                finish();
            }
            return false;
        });

        loadSettings();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        locationList = new ArrayList<>();

        locationRecyclerAdapter = new LocationRecyclerAdapter(LocationListActivity.this,
                locationList, this);
        recyclerView.setAdapter(locationRecyclerAdapter);

        queue = Volley.newRequestQueue(this);

        locationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                infoText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                if (!snapshot.exists()) {
                    infoText.setText(R.string.no_location_text);
                    infoText.setVisibility(View.VISIBLE);
                } else {
                    locationList.clear();

                    for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                        Location location = locationSnapshot.getValue(Location.class);
                        locationList.add(0, location);
                    }

                    locationRecyclerAdapter.notifyDataSetChanged();
                    recyclerView.setVisibility(View.VISIBLE);
                }

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                recyclerView.setVisibility(View.GONE);
                infoText.setText(R.string.went_wrong_text);
                infoText.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        };

        deviceIdListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                infoText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                if (!snapshot.exists()) {
                    infoText.setText(R.string.no_device_text);
                    infoText.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                } else {
                    //remove old listener if there is one
                    if (mLocationsRef != null) {
                        mLocationsRef.removeEventListener(locationsListener);
                    }
                    currentDeviceId = snapshot.getValue(String.class);
                    mLocationsRef = mDatabase.child("devices").child(currentDeviceId);
                    mLocationsRef.limitToLast(5).addValueEventListener(locationsListener);//add new listener to get locations
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                recyclerView.setVisibility(View.GONE);
                infoText.setText(R.string.went_wrong_text);
                infoText.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        };

        requestLocationBtn.setOnClickListener(view -> {
            StringRequest stringRequest = new StringRequest(Request.Method.GET, requestLocationUrl + "?deviceId=" + currentDeviceId, response -> {
                Toast.makeText(LocationListActivity.this, response, Toast.LENGTH_LONG)
                        .show();
            }, error -> {
                Toast.makeText(LocationListActivity.this, R.string.went_wrong_text, Toast.LENGTH_LONG)
                        .show();
            });

            queue.add(stringRequest);
        });
    }

    private void loadSettings() {
        Settings settings = Settings.getInstance();
        SharedPreferences sharedPref = this.getSharedPreferences("boundary", MODE_PRIVATE);
        int sizeOfBoundary = sharedPref.getInt("sizeOfBoundary", 0);
        if (sizeOfBoundary != 0) {
            List<LatLng> boundaryPoints = new ArrayList<>();
            for (int i = 0; i < sizeOfBoundary; i++) {
                String latitude = sharedPref.getString("lat" + i, "0");
                String longitude = sharedPref.getString("lng" + i, "0");

                double lat = Double.parseDouble(latitude);
                double lng = Double.parseDouble(longitude);

                com.google.android.gms.maps.model.LatLng position = new LatLng(lat, lng);

                boundaryPoints.add(position);
            }
            settings.setBoundaryPoints(boundaryPoints);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSettings:
                startActivity(new Intent(LocationListActivity.this, SettingsActivity.class));
                break;
            case R.id.actionSignout:
                if (currentUser != null && mAuth != null) {
                    mAuth.signOut();
                    startActivity(new Intent(LocationListActivity.this, LoginActivity.class));
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        recyclerView.setVisibility(View.GONE);
        infoText.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mDeviceIdRef = mDatabase.child("users").child(currentUser.getUid()).child("deviceId");
            mDeviceIdRef.addValueEventListener(deviceIdListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDeviceIdRef.removeEventListener(deviceIdListener);
        if (mLocationsRef != null) {
            mLocationsRef.removeEventListener(locationsListener);
        }
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(LocationListActivity.this, LocationMapActivity.class);
        intent.putExtra("lat", locationList.get(position).getLat());
        intent.putExtra("lng", locationList.get(position).getLng());
        startActivity(intent);
    }
}