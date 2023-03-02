package com.example.blindassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private AutoCompleteTextView emailInput;
    private EditText passwordInput;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        Button loginBtn = findViewById(R.id.signInBtn);
        Button switchToCreateBtn = findViewById(R.id.switchToCreateBtn);
        progressBar = findViewById(R.id.loginProgress);
        emailInput = findViewById(R.id.emailLogin);
        passwordInput = findViewById(R.id.passwordLogin);

        switchToCreateBtn.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class));
        });

        loginBtn.setOnClickListener(view -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, R.string.empty_field_text, Toast.LENGTH_LONG)
                        .show();
            } else {
                login(email, password);
            }
        });
    }

    private void login(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, LocationListActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.failed_authentication_text, Toast.LENGTH_LONG)
                                .show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, R.string.failed_authentication_text, Toast.LENGTH_LONG)
                            .show();
                });
    }
}