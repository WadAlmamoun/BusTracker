package net.net23.httpbustracker.bustracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by Hamid on 12/5/2016.
 */

public class BackgroundLogReg extends AsyncTask<String,Void,String> {
    SharedPreferences Data;
    SharedPreferences.Editor editor;
  //  String reg_url = "http://bustracker.net23.net/user_register.php";
//    String login_url = "http://bustracker.net23.net/user_login.php";
        String reg_url = "http://bustrackersudan.net16.net/user_register.php";
        String login_url = "http://bustrackersudan.net16.net/user_login.php";
  //  String reg_url = "http://192.168.8.107/user_register.php";
  //  String login_url = "http://192.168.8.107/user_login.php";
    Activity activity;
    Context ctx;
    ProgressDialog progressDialog;
    AlertDialog.Builder builder;
    BackgroundLogReg(Context ctx)
    {
        this.ctx = ctx;
        activity = (Activity) ctx;
    }
    public BackgroundLogReg asyncObject;
    @Override
    protected void onPreExecute()
    {
        builder = new AlertDialog.Builder(activity);
        progressDialog = new ProgressDialog(ctx);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Connecting to server...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
        asyncObject = this;
        new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {}
            public void onFinish() {
                // stop async task if not in progress
                if (asyncObject.getStatus() == AsyncTask.Status.RUNNING) {
                    progressDialog.dismiss();
                    asyncObject.cancel(false);
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setPositiveButton("OK", null);
                    builder.setTitle("Error!");
                    builder.setMessage("Slow Internet connection, please try again");
                    builder.show();
                }
            }
        }.start();
    }

    @Override
    protected String doInBackground(String... params) {
        Data = ctx.getSharedPreferences("MYDATA", Context.MODE_PRIVATE);
        editor = Data.edit();

        String method = params[0];
        if (method.equals("register")) {
            try {
                URL url = new URL(reg_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream OS = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(OS, "UTF-8"));
                String name = params[1];
                String username = params[2];
                String password = params[3];
                String pin = params [4];

                String data =  URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(name, "UTF-8") + "&" +
                        URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(username, "UTF-8") + "&" +
                        URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8") + "&" +
                        URLEncoder.encode("pin", "UTF-8") + "=" + URLEncoder.encode(pin, "UTF-8");
                bufferedWriter.write(data);
                bufferedWriter.flush();
                bufferedWriter.close();
                OS.close();

                InputStream IS = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(IS));
                StringBuilder stringBuilder = new StringBuilder();
                String line = "";
                while ((line = bufferedReader.readLine())!= null)
                {
                    stringBuilder.append(line+"\n");
                }
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if(method.equals("login"))
        {

            try {
                URL url = new URL(login_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream,"UTF-8"));
                String username = params[1];
                String password = params[2];
                String data = URLEncoder.encode("username","UTF-8")+"="+URLEncoder.encode(username,"UTF-8")+"&"+
                        URLEncoder.encode("password","UTF-8")+"="+URLEncoder.encode(password,"UTF-8");
                bufferedWriter.write(data);
                bufferedWriter.flush();
                bufferedWriter.close();
                outputStream.close();

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line = "";
                while ((line = bufferedReader.readLine())!= null)
                {
                    stringBuilder.append(line+"\n");
                }
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(String json) {
        try {
            progressDialog.dismiss();
            JSONObject jsonObject = new JSONObject(json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1));
            JSONArray jsonArray = jsonObject.getJSONArray("server_response");
            JSONObject JO = jsonArray.getJSONObject(0);
            String code = JO.getString("code");
            String message = JO.getString("message");

            if(code.equals("reg_success"))
            {
                AlertDialog.Builder builder  = new AlertDialog.Builder(ctx);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                });
                builder.setTitle("Registration Success");
                builder.setMessage(message);
                builder.show();
            }
            else if(code.equals("reg_failed"))
            {
                AlertDialog.Builder builder  = new AlertDialog.Builder(ctx);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText editText = (EditText) activity.findViewById(R.id.new_username);
                        editText.setText("");
                    }
                });
                builder.setTitle("Registration Failed");
                builder.setMessage(message);
                builder.show();
             }
            else if (code.equals("login_success"))
            {
                String id = JO.getString("id");
                String name = JO.getString("name");
                String credit = JO.getString("credit");
                String pin = JO.getString("pin");
                Intent intent = new Intent(activity,MainActivity.class);
                editor.putString("Message",message).commit();
                editor.putString("UserID",id).commit();
                editor.putString("Name",name).commit();
                editor.putString("Credit",credit).commit();
                editor.putString("Pin",pin).commit();
                editor.putBoolean("logout",false).commit();
                activity.startActivity(intent);
                activity.finish();
            }
            else if (code.equals("login_failed"))
            {
                AlertDialog.Builder builder  = new AlertDialog.Builder(ctx);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText user,pass;
                        user = (EditText) activity.findViewById(R.id.user_name);
                        pass = (EditText) activity.findViewById(R.id.user_pass);
                        user.setText("");
                        pass.setText("");
                    }
                });
                builder.setTitle("Login Failed");
                builder.setMessage(message);
                builder.show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    }