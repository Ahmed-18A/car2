package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.Calendar;

public class payment extends AppCompatActivity {

    public static final String EXTRA_PRICE = "extra_price";
    public static final String EXTRA_FEE = "extra_fee";

    private EditText etCardNumber, etMM, etYY, etCVC;
    private Spinner spCountry;
    private Button btnClose, btnPay;

    private double carPrice = 0.0;
    private double fee = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        btnClose = findViewById(R.id.btnClose);
        btnPay = findViewById(R.id.btnPay);

        etCardNumber = findViewById(R.id.etCardNumber);
        etMM = findViewById(R.id.etMM);
        etYY = findViewById(R.id.etYY);
        etCVC = findViewById(R.id.etCVC);

        spCountry = findViewById(R.id.spCountry);

        carPrice = getIntent().getDoubleExtra(EXTRA_PRICE, 0.0);
        fee = carPrice * 0.035;

        DecimalFormat df = new DecimalFormat("0.00");
        btnPay.setText("Pay " + df.format(fee)+"$");

        btnClose.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnPay.setOnClickListener(v -> {
            if (!isVisaInfoValid()) return;

            Intent data = new Intent();
            data.putExtra(EXTRA_FEE, fee);
            setResult(RESULT_OK, data);
            finish();
        });

        etCardNumber.addTextChangedListener(new TextWatcher() {

            private boolean isUpdating;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;

                isUpdating = true;

                // إحذف الفراغات
                String digits = s.toString().replace(" ", "");

                // حد أقصى 16 رقم
                if (digits.length() > 16) {
                    digits = digits.substring(0, 16);
                }

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digits.charAt(i));
                }

                etCardNumber.setText(formatted.toString());
                etCardNumber.setSelection(formatted.length());

                isUpdating = false;
            }
        });

    }

    private boolean isVisaInfoValid() {

        String card = etCardNumber.getText().toString().replace(" ", "").trim();
        String mmStr = etMM.getText().toString().trim();
        String yyStr = etYY.getText().toString().trim();
        String cvc = etCVC.getText().toString().trim();

        if (card.length() != 16 || !TextUtils.isDigitsOnly(card)) {
            Toast.makeText(this, "Card number must be 16 digits", Toast.LENGTH_SHORT).show();
            return false;
        }

        Calendar cal = Calendar.getInstance();
        int currentYearYY = cal.get(Calendar.YEAR) % 100;
        int currentMonth = cal.get(Calendar.MONTH) + 1;

        if (TextUtils.isEmpty(mmStr)) {
            Toast.makeText(this, "MM is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(yyStr)) {
            Toast.makeText(this, "YY is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        int yy = Integer.parseInt(yyStr);
        if (yy < currentYearYY) {
            Toast.makeText(this, "Card expired (year)", Toast.LENGTH_SHORT).show();
            return false;
        }

        int mm = Integer.parseInt(mmStr);
        if (mm < 1 || mm > 12) {
            Toast.makeText(this, "MM must be between 01 and 12", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (yy == currentYearYY && mm < currentMonth) {
            Toast.makeText(this, "Card expired (month)", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cvc.length() != 3 || !TextUtils.isDigitsOnly(cvc)) {
            Toast.makeText(this, "CVC must be 3 digits", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
