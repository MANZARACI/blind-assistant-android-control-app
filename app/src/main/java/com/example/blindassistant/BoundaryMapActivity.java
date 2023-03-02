package com.example.blindassistant;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

import util.Settings;

public class BoundaryMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private List<LatLng> polygonPoints;
    private Polygon polygon;
    private GoogleMap map;
    private SharedPreferences.Editor editor;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boundary_map);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        polygonPoints = new ArrayList<>();

        SharedPreferences sharedPref = this.getSharedPreferences("boundary", MODE_PRIVATE);
        editor = sharedPref.edit();

        settings = Settings.getInstance();
        polygonPoints = settings.getBoundaryPoints();

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.boundaryMapLayout, mapFragment)
                .commit();

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        if (polygonPoints.size() != 0) {
            for (LatLng position : polygonPoints) {
                googleMap.addMarker(new MarkerOptions().position(position));
            }

            polygon = googleMap.addPolygon(new PolygonOptions()
                    .addAll(polygonPoints));
        }

        googleMap.setOnMapClickListener(latLng -> {
            googleMap.addMarker(new MarkerOptions().position(latLng));
            polygonPoints.add(latLng);
            if (polygonPoints.size() == 3) {
                polygon = googleMap.addPolygon(new PolygonOptions()
                        .addAll(polygonPoints));
            } else if (polygonPoints.size() > 3) {
                polygon.setPoints(polygonPoints);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.boundary_map_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionReset:
                resetBoundary();
                break;
            case R.id.actionSave:
                saveBoundary();
                break;
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetBoundary() {
        editor.putInt("sizeOfBoundary", 0);
        editor.apply();
        map.clear();
        polygonPoints.clear();
        settings.setBoundaryPoints(polygonPoints);
    }

    private void saveBoundary() {
        if (polygonPoints.size() < 3) {
            Toast.makeText(BoundaryMapActivity.this, R.string.not_enough_points_text, Toast.LENGTH_LONG)
                    .show();
        } else {
            for (int i = 0; i < polygonPoints.size(); i++) {
                editor.putString("lat" + i, Double.toString(polygonPoints.get(i).latitude));
                editor.putString("lng" + i, Double.toString(polygonPoints.get(i).longitude));
            }
            editor.putInt("sizeOfBoundary", polygonPoints.size());
            editor.apply();
            settings.setBoundaryPoints(polygonPoints);
            Toast.makeText(BoundaryMapActivity.this, R.string.boundary_saved_text, Toast.LENGTH_LONG)
                    .show();
        }
    }
}