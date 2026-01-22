package com.example.car2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.car2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class sign_up extends BaseActivity {
    FrameLayout progressOverlay;
    EditText etEmail, etPass, etName, etPhone;
    Button btnSignup,btnInpage;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        applySystemBars();

        getWindow().setStatusBarColor(Color.parseColor("#dbf2ff"));

        progressOverlay = findViewById(R.id.progressOverlay);
        etEmail = findViewById(R.id.etEmail);
        etPass = findViewById(R.id.etPass);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        btnInpage = findViewById(R.id.inpage);
        btnSignup = findViewById(R.id.signupbtn);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();
                progressOverlay.setVisibility(View.VISIBLE);
                String email = etEmail.getText().toString().trim();
                String password = etPass.getText().toString().trim();
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();

                if(email.isEmpty() || password.isEmpty() || name.isEmpty() || phone.isEmpty()){
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(sign_up.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(password.length() < 6){
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(sign_up.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(phone.length() != 10){
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(sign_up.this, "Phone number must be 10 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(sign_up.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                    return;
                }

                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(sign_up.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()){
                                    String uid = auth.getCurrentUser().getUid();
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("name", name);
                                    userMap.put("phone", phone);
                                    userMap.put("email", email);

                                    db.collection("users").document(uid)
                                            .set(userMap)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(sign_up.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                                etEmail.setText("");
                                                etName.setText("");
                                                etPass.setText("");
                                                etPhone.setText("");
                                                Intent intent = new Intent(sign_up.this, log_in.class);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                progressOverlay.setVisibility(View.GONE);
                                                Toast.makeText(sign_up.this, "Account created but failed to save profile. Try again.", Toast.LENGTH_LONG).show();
                                            });
                                }
                                else {
                                    progressOverlay.setVisibility(View.GONE);

                                    Exception ex = task.getException();
                                    String msg = "Sign up failed";

                                    if (ex instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                        msg = "This email is already registered";
                                    } else if (ex instanceof com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
                                        msg = "Password is too weak";
                                    } else if (ex instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                                        msg = "Invalid email format";
                                    } else if (ex instanceof com.google.firebase.FirebaseNetworkException) {
                                        msg = "No internet connection";
                                    } else if (ex != null && ex.getMessage() != null && !ex.getMessage().trim().isEmpty()) {
                                        msg = ex.getMessage();
                                    }

                                    Toast.makeText(sign_up.this, msg, Toast.LENGTH_LONG).show();
                                }
                            }
                        });

            }
        });
        btnInpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(sign_up.this, log_in.class);
                startActivity(intent);
                finish();
            }
        });

    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}