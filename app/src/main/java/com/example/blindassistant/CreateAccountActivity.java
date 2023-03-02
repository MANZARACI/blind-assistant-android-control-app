package com.example.blindassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class CreateAccountActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput, passwordVerifyInput;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mAuth = FirebaseAuth.getInstance();

        Button createAccountBtn = findViewById(R.id.createAccountBtn);
        emailInput = findViewById(R.id.emailCreate);
        passwordInput = findViewById(R.id.passwordCreate);
        passwordVerifyInput = findViewById(R.id.passwordVerifyCreate);
        progressBar = findViewById(R.id.createAccountProgress);

        createAccountBtn.setOnClickListener(view -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String passwordVerify = passwordVerifyInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)
                    || TextUtils.isEmpty(password)
                    || TextUtils.isEmpty(passwordVerify)) {
                Toast.makeText(CreateAccountActivity.this, R.string.empty_field_text, Toast.LENGTH_LONG)
                        .show();
            } else if (!password.equals(passwordVerify)) {
                Toast.makeText(CreateAccountActivity.this, R.string.different_passwords_text, Toast.LENGTH_LONG)
                        .show();
            } else if (password.length() < 6) {
                Toast.makeText(CreateAccountActivity.this, R.string.short_password_text, Toast.LENGTH_LONG)
                        .show();
            } else {
                createUser(email, password);
            }
        });
    }

    private void createUser(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(CreateAccountActivity.this, LocationListActivity.class));
                        finish();
                    } else {
                        Toast.makeText(CreateAccountActivity.this, R.string.failed_authentication_text, Toast.LENGTH_LONG)
                                .show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CreateAccountActivity.this, R.string.failed_authentication_text, Toast.LENGTH_LONG)
                            .show();
                });
    }
}