package com.example.blindassistant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Face;
import model.FaceRecyclerViewInterface;
import ui.FaceRecyclerAdapter;

public class FacesActivity extends AppCompatActivity implements FaceRecyclerViewInterface {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private ImageView faceIcon, faceIcon2, faceIcon3;
    private Bitmap bitmap, bitmap2, bitmap3;
    private RequestQueue queue;
    private static final String newFaceUrl = BuildConfig.BACKEND_URL + "/new-face-base64";
    private static final String fetchFacesUrl = BuildConfig.BACKEND_URL + "/faces";
    private static final String deleteFaceUrl = BuildConfig.BACKEND_URL + "/delete-face";
    private List<Face> faceList;
    private RecyclerView recyclerView;
    private FaceRecyclerAdapter faceRecyclerAdapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faces);

        mAuth = FirebaseAuth.getInstance();

        BottomNavigationView bottomBar = findViewById(R.id.bottomNavigationView);
        faceIcon = findViewById(R.id.addFaceIcon);
        faceIcon2 = findViewById(R.id.addFaceIcon2);
        faceIcon3 = findViewById(R.id.addFaceIcon3);
        Button saveFaceBtn = findViewById(R.id.saveFaceBtn);
        recyclerView = findViewById(R.id.faceRecyclerView);
        progressBar = findViewById(R.id.facesLoadingBar);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        faceList = new ArrayList<>();

        faceRecyclerAdapter = new FaceRecyclerAdapter(FacesActivity.this, faceList, this);
        recyclerView.setAdapter(faceRecyclerAdapter);

        queue = Volley.newRequestQueue(this);

        bottomBar.getMenu().findItem(R.id.facesPage).setEnabled(false);

        faceIcon.setOnClickListener(view -> getImage(0));
        faceIcon2.setOnClickListener(view -> getImage(1));
        faceIcon3.setOnClickListener(view -> getImage(2));

        saveFaceBtn.setOnClickListener(view -> {
            if (bitmap == null || bitmap2 == null || bitmap3 == null) {
                Toast.makeText(FacesActivity.this, R.string.not_enough_images, Toast.LENGTH_LONG)
                        .show();
            } else {
                LinearLayout parentLayout = new LinearLayout(this);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                );
                layoutParams.setMargins(dpToPx(20), 0, dpToPx(50), 0);

                EditText editText = new EditText(this);
                editText.setLayoutParams(layoutParams);
                parentLayout.addView(editText);

                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
                dialogBuilder.setTitle(R.string.new_face_dialog_title);
                dialogBuilder.setView(parentLayout);
                dialogBuilder.setPositiveButton(R.string.new_face_confirm_btn, null);
                dialogBuilder.setNegativeButton(R.string.new_face_cancel_btn, null);
                dialogBuilder.setCancelable(false);
                AlertDialog dialog = dialogBuilder.create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();

                //overriding default button behaviour to prevent dialog dismiss if input field is empty
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {
                    String input = editText.getText().toString().trim();
                    if (TextUtils.isEmpty(input)) {
                        Toast.makeText(FacesActivity.this, R.string.new_face_empty_identifier, Toast.LENGTH_LONG)
                                .show();
                    } else {
                        dialog.dismiss();
                        progressBar.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        currentUser.getIdToken(true).addOnSuccessListener(getTokenResult -> {
                            sendNewFaceRequest(getTokenResult.getToken(), input);
                        }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            Toast.makeText(FacesActivity.this, R.string.went_wrong_text, Toast.LENGTH_LONG)
                                    .show();
                        });
                    }
                });
            }
        });

        bottomBar.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.locationListPage) {
                startActivity(new Intent(FacesActivity.this, LocationListActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
            return false;
        });
    }

    private void getFacesRequest() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, fetchFacesUrl + "?userId=" + currentUser.getUid(), null, response -> {
            faceList.clear();
            try {
                JSONArray faces = response.getJSONArray("faceArray");
                for (int i = 0; i < faces.length(); i++) {
                    JSONObject faceJSON = (JSONObject) faces.get(i);
                    Face face = new Face(faceJSON.getString("id"), faceJSON.getString("label"));
                    faceList.add(face);
                }
                faceRecyclerAdapter.notifyDataSetChanged();


                recyclerView.setVisibility(View.VISIBLE);
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(FacesActivity.this, R.string.went_wrong_text, Toast.LENGTH_LONG)
                        .show();
            }
            progressBar.setVisibility(View.GONE);

        }, error -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(FacesActivity.this, R.string.went_wrong_text, Toast.LENGTH_LONG)
                    .show();
        });

        queue.add(jsonObjectRequest);
    }

    private void sendNewFaceRequest(String idToken, String identifier) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("encoded", getStringImage(bitmap));
            jsonBody.put("encoded2", getStringImage(bitmap2));
            jsonBody.put("encoded3", getStringImage(bitmap3));
            jsonBody.put("idToken", idToken);
            jsonBody.put("label", identifier);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestBody = jsonBody.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, newFaceUrl, response -> {
            try {
                JSONObject responseObject = new JSONObject(response);
                Toast.makeText(FacesActivity.this, responseObject.getString("successMessage"), Toast.LENGTH_LONG)
                        .show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            getFacesRequest();
        }, error -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(FacesActivity.this, R.string.went_wrong_text, Toast.LENGTH_LONG)
                    .show();
        }) {
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

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                120000, //120 seconds
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(stringRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            switch (requestCode) {
                case 0:
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    faceIcon.setImageBitmap(bitmap);
                    break;
                case 1:
                    try {
                        bitmap2 = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    faceIcon2.setImageBitmap(bitmap2);
                    break;
                case 2:
                    try {
                        bitmap3 = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    faceIcon3.setImageBitmap(bitmap3);
                    break;
            }
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
                startActivity(new Intent(FacesActivity.this, SettingsActivity.class));
                break;
            case R.id.actionSignout:
                if (currentUser != null && mAuth != null) {
                    mAuth.signOut();
                    startActivity(new Intent(FacesActivity.this, LoginActivity.class));
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            getFacesRequest();
        }
    }

    private String getStringImage(Bitmap bm) {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, ba);
        byte[] imagebyte = ba.toByteArray();
        return Base64.encodeToString(imagebyte, Base64.DEFAULT);
    }

    private void getImage(int code) {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Image"), code);
    }

    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    private void sendDeleteFaceRequest(String idToken, int pos) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("idToken", idToken);
            jsonBody.put("faceId", faceList.get(pos).getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestBody = jsonBody.toString();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, deleteFaceUrl, response -> {
            getFacesRequest();
        }, error -> {
            Toast.makeText(FacesActivity.this, R.string.went_wrong_text, Toast.LENGTH_LONG)
                    .show();
            getFacesRequest();
        }) {
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

    @Override
    public void onItemClick(int position) {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle(R.string.are_you_sure);
        dialogBuilder.setMessage(R.string.deleting_face_dialog_message);
        dialogBuilder.setPositiveButton(R.string.dialog_yes_option, ((dialogInterface, i) -> {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            currentUser.getIdToken(true).addOnSuccessListener(getTokenResult -> {
                sendDeleteFaceRequest(getTokenResult.getToken(), position);
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                Toast.makeText(FacesActivity.this, R.string.went_wrong_text, Toast.LENGTH_LONG)
                        .show();
            });
        }));
        dialogBuilder.setNegativeButton(R.string.dialog_no_option, null);
        dialogBuilder.show();
    }
}