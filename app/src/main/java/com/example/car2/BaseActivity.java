package com.example.car2;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void applySystemBars() {

        Window window = getWindow();

        // ✅ هذا مهم عشان لون الستاتوس يشتغل على كل الأجهزة
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // ✅ Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // ✅ لون الستاتوس بار (غيّر الاسم للونك الحقيقي)
        int statusColor = ContextCompat.getColor(this, R.color.my_status_bar);
        window.setStatusBarColor(statusColor);

        // (اختياري) خلي النفيجيشن بار شفاف لأنه مخفي أصلاً
        window.setNavigationBarColor(Color.TRANSPARENT);

        // ✅ تحكم بالـ bars
        View decor = window.getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, decor);

        // نخفي الأزرار اللي تحت فقط
        controller.hide(WindowInsetsCompat.Type.navigationBars());

        // نخلي الستاتوس بار ظاهر
        controller.show(WindowInsetsCompat.Type.statusBars());

        // يظهر بالسحب
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        controller.setAppearanceLightStatusBars(true);

        // ✅ أهم نقطة: البادينج على محتوى الصفحة مش على decorView

        View content = findViewById(android.R.id.content);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {

                // ✅ فوق: ستاتوس بار
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;

                // ✅ تحت: الكيبورد فقط (IME) — مش النافيجيشن بار
                int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

                // لا تترك فراغ تحت إلا إذا الكيبورد طالع
                v.setPadding(0, top, 0, imeBottom);

                return insets;
            });

            ViewCompat.requestApplyInsets(content);
        }
        View bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                // شيل أي padding تحت (حتى لو Material ضافته)
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 0);

                // استهلك الـ insets عشان ما يضيفها مرة ثانية
                return WindowInsetsCompat.CONSUMED;
            });

            ViewCompat.requestApplyInsets(bottomNav);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applySystemBars();
    }
}
