/*
 * Copyright (c) 2012-2017 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.fragments.VPNProfileList;

/**
 * Created by admin on 2017/11/27.
 */

public class Login extends BaseActivity {

    Button submit;
    EditText id;
    EditText password;
    CheckBox checkBox;

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.login);
        id = findViewById(R.id.id);
        password = findViewById(R.id.password);
        submit = findViewById(R.id.submit);
        checkBox = findViewById(R.id.checkbox);
        File file = new File("/data/user/0/de.blinkt.openvpn/shared_prefs/VPNList.xml");
        if (file.exists() == true)
            file.delete();

        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        String mid = sharedPreferences.getString("id", "0"), mpw = sharedPreferences.getString("password", "0");
        if (mid.equals("0") != true && MainActivity.ACCOUNT == null) {
            new login().execute(mid, mpw);
        }


        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new login().execute(id.getText().toString(), password.getText().toString());
            }
        });
    }

    private class login extends AsyncTask<String, Void, String> {
        String login = null;
        URL url1 = null;
        String user = null;
        HttpURLConnection getuser = null;
        String accunt;
        String password;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            submit.setClickable(false);
        }

        @Override
        protected String doInBackground(String... params) {
            accunt = params[0];
            password = params[1];

            if (new httpcontent().GET(MainActivity.APIURL, false).equals("ERROR"))
                return "无法连接到服务器";
            login = login(accunt, password);
            if (login.equals("success")) {
                VPNProfileList.noticeText = "欢迎使用校园网vpn系统";
                return "success";
            } else {
                return login;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            submit.setClickable(true);
            if (s.equals("YES_VIP")) {
                Log.e("ASASASA", "SSSSAAASASAS");
                MainActivity.ACCOUNT = accunt;
                MainActivity.PASSWORD = password;
                Log.e("AS", MainActivity.ACCOUNT + "/" + accunt + MainActivity.PASSWORD);
                if (checkBox.isChecked()) {
                    SharedPreferences.Editor shard = getSharedPreferences("user", MODE_PRIVATE).edit();
                    shard.putString("id", accunt);
                    shard.putString("password", password);
                    shard.apply();
                }
                Intent intent = new Intent();
                setResult(1, intent);
                finish();
            } else {
                Toast.makeText(Login.this, s, Toast.LENGTH_SHORT).show();
            }

        }
    }

    private String login(String user, String password) {
        JSONObject json = null;
        String data = "user=" + user + "&passwd=" + password;
        URL url = null;
        HttpURLConnection http = null;
        try {
            url = new URL(MainActivity.APIURL + "login");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("POST");
            http.setConnectTimeout(1000);
            http.setReadTimeout(1000);
        } catch (IOException e) {
            e.printStackTrace();
        }


        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        http.setRequestProperty("Content-Length", String.valueOf(data.length()));
        http.setDoOutput(true);
        http.setDoInput(true);

        OutputStream out = null;
        try {
            out = http.getOutputStream();
            out.write(data.getBytes());
            out.close();
        } catch (IOException e) {
            return "连接异常";

        }

        try {
            if (http.getResponseCode() == 200) {
                InputStream in = http.getInputStream();

                String str = new httpcontent().readstream(in);
                try {
                    json = new JSONObject(str);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    if (json.getInt("code") == 0) {
                        List<String> map = http.getHeaderFields().get("Set-Cookie");
                        MainActivity.uid_token = map.get(0).substring(0, map.get(0).indexOf(";")) + ";" + map.get(1).substring(0, map.get(1).indexOf(";"));
                        return "success";
                    } else {
                        return json.getString("msg");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                return "连接异常";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "连接异常";
    }

}