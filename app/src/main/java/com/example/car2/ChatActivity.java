package com.example.car2;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends BaseActivity {

    public static volatile String OPEN_CHAT_ID = null;

    private ImageView btnCall;
    private String otherUserPhone;

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private ImageView btnLocation;

    private FirebaseFirestore db;
    private DocumentReference chatDocRef;

    private String myId;
    private String sellerId;
    private String chatId;

    private MessagesAdapter messagesAdapter;
    private final ArrayList<Message> messages = new ArrayList<>();
    private final ArrayList<String> messageIds = new ArrayList<>();
    private ListenerRegistration messagesListener;

    private ImageView imgUser;
    private TextView txtName;

    private static final int REQ_LOCATION = 500;
    private static final int REQ_GPS = 1001;

    private FusedLocationProviderClient fusedClient;

    private Timestamp myLastReadBeforeOpen = null;
    private boolean firstSnapshotHandled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        applySystemBars();

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.ImageButton);
        btnLocation = findViewById(R.id.ivLocation);
        btnCall = findViewById(R.id.ivCall);

        db = FirebaseFirestore.getInstance();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        myId = FirebaseAuth.getInstance().getUid();
        sellerId = getIntent().getStringExtra("sellerId");

        if (myId == null || sellerId == null) {
            Toast.makeText(this, "Missing user data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(ChatActivity.this, ChatsActivity.class));
            finish();
        });

        btnLocation.setOnClickListener(v -> showSendLocationDialog());
        btnCall.setOnClickListener(v -> openDialerWithOtherUserPhone());

        chatId = makeChatId(myId, sellerId);
        chatDocRef = db.collection("chats").document(chatId);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);

        messagesAdapter = new MessagesAdapter(messages, myId);
        rvMessages.setAdapter(messagesAdapter);

        try {
            imgUser = findViewById(R.id.imgUser);
            txtName = findViewById(R.id.txtName);
            loadHeaderUser();
        } catch (Exception ignored) {}

        btnSend.setEnabled(true);

        chatDocRef.get().addOnSuccessListener(doc -> {
            if (doc != null && doc.exists()) {
                Object lrObj = doc.get("lastRead");
                if (lrObj instanceof Map) {
                    Map<String, Object> lr = (Map<String, Object>) lrObj;
                    Object my = lr.get(myId);
                    if (my instanceof Timestamp) {
                        myLastReadBeforeOpen = (Timestamp) my;
                    }
                }
            }
        });

        startMessagesListenerRealtime();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        OPEN_CHAT_ID = chatId;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chatId != null && chatId.equals(OPEN_CHAT_ID)) OPEN_CHAT_ID = null;
    }

    private String makeChatId(String a, String b) {
        return (a.compareTo(b) < 0) ? a + "_" + b : b + "_" + a;
    }

    private void startMessagesListenerRealtime() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }

        messages.clear();
        messageIds.clear();
        messagesAdapter.notifyDataSetChanged();
        firstSnapshotHandled = false;

        LinearLayoutManager lm = (LinearLayoutManager) rvMessages.getLayoutManager();

        messagesListener = chatDocRef.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        android.util.Log.e("CHAT_LISTEN", "listen failed", e);
                        return;
                    }
                    if (snap == null) return;

                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        String id = dc.getDocument().getId();

                        try {
                            Message m = dc.getDocument().toObject(Message.class);

                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                if (messageIds.contains(id)) continue;

                                if (m != null) {
                                    messages.add(m);
                                    messageIds.add(id);
                                    messagesAdapter.notifyItemInserted(messages.size() - 1);
                                }

                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                int idx = messageIds.indexOf(id);
                                if (m != null) {
                                    if (idx != -1) {
                                        messages.set(idx, m);
                                        messagesAdapter.notifyItemChanged(idx);
                                    } else {
                                        messages.add(m);
                                        messageIds.add(id);
                                        messagesAdapter.notifyItemInserted(messages.size() - 1);
                                    }
                                }

                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                                int idx = messageIds.indexOf(id);
                                if (idx != -1) {
                                    messageIds.remove(idx);
                                    messages.remove(idx);
                                    messagesAdapter.notifyItemRemoved(idx);
                                }
                            }

                        } catch (Exception ex) {
                            android.util.Log.e("CHAT_PARSE", "Bad message doc: " + id, ex);
                        }
                    }

                    if (!firstSnapshotHandled) {
                        firstSnapshotHandled = true;

                        int targetPos = -1;

                        if (myLastReadBeforeOpen != null) {
                            for (int i = 0; i < messages.size(); i++) {
                                Timestamp t = messages.get(i).getTimestamp();
                                if (t != null && t.compareTo(myLastReadBeforeOpen) > 0) {
                                    targetPos = i;
                                    break;
                                }
                            }
                        }

                        if (targetPos == -1 && !messages.isEmpty()) {
                            targetPos = messages.size() - 1;
                        }

                        if (lm != null) lm.setStackFromEnd(true);

                        if (targetPos != -1) {
                            rvMessages.scrollToPosition(targetPos);
                        }

                        markChatAsRead();
                    } else {
                        if (!messages.isEmpty()) rvMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void markChatAsRead() {
        Map<String, Object> update = new HashMap<>();
        update.put("lastRead." + myId, Timestamp.now());
        chatDocRef.set(update, SetOptions.merge());
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Timestamp now = Timestamp.now();

        DocumentReference msgRef = chatDocRef.collection("messages").document();

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", myId);
        msg.put("text", text);
        msg.put("timestamp", now);

        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("users", Arrays.asList(myId, sellerId));
        chatUpdate.put("lastMessage", text);
        chatUpdate.put("lastMessageTime", now);
        chatUpdate.put("lastSenderId", myId);
        chatUpdate.put("lastRead." + myId, now);

        etMessage.setText("");

        db.runBatch(batch -> {
            batch.set(chatDocRef, chatUpdate, SetOptions.merge());
            batch.set(msgRef, msg);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "فشل إرسال الرسالة: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                    if (location != null) {
                        // لو فيه موقع مباشرة
                        sendLocationMessage(location.getLatitude(), location.getLongitude());
                    } else {
                        // لو GPS مطفي أو ما فيه fix، نطلب من المستخدم تشغيله
                        LocationRequest request = LocationRequest.create()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setInterval(1000)
                                .setFastestInterval(500)
                                .setNumUpdates(1);

                        LocationSettingsRequest.Builder builder =
                                new LocationSettingsRequest.Builder().addLocationRequest(request);

                        SettingsClient client = LocationServices.getSettingsClient(this);
                        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

                        task.addOnSuccessListener(response -> {
                            // GPS شغال، نطلب الموقع مباشرة
                            fusedClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener(loc -> {
                                        if (loc != null) sendLocationMessage(loc.getLatitude(), loc.getLongitude());
                                        else Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                                    });
                        });

                        task.addOnFailureListener(e -> {
                            if (e instanceof ResolvableApiException) {
                                try {
                                    ((ResolvableApiException) e).startResolutionForResult(ChatActivity.this, REQ_GPS);
                                } catch (IntentSender.SendIntentException ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                Toast.makeText(this, "Cannot access location", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Location error", Toast.LENGTH_SHORT).show()
                );
    }

    private void sendLocationMessage(double lat, double lng) {
        String mapLink = "https://maps.google.com/?q=" + lat + "," + lng;
        etMessage.setText(mapLink);
        sendMessage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_GPS) {
            if (resultCode == RESULT_OK) {
                sendMyLocation();
            } else {
                Toast.makeText(this, "Permission error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendMyLocation();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    private void loadHeaderUser() {
        db.collection("users").document(sellerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (txtName != null) {
                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) name = "User";
                        txtName.setText(name);
                    }

                    otherUserPhone = doc.getString("phone");

                    if (imgUser != null) {
                        String img = doc.getString("profileImage");
                        if (img != null && !img.isEmpty()) {
                            Glide.with(this)
                                    .load(img)
                                    .circleCrop()
                                    .override(300, 300)
                                    .into(imgUser);
                        } else {
                            imgUser.setImageResource(R.drawable.user2);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("CHAT_HEADER", "loadHeaderUser failed", e)
                );
    }

    private void openDialerWithOtherUserPhone() {
        if (otherUserPhone == null || otherUserPhone.trim().isEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(android.net.Uri.parse("tel:" + otherUserPhone.trim()));
        startActivity(intent);
    }

    private void showSendLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Send location")
                .setMessage("Do you want to send your current location?")
                .setPositiveButton("Send", (dialog, which) -> sendMyLocation())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}