package edu.jsamulsk.syncin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;

    private SharedPreferences preferences;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkSignedIn();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = (EditText) findViewById(R.id.emailEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        loginButton = (Button) findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new LoginListener());
    }

    @Override
    protected void onResume() {
        checkSignedIn();
        super.onResume();
    }

    // check if user is signed in
    private void checkSignedIn() {
        preferences = getSharedPreferences(getString(R.string.my_preferences), Context.MODE_PRIVATE);
        if (preferences.contains("email")) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    // referenced by async task, in main class for access to context
    private void createNewUser() {
        String email = emailEditText.getText().toString();

        // ask if user would like to create account
        String message = "No user \"" + email + "\" exists.\n" +
                "Create account in browser?";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("CONTINUE", (dialog, id) -> {
                    String uriString = getString(R.string.root_address) + "/api/oauth2/?email=" + email;
                    Uri url = Uri.parse(uriString);

                    CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                            .build();
                    customTabsIntent.launchUrl(this, url);
                })
                .setNegativeButton("CANCEL",null);
        builder.create().show();
    }

    // referenced by async task, in main class for access to context
    private void startNextActivity() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("email", email);
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private class LoginListener implements Button.OnClickListener {
        @Override
        public void onClick(View v) {
            email = emailEditText.getText().toString();

            if (!email.equals("")) {
                // create request
                HttpRequest httpRequest = new HttpRequest();
                httpRequest.uri = getString(R.string.root_address) + "/api/exists/"; // my API address for checking if user exists
                httpRequest.params.put("email", email);

                // execute request
                ConfirmUserExists confirmUserExists = new ConfirmUserExists();
                confirmUserExists.execute(httpRequest);
            }
            else
                Toast.makeText(v.getContext(), "Please enter an email", Toast.LENGTH_LONG).show();
        }
    }

    // async task to check if user exists in database
    @SuppressLint("StaticFieldLeak")
    private class ConfirmUserExists extends AsyncTask<HttpRequest, String, String> {
        @Override
        protected String doInBackground(HttpRequest... httpRequests) {
            return httpRequests[0].getData(); // makes http request
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonResult = new JSONObject(result);

                // if user exists, login
                // else, prompt user to create new account
                if (jsonResult.getBoolean("response"))
                    startNextActivity();
                else
                    createNewUser();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}