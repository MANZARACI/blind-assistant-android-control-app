package com.example.blindassistant;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class SettingsActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference mDatabase, mDeviceIdRef;
    private ValueEventListener deviceIdListener;
    private EditText deviceIdInput;
    private ProgressBar progressBar;
    private RequestQueue queue;
    private static final String url = BuildConfig.BACKEND_URL + "/deviceId";
    private String currentDeviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        deviceIdInput = findViewById(R.id.deviceIdInput);
        progressBar = findViewById(R.id.settingsLoadingBar);
        Button deviceIdUpdateBtn = findViewById(R.id.deviceIdUpdateBtn);
        Button setBoundaryBtn = findViewById(R.id.setBoundaryBtn);
        Button boundaryInfoBtn = findViewById(R.id.boundaryInfoBtn);

        queue = Volley.newRequestQueue(this);

        deviceIdListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentDeviceId = snapshot.getValue(String.class);
                    deviceIdInput.setText(currentDeviceId);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SettingsActivity.this, R.string.went_wrong_text, Toast.LENGTH_LONG)
                        .show();
                progressBar.setVisibility(View.GONE);
            }
        };

        boundaryInfoBtn.setOnClickListener(view -> {
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
            dialogBuilder.setTitle(R.string.boundary_dialog_title);
            dialogBuilder.setMessage(R.string.boundary_description);
            dialogBuilder.show();
        });

        deviceIdUpdateBtn.setOnClickListener(view -> {
            String newDeviceId = deviceIdInput.getText().toString().trim();
            if (TextUtils.isEmpty(newDeviceId)) {
                Toast.makeText(SettingsActivity.this, R.string.empty_device_id, Toast.LENGTH_LONG)
                        .show();
            } else {
                progressBar.setVisibility(View.VISIBLE);

                currentUser.getIdToken(true).addOnSuccessListener(getTokenResult -> {
                    sendNewDeviceIdRequest(getTokenResult.getToken(), newDeviceId);
                });
            }
        });

        setBoundaryBtn.setOnClickListener(view -> {
            startActivity(new Intent(SettingsActivity.this, BoundaryMapActivity.class));
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        progressBar.setVisibility(View.VISIBLE);
        if (currentUser != null) {
            mDeviceIdRef = mDatabase.child("users").child(currentUser.getUid()).child("deviceId");
            mDeviceIdRef.addValueEventListener(deviceIdListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDeviceIdRef.removeEventListener(deviceIdListener);
    }

    private void sendNewDeviceIdRequest(String idToken, String newDeviceId) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("idToken", idToken);
            jsonBody.put("newDeviceId", newDeviceId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestBody = jsonBody.toString();

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST, url, response -> {
            progressBar.setVisibility(View.GONE);
            try {
                JSONObject responseObject = new JSONObject(response);
                Toast.makeText(SettingsActivity.this, responseObject.getString("successMessage"), Toast.LENGTH_LONG)
                        .show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(SettingsActivity.this, R.string.went_wrong_text, Toast.LENGTH_LONG)
                    .show();
        }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                            requestBody, "utf-8");
                    return null;
                }
            }
        };

        queue.add(stringRequest);
    }
}