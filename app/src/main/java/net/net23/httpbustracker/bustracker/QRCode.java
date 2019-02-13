package net.net23.httpbustracker.bustracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRCode extends Activity {
    String text2Qr;
    ImageView QRCODE;
    TextView tvtime;
    Button back;
    CounterClass timer;
    SharedPreferences Data;
    SharedPreferences.Editor editor;
    @Override
    public void onBackPressed() {
        startActivity(new Intent(this,TicketsActivity.class));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        QRCODE = (ImageView) findViewById(R.id.qr_code);
        tvtime = (TextView) findViewById(R.id.time_text_view);
        back = (Button) findViewById(R.id.back_button);


        Data = getSharedPreferences("MYDATA", Context.MODE_PRIVATE);
        editor = Data.edit();
        String info = Data.getString("Info", "Error getting info");
        text2Qr = info.trim();
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(text2Qr, BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            QRCODE.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        timer = new CounterClass(30000,1000);
        timer.start();
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer.cancel();
                Intent Intent = new Intent(QRCode.this, TicketsActivity.class);
                startActivity(Intent);
                finish();
            }
        });
    }
    public  class  CounterClass extends CountDownTimer {
        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {
            tvtime.setText("Ticket timeout in: " + (millisUntilFinished/1000));
        }
        @Override
        public void onFinish() {
            Intent Intent = new Intent(QRCode.this, TicketsActivity.class);
            startActivity(Intent);
            finish();
        }
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }
}
