package net.net23.httpbustracker.bustracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

public class TicketsActivity extends AppCompatActivity {
    SharedPreferences Data;
    SharedPreferences.Editor editor;
    Button checkbalance, createticket;
    int offlineBalance;



// bvg
//     flix bus
//    europe bus

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tickets);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Data = getSharedPreferences("MYDATA", Context.MODE_PRIVATE);
        editor = Data.edit();
        String strCredit = Data.getString("Credit", "Error getting credit");
        int credit = Integer.valueOf(strCredit);

        if (credit <= 6 && credit >= 2) {
            AlertDialog.Builder builder = new AlertDialog.Builder(TicketsActivity.this);
            builder.setPositiveButton("OK", null);
            builder.setTitle("Attention!");
            builder.setMessage("You are running out of credit, please top-up, thank you");
            builder.show();
        } else if (credit == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(TicketsActivity.this);
            builder.setPositiveButton("OK", null);
            builder.setTitle("Attention!");
            builder.setMessage("You credit balance is 0 SDG, you must buy credit as soon as possible!");
            builder.show();
        }

        createticket = (Button) findViewById(R.id.create_ticket);
        createticket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String strBalance = Data.getString("Credit", "Error getting credit");
                int intBalance;
                intBalance = Integer.parseInt(strBalance);
                offlineBalance = intBalance;

                if (intBalance >= 2) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(TicketsActivity.this);
                    alert.setTitle("Pin");
                    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        //Online MODE
                        alert.setMessage("Enter Your PIN code");
                    } else {
                        //Offline MODE
                        int TicketsNO = offlineBalance / 2;
                        alert.setMessage("Offline tickets left: " + TicketsNO + "\nEnter Your PIN code");
                    }
                    // Set up the input (ALERT DIALOG WITH INPUT)
                    final EditText input = new EditText(TicketsActivity.this);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                    alert.setView(input);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String pin = input.getText().toString();
                            String hashedpin = md5(pin);
                            String savedPin = Data.getString("Pin", "Error with the pin");
                            if (hashedpin.equals(savedPin)) {
                                offlineBalance = offlineBalance - 2;
                                ////////////////////////////////////////////////////////////////
                                //Access Token Creator
                                Calendar calendar = Calendar.getInstance();
                                String min = String.valueOf(calendar.get(Calendar.MINUTE));
                                String hour = String.valueOf(calendar.get(Calendar.HOUR));
                                String day = String.valueOf(calendar.get(Calendar.DAY_OF_WEEK));
                                String month = String.valueOf(calendar.get(Calendar.MONTH));
                                String year = String.valueOf(calendar.get(Calendar.YEAR));
                                String hashedDMY = md5(day + month + year);
                                ////////////////////////////////////////////////////////////////
                                ////////////////////////////////////////////////////////////////////////////////////////
                                //GET ALL DATA REQUIRED INCLUDING ACCESS TOKEN AND SAVE THEM IN ON VARIABLE FOR QR CODE ENCODING
                                String Name = Data.getString("Name", "Error");
                                String id = Data.getString("UserID", "ERROR acquiring your ID");
                                editor.putString("Info", Name.trim() + ":" + id + ":" + min + ":" + hour + ":" + hashedDMY).commit();
                                ////////////////////////////////////////////////////////////////////////////////////////
                                Intent intent = new Intent(TicketsActivity.this, QRCode.class);
                                startActivity(intent);
                                finish();
                            } else {
                                AlertDialog.Builder dialog = new AlertDialog.Builder(TicketsActivity.this);
                                dialog.setTitle("Error!");
                                dialog.setMessage("Incorrect PIN,please try again");
                                dialog.setPositiveButton("Ok", null);
                                dialog.show();
                            }
                        }
                    });
                    alert.show();

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TicketsActivity.this);
                    builder.setPositiveButton("OK", null);
                    builder.setTitle("Error!");
                    builder.setMessage("You don't have the minimum credit (2 SDG) to create a ticket");
                    builder.show();
                }
            }
        });

        checkbalance = (Button) findViewById(R.id.check_my_balance);
        checkbalance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    String id = Data.getString("UserID", "Error getting ID");
                    String method = "updateC";
                    String request = "foreground";
                    BackgroundCredit backgroundCredit = new BackgroundCredit(TicketsActivity.this);
                    backgroundCredit.execute(method, id, request);
                } else {
                    String credit = Data.getString("Credit", "Error getting credit");

                    AlertDialog.Builder builder = new AlertDialog.Builder(TicketsActivity.this);
                    builder.setPositiveButton("OK", null);
                    builder.setTitle("Your Balance");
                    builder.setMessage("You are currently offline,\nyour balance is " + credit + " SDG, " +
                            "reconnect to the the internet to get your updated balance");
                    builder.show();
                }
            }
        });
    }

    public String md5(String toEncrypt) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(toEncrypt.getBytes());
            byte[] a = digest.digest();
            int len = a.length;
            StringBuilder sb = new StringBuilder(len << 1);
            for (int i = 0; i < len; i++) {
                sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
                sb.append(Character.forDigit(a[i] & 0x0f, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

}

