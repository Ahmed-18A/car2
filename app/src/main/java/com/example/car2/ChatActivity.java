package com.example.car2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends BaseActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private ImageView ivLocation;

    private FirebaseFirestore db;
    private DocumentReference chatDocRef;

    private String myId;
    private String sellerId;
    private String chatId;

    private MessagesAdapter messagesAdapter;
    private final ArrayList<Message> messages = new ArrayList<>();
    private ListenerRegistration messagesListener;

    // (اختياري) هيدر
    private ImageView imgUser;
    private TextView txtName;

    // Location
    private static final int REQ_LOCATION = 500;
    private FusedLocationProviderClient fusedClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        applySystemBars();

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.ImageButton);
        ivLocation = findViewById(R.id.ivLocation);

        db = FirebaseFirestore.getInstance();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        myId = FirebaseAuth.getInstance().getUid();
        sellerId = getIntent().getStringExtra("sellerId");

        if (myId == null || sellerId == null) {
            Toast.makeText(this, "Missing user data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (myId.equals(sellerId)) {
            Toast.makeText(this, "ما بصير تحكي مع حالك", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(ChatActivity.this, ChatsActivity.class));
            finish();
        });

        // زر الموقع
        ivLocation.setOnClickListener(v -> sendMyLocation());

        chatId = makeChatId(myId, sellerId);
        chatDocRef = db.collection("chats").document(chatId);

        // ===== RecyclerView =====
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);

        messagesAdapter = new MessagesAdapter(messages, myId);
        rvMessages.setAdapter(messagesAdapter);

        // ===== (اختياري) هيدر =====
        try {
            imgUser = findViewById(R.id.imgUser);
            txtName = findViewById(R.id.txtName);
            loadHeaderUser();
        } catch (Exception ignored) { }

        // ✅ تأكد الشات موجود وبعدين اسمع الرسائل
        ensureChatExists(() -> {
            startMessagesListener();
            btnSend.setEnabled(true);
        });

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private String makeChatId(String a, String b) {
        return (a.compareTo(b) < 0) ? a + "_" + b : b + "_" + a;
    }

    private void ensureChatExists(Runnable onReady) {
        chatDocRef.get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        onReady.run();
                        return;
                    }

                    Map<String, Object> chat = new HashMap<>();
                    chat.put("users", Arrays.asList(myId, sellerId));
                    chat.put("lastMessage", "");
                    chat.put("lastMessageTime", FieldValue.serverTimestamp());

                    chatDocRef.set(chat)
                            .addOnSuccessListener(v -> onReady.run())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "فشل إنشاء الشات: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "فشل قراءة الشات: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void startMessagesListener() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }

        messagesListener = chatDocRef.collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        android.util.Log.e("CHAT_LISTEN", "listen failed", e);
                        return;
                    }

                    messages.clear();
                    for (var doc : snap.getDocuments()) {
                        try {
                            Message m = doc.toObject(Message.class);
                            if (m != null) {
                                messages.add(m);
                            }
                        } catch (Exception ex) {
                            android.util.Log.e("CHAT_PARSE", "Bad message doc: " + doc.getId(), ex);
                        }
                    }

                    messagesAdapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) rvMessages.scrollToPosition(messages.size() - 1);
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", myId);
        msg.put("text", text);
        msg.put("timestamp", FieldValue.serverTimestamp());

        chatDocRef.collection("messages")
                .add(msg)
                .addOnSuccessListener(r -> {
                    etMessage.setText("");

                    Map<String, Object> update = new HashMap<>();
                    update.put("lastMessage", text);
                    update.put("lastMessageTime", FieldValue.serverTimestamp());
                    chatDocRef.update(update);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CHAT_SEND", "Send failed", e);
                    Toast.makeText(this, "فشل إرسال الرسالة (شوف Logcat)", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION
            );
            return;
        }

        fusedClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Toast.makeText(this, "شغّل GPS وجرب مرة ثانية", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    String mapLink = "https://maps.google.com/?q=" + lat + "," + lng;

                    // نرسل الرابط كنص
                    etMessage.setText(mapLink);
                    sendMessage();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Location error", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendMyLocation();
            } else {
                Toast.makeText(this, "لازم تسمح بالموقع عشان تبعت موقعك", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ===== (اختياري) Header data =====
    private void loadHeaderUser() {
        db.collection("users").document(sellerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (txtName != null) {
                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) name = "User";
                        txtName.setText(name);
                    }

                    if (imgUser != null) {
                        String img = doc.getString("profileImage");
                        if (img != null && !img.isEmpty()) {
                            Glide.with(this).load(img).circleCrop().into(imgUser);
                        } else {
                            imgUser.setImageResource(R.drawable.user2);
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }
}
