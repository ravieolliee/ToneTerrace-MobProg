package com.toneterrace.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.toneterrace.app.MainActivity;
import com.toneterrace.app.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPass;
    private Button btnRegister;
    private TextView tvGoToLogin;

    // Firebase Authentication Instance
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Bind Views
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPass = findViewById(R.id.etRegisterConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // 3. Handle "Go to Login" click
        tvGoToLogin.setOnClickListener(v -> {
            finish(); // Just close this activity to go back to Login
        });

        // 4. Handle Register Button click
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPass = etConfirmPass.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be >= 6 characters");
            return;
        }
        if (!password.equals(confirmPass)) {
            etConfirmPass.setError("Passwords do not match");
            return;
        }

        // Create User in Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Success
                            Toast.makeText(RegisterActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();
                            // Navigate to Home
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finishAffinity(); // Clear back stack so user can't go back to login
                        } else {
                            // Failure
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}