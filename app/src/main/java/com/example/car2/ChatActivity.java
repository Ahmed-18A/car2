// ======================= ChatActivity.java =======================
package com.example.car2;

import android.Manifest;
import android.content.Intent;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends BaseActivity {

    // ✅ لمنع نوتيفيكيشن إذا أنت داخل نفس الشات
    public static volatile String OPEN_CHAT_ID = null;

    private ImageView ivCall;
    private String otherUserPhone;

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
    private final ArrayList<String> messageIds = new ArrayList<>();
    private ListenerRegistration messagesListener;

    // Header (اختياري)
    private ImageView imgUser;
    private TextView txtName;

    // Location
    private static final int REQ_LOCATION = 500;
    private FusedLocationProviderClient fusedClient;

    // ✅ unread logic
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
        ivLocation = findViewById(R.id.ivLocation);
        ivCall = findViewById(R.id.ivCall);

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

        ivLocation.setOnClickListener(v -> showSendLocationDialog());
        ivCall.setOnClickListener(v -> openDialerWithOtherUserPhone());

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
        } catch (Exception ignored) {}

        btnSend.setEnabled(true);

        // ✅ اقرأ lastRead قبل فتح الرسائل
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

        // ✅ ابدأ listener مباشرة
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

    /**
     * ✅ Listener محسّن + يوقف عند أول رسالة جديدة
     */
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

                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            if (messageIds.contains(id)) continue;

                            try {
                                Message m = dc.getDocument().toObject(Message.class);
                                if (m != null) {
                                    messages.add(m);
                                    messageIds.add(id);

                                    messagesAdapter.notifyItemInserted(messages.size() - 1);
                                }
                            } catch (Exception ex) {
                                android.util.Log.e("CHAT_PARSE", "Bad message doc: " + id, ex);
                            }
                        }
                    }

                    // ✅ أول مرة فقط: روح لأول رسالة جديدة
                    if (!firstSnapshotHandled) {
                        firstSnapshotHandled = true;

                        int firstUnreadPos = -1;

                        if (myLastReadBeforeOpen != null) {
                            for (int i = 0; i < messages.size(); i++) {
                                Timestamp t = messages.get(i).getTimestamp();
                                if (t != null && t.compareTo(myLastReadBeforeOpen) > 0) {
                                    firstUnreadPos = i;
                                    break;
                                }
                            }
                        } else {
                            if (!messages.isEmpty()) firstUnreadPos = 0;
                        }

                        if (firstUnreadPos != -1) {
                            if (lm != null) lm.setStackFromEnd(false);
                            rvMessages.scrollToPosition(firstUnreadPos);
                        } else {
                            if (lm != null) lm.setStackFromEnd(true);
                            if (!messages.isEmpty()) rvMessages.scrollToPosition(messages.size() - 1);
                        }

                        // ✅ بعد ما نوقف بالمكان الصحيح: علّم الشات مقروء
                        markChatAsRead();

                    } else {
                        // بعدها: إذا رسائل جديدة وأنت فاتح الشات، نزل لآخر
                        if (!messages.isEmpty()) rvMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void markChatAsRead() {
        Map<String, Object> update = new HashMap<>();
        update.put("lastRead." + myId, Timestamp.now());
        chatDocRef.set(update, SetOptions.merge());
    }

    /**
     * ✅ الإرسال: يحدث وثيقة الشات + lastRead للمرسل + lastSenderId
     */
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Timestamp now = Timestamp.now();

        Map<String, Object> chat = new HashMap<>();
        chat.put("users", Arrays.asList(myId, sellerId));
        chat.put("lastMessage", text);
        chat.put("lastMessageTime", now);
        chat.put("lastSenderId", myId);

        // ✅ المرسل قرأ لحد الآن
        Map<String, Object> lastReadMap = new HashMap<>();
        lastReadMap.put(myId, now);
        chat.put("lastRead", lastReadMap);

        chatDocRef.set(chat, SetOptions.merge())
                .addOnSuccessListener(v -> {

                    Map<String, Object> msg = new HashMap<>();
                    msg.put("senderId", myId);
                    msg.put("text", text);
                    msg.put("timestamp", now);
                    msg.put("serverTime", FieldValue.serverTimestamp());

                    chatDocRef.collection("messages")
                            .add(msg)
                            .addOnSuccessListener(r -> etMessage.setText(""))
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "فشل إرسال الرسالة", Toast.LENGTH_LONG).show()
                            );

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "فشل إنشاء الشات", Toast.LENGTH_LONG).show()
                );
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
