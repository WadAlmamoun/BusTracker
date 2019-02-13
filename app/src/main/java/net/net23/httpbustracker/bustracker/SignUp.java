package net.net23.httpbustracker.bustracker;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SignUp extends Activity {
    EditText etUsername, etFname, etLname, etPassword, etConfirmPassword,etpin,etcon_pin;
    Button btnSignup;

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this,LoginActivity.class));
        finish();
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        btnSignup =(Button)findViewById(R.id.sign_up_button);
        etUsername = (EditText) findViewById(R.id.new_username);
        etFname = (EditText) findViewById(R.id.fname);
        etLname = (EditText)findViewById(R.id.lname);
        etPassword = (EditText) findViewById(R.id.new_password);
        etConfirmPassword = (EditText) findViewById(R.id.confirm_password);
        etpin = (EditText) findViewById(R.id.new_pin);
        etcon_pin = (EditText) findViewById(R.id.confirm_pin);

        etPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(etPassword.getText().length()<6)
                {
                    etPassword.setError("Password must be at least 6 characters.");
                    btnSignup.setEnabled(false);
                }
                else
                {
                    btnSignup.setEnabled(true);
                }
            }
        });
        etpin.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(etpin.getText().length()<4)
                {
                    etpin.setError("Pin must be a 4 digit number.");
                    btnSignup.setEnabled(false);
                }
                else {
                    btnSignup.setEnabled(true);
                }
            }
        });

    }

    public void sign_up_btn(View v) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
        {
            String username = etUsername.getText().toString();
            String fname = etFname.getText().toString();
            String lname = etLname.getText().toString();
            String name = etFname.getText().toString() + " " + etLname.getText().toString();
            String password = etPassword.getText().toString();
            String confirmpassword = etConfirmPassword.getText().toString();
            String pin = etpin.getText().toString();
            String confirmpin = etcon_pin.getText().toString();

            if (username.isEmpty() || fname.isEmpty() || lname.isEmpty() || password.isEmpty() || confirmpassword.isEmpty() || pin.isEmpty() || confirmpin.isEmpty())
            {
                AlertDialog.Builder builder  = new AlertDialog.Builder(this);
                builder.setPositiveButton("OK",null);
                builder.setTitle("Error!");
                builder.setMessage("Please Fill All the Fields!");
                builder.show();
                etPassword.setText("");
                etConfirmPassword.setText("");
                etpin.setText("");
                etcon_pin.setText("");
            }
            else if (!password.equals(confirmpassword)) {
                AlertDialog.Builder builder  = new AlertDialog.Builder(this);
                builder.setPositiveButton("OK",null);
                builder.setTitle("Error!");
                builder.setMessage("Passwords Don't Match");
                builder.show();
                etPassword.setText("");
                etConfirmPassword.setText("");
            }
            else if (!pin.equals(confirmpin))
            {
                AlertDialog.Builder builder  = new AlertDialog.Builder(this);
                builder.setPositiveButton("OK",null);
                builder.setTitle("Error!");
                builder.setMessage("PINs Entered Don't Match");
                builder.show();
                etpin.setText("");
                etcon_pin.setText("");
            }
            else
            {
                String method = "register";
                BackgroundLogReg backgroundLogReg = new BackgroundLogReg(SignUp.this);
                backgroundLogReg.execute(method, name, username, password,pin);
            }
        }
        else
        {
            AlertDialog.Builder builder  = new AlertDialog.Builder(this);
            builder.setPositiveButton("OK",null);
            builder.setTitle("Error!");
            builder.setMessage("You are not Connected to the internet");
            builder.show();
        }
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }
}


