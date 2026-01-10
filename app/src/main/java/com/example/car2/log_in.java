package com.example.car2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class log_in extends AppCompatActivity {
    EditText etEmail, etPass;
    Button btnLogin,btnReset,btnCreate;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        getWindow().setStatusBarColor(Color.parseColor("#dbf2ff"));

        etEmail = findViewById(R.id.email);
        etPass = findViewById(R.id.pass);
        btnLogin = findViewById(R.id.loginptn);
        btnCreate = findViewById(R.id.create);
        btnReset = findViewById(R.id.btnReset);

        auth = FirebaseAuth.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            startActivity(new Intent(this, profile.class));
            finish();
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim();
                String pass = etPass.getText().toString().trim();
                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(log_in.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pass.length() < 6) {
                    Toast.makeText(log_in.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(log_in.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                    return;
                }
                auth.signInWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(log_in.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                    etEmail.setText("");
                                    etPass.setText("");
                                    Intent intent = new Intent(log_in.this, profile.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(log_in.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e("FirebaseAuth", "Login failed", task.getException());
                                }
                            }
                        });
            }
        });
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim();

                if(email.isEmpty()){
                    Toast.makeText(log_in.this, "Enter your email first", Toast.LENGTH_SHORT).show();
                    return;
                }

                auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(log_in.this, "Reset email sent to " + email, Toast.LENGTH_LONG).show();
                                    etEmail.setText("");
                                    etPass.setText("");
                                } else {
                                    Toast.makeText(log_in.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
        btnCreate.setOnClickListener(view -> {
            Intent intent = new Intent(log_in.this, sign_up.class);
            startActivity(intent);
            finish();
        });
    }
}