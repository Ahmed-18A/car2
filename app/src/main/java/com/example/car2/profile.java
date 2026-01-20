package com.example.car2;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class profile extends BaseActivity {

    private static final int REQUEST_GALLERY = 101;
    private static final int REQUEST_CAMERA = 100;
    private Uri cameraImageUri;

    ImageView imageview;
    TextView name, email, phone, password;
    Button btnSignOut, btnEditInfo, btnChangeImage;

    FirebaseAuth auth;
    FirebaseFirestore db;
    Dialog editDialog;

    private static final String IMGBB_API_KEY = "3c6e38b46c0548e23b364cf83954877f";

    BottomNavigationView bottomNav;

    FrameLayout progressOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        applySystemBars();

        progressOverlay = findViewById(R.id.progressOverlay);

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.getMenu().getItem(0).setChecked(false);
        bottomNav.getMenu().setGroupCheckable(0, false, true);

        imageview = findViewById(R.id.profileImage);
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        password = findViewById(R.id.password);

        btnSignOut = findViewById(R.id.btnOut);
        btnEditInfo = findViewById(R.id.btnchinfo);
        btnChangeImage = findViewById(R.id.btnchImg);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        ensureUserDocumentExists();

        loadUserData(user.getUid());

        btnChangeImage.setOnClickListener(v -> showImagePickerDialog());

        bottomNav.setOnItemSelectedListener(item -> {
            if(item.getItemId() == R.id.mnu_dash) {
                startActivity(new Intent(profile.this, dashboard.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_add) {
                startActivity(new Intent(profile.this, addCar.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_myC) {
                startActivity(new Intent(profile.this, MyCars.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_chats) {
                startActivity(new Intent(profile.this, ChatsActivity.class));
                finish();
            }
            return true;
        });

        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(profile.this, log_in.class));
            finish();
        });

        btnEditInfo.setOnClickListener(v -> showEditDialog());
    }

    // ===================== PERMISSION =====================
    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    REQUEST_GALLERY);
        } else {
            openGallery();
        }
    }

    private void checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_GALLERY && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else if (requestCode == REQUEST_CAMERA && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================== ACTIVITY RESULT =====================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_GALLERY && data != null) {
            uploadToImgBB(data.getData());
        }

        if (requestCode == REQUEST_CAMERA) {
            uploadToImgBB(cameraImageUri);
        }
    }

    // ===================== IMAGE PICKER =====================
    private void showImagePickerDialog() {
        String[] options = {"Camera","Album","remove image"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkPermissionAndOpenCamera();
                    if (which == 1) checkPermissionAndOpenGallery();
                    if (which == 2){
                        imageview.setImageResource(R.drawable.user2);
                        Bitmap bitmap = Utils.drawableToBitmap(this, R.drawable.user2);
                        uploadDrawableBitmapToImgBB(bitmap);
                    }
                })
                .show();
    }

    private void uploadDrawableBitmapToImgBB(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

            String encodedImage = android.util.Base64.encodeToString(
                    baos.toByteArray(),
                    android.util.Base64.NO_WRAP
            );

            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("key", IMGBB_API_KEY)
                    .add("image", encodedImage)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    runOnUiThread(() -> Toast.makeText(profile.this, "Upload failed", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        String res = response.body().string();
                        String imageUrl = Utils.parseImgBBUrl(res);

                        runOnUiThread(() -> {
                            Glide.with(profile.this)
                                    .load(imageUrl)
                                    .into(imageview);

                            saveImageUrl(imageUrl);
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(profile.this, "Upload error", Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Image error", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "profile_image");
        cameraImageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    // ===================== UPLOAD =====================
    private void uploadToImgBB(Uri imageUri) {
        progressOverlay.setVisibility(View.VISIBLE);
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            bitmap = Utils.rotateImageIfRequired(this, bitmap, imageUri);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

            String encodedImage = android.util.Base64.encodeToString(
                    baos.toByteArray(),
                    android.util.Base64.NO_WRAP
            );

            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("key", IMGBB_API_KEY)
                    .add("image", encodedImage)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    runOnUiThread(() -> Toast.makeText(profile.this, "Upload failed", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        String res = response.body().string();
                        String imageUrl = Utils.parseImgBBUrl(res);

                        runOnUiThread(() -> {
                            Glide.with(profile.this)
                                    .load(imageUrl)
                                    .into(imageview);

                            saveImageUrl(imageUrl);
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(profile.this, "Upload error", Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Image error", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================== FIRESTORE =====================
    private void saveImageUrl(String url) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            progressOverlay.setVisibility(View.GONE);
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("profileImage", url);
        db.collection("users")
                .document(user.getUid())
                .set(map, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(profile.this, "تم حفظ الصورة", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(profile.this, "فشل الحفظ", Toast.LENGTH_SHORT).show();
                });
    }

    private void ensureUserDocumentExists() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", user.getDisplayName());
                        map.put("email", user.getEmail());
                        map.put("phone", "");
                        map.put("profileImage", "");

                        db.collection("users")
                                .document(user.getUid())
                                .set(map);
                    }
                });
    }


    private void loadUserData(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {

                    Log.d("PROFILE", "DOC EXISTS = " + doc.exists());
                    Log.d("PROFILE", "DATA = " + doc.getData());

                    if (!doc.exists()) return;

                    name.setText(doc.getString("name"));
                    email.setText(doc.getString("email"));
                    phone.setText(doc.getString("phone"));
                    password.setText("******");

                    String img = doc.getString("profileImage");
                    if (img != null && !img.isEmpty()) {
                        Glide.with(this).load(img).into(imageview);
                    }
                });
    }

    // ===================== EDIT DIALOG =====================
    private void showEditDialog() {
        editDialog = new Dialog(this);
        editDialog.setContentView(R.layout.dialog);

        EditText editName = editDialog.findViewById(R.id.editName);
        EditText editPhone = editDialog.findViewById(R.id.editPhone);
        Button btnSave = editDialog.findViewById(R.id.btnSave);
        Button btnCancel = editDialog.findViewById(R.id.btnCancel);
        Button btnReset = editDialog.findViewById(R.id.btnReset2);

        editName.setText(name.getText());
        editPhone.setText(phone.getText());

        btnReset.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) return;

            auth.sendPasswordResetEmail(user.getEmail()).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    Toast.makeText(profile.this, "Reset email sent to " + user.getEmail(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(profile.this, "Failed to send email", Toast.LENGTH_LONG).show();
                }
            });
        });

        btnCancel.setOnClickListener(v -> editDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) return;

            db.collection("users")
                    .document(user.getUid())
                    .update(
                            "name", editName.getText().toString(),
                            "phone", editPhone.getText().toString()
                    );

            name.setText(editName.getText());
            phone.setText(editPhone.getText());
            editDialog.dismiss();
        });

        int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        editDialog.getWindow().setLayout(w, WindowManager.LayoutParams.WRAP_CONTENT);
        editDialog.show();
    }
}
