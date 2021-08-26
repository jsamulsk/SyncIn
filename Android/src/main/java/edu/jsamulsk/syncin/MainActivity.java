package edu.jsamulsk.syncin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private ListView contactsListView;
    private EditText addContactEditText;
    private Button addContactButton;
    private Button clearContactButton;
    private ArrayList<String> contactsList;
    private ArrayAdapter<String> contactsAdapter;

    private EditText startTimeEditText;
    private EditText startDateEditText;
    private EditText endTimeEditText;
    private EditText endDateEditText;
    private Spinner durationDropdown;
    private Calendar startDateTime;
    private Calendar endDateTime;

    private Button submitSearchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize layout for choosing contacts
        contactsListView = (ListView) findViewById(R.id.contactsListView);
        addContactEditText = (EditText) findViewById(R.id.addContactEditText);
        addContactButton = (Button) findViewById(R.id.addContactButton);
        clearContactButton = (Button) findViewById(R.id.clearContactButton);
        contactsList = new ArrayList<>();
        contactsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactsList);

        // initialize layout for choosing search parameters
        startTimeEditText = (EditText) findViewById(R.id.startTimeEditText);
        startDateEditText = (EditText) findViewById(R.id.startDateEditText);
        endTimeEditText = (EditText) findViewById(R.id.endTimeEditText);
        endDateEditText = (EditText) findViewById(R.id.endDateEditText);
        durationDropdown = (Spinner) findViewById(R.id.durationDropdown);
        String[] durationList = new String[]{"15 minutes", "30 minutes", "45 minutes", "1 hour"};
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, durationList);
        durationDropdown.setAdapter(durationAdapter);
        startDateTime = Calendar.getInstance();
        endDateTime = Calendar.getInstance();
        startDateTime.set(Calendar.SECOND, 0); // app doesn't let user choose seconds, so don't want them in UTC
        startDateTime.set(Calendar.MILLISECOND, 0); // "
        endDateTime.set(Calendar.SECOND, 0); // "
        endDateTime.set(Calendar.MILLISECOND, 0); // "

        // add listener to contact management button (dynamically expands list view)
        addContactButton.setOnClickListener(new ButtonClickListener());
        clearContactButton.setOnClickListener(new ButtonClickListener());
        contactsListView.setAdapter(contactsAdapter);

        // make date and time edit texts not respond to keyboard
        startTimeEditText.setKeyListener(null);
        startDateEditText.setKeyListener(null);
        endTimeEditText.setKeyListener(null);
        endDateEditText.setKeyListener(null);

        // instead, they pull up time and date pickers on focus and on click (need focus for first tap, click for subsequent)
        startTimeEditText.setOnClickListener(new ButtonClickListener());
        startDateEditText.setOnClickListener(new ButtonClickListener());
        endTimeEditText.setOnClickListener(new ButtonClickListener());
        endDateEditText.setOnClickListener(new ButtonClickListener());

        startTimeEditText.setOnFocusChangeListener(new ShowPickerFocusChangeListener());
        startDateEditText.setOnFocusChangeListener(new ShowPickerFocusChangeListener());
        endTimeEditText.setOnFocusChangeListener(new ShowPickerFocusChangeListener());
        endDateEditText.setOnFocusChangeListener(new ShowPickerFocusChangeListener());

        // search and submit button
        submitSearchButton = (Button) findViewById(R.id.submitSearchButton);
        submitSearchButton.setOnClickListener(new ButtonClickListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings_logout) {
            SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.my_preferences), Context.MODE_PRIVATE).edit();
            editor.remove("email");
            editor.commit();

            while (getSharedPreferences(getString(R.string.my_preferences), Context.MODE_PRIVATE).contains("email"));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addContact() {
        if (!addContactEditText.getText().toString().equals("")) {
            contactsList.add(addContactEditText.getText().toString());
            addContactEditText.setText("");

            contactsAdapter.notifyDataSetChanged();
        }
    }

    private void clearContacts() {
        contactsList.clear();

        contactsAdapter.notifyDataSetChanged();
    }

    private void displayDateTimePicker(View v) {
        Calendar c = Calendar.getInstance();

        if (v.getId() == startTimeEditText.getId() || v.getId() == endTimeEditText.getId()) {
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(v.getContext(), new TimePickerListener(v.getId()), hour, minute,
                    DateFormat.is24HourFormat(v.getContext()));
            timePickerDialog.show();
        }
        else if (v.getId() == startDateEditText.getId() || v.getId() == endDateEditText.getId()) {
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(v.getContext(), new DatePickerListener(v.getId()), year, month, day);
            datePickerDialog.show();
        }
    }

    private void submitSearch() {
        if (!startTimeEditText.getText().toString().equals("") &&
            !startDateEditText.getText().toString().equals("") &&
            !endTimeEditText.getText().toString().equals("") &&
            !endDateEditText.getText().toString().equals("")) {
            // parse list of emails into json array
            JsonArray people = new Gson().toJsonTree(contactsList).getAsJsonArray();
            people.add(getSharedPreferences(getString(R.string.my_preferences), Context.MODE_PRIVATE).getString("email", null));

            // create request
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.uri = getString(R.string.root_address) + "/api/find/"; // my API address for getting list of events
            httpRequest.method = HttpRequest.METHOD_POST;
            httpRequest.params.put("searchStart", String.valueOf(startDateTime.getTimeInMillis() / 1000));
            httpRequest.params.put("searchEnd", String.valueOf(endDateTime.getTimeInMillis() / 1000));
            httpRequest.arrayParams.put("people", people);
            Log.d("YEEHAW", httpRequest.getBody());

            // execute request
            RequestEvents requestEvents = new RequestEvents();
            requestEvents.execute(httpRequest);
        }
        else
            Toast.makeText(this, "Please finish choosing search parameters", Toast.LENGTH_LONG).show();
    }

    // referenced by async task, in main class for access to context
    private void startNextActivity(Bundle bundle) {
        Intent intent = new Intent(this, ScheduleActivity.class);
        intent.putExtras(bundle);
        intent.setAction(getString(R.string.ACTION_FOUND));
        startActivity(intent);
    }

    // referenced by async task, in main class for access to context
    private void notifyInvalidEmails(String emailList) {
        String message = "The following emails are not registered with Sync In: " + emailList;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId() == addContactButton.getId())
                addContact();
            else if (v.getId() == clearContactButton.getId())
                clearContacts();
            else if (v.getId() == submitSearchButton.getId())
                submitSearch();
            else
                displayDateTimePicker(v);
        }
    }
    private class ShowPickerFocusChangeListener implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus)
                displayDateTimePicker(v);
        }
    }

    public class TimePickerListener implements TimePickerDialog.OnTimeSetListener {
        private final int viewID;

        public TimePickerListener(int id) {
            viewID = id;
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Calendar calendar;
            if (viewID == startTimeEditText.getId())
                calendar = startDateTime;
            else
                calendar = endDateTime;

            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);

            String half = "P.M.";
            if (hourOfDay < 12)
                half = "A.M.";
            hourOfDay %= 12;
            if (hourOfDay == 0)
                hourOfDay = 12;
            String minuteString = "" + minute;
            if (minute < 10)
                minuteString = "0" + minuteString;

            String newText = "" + hourOfDay + ":" + minuteString + " " + half;
            EditText editText = findViewById(viewID);
            editText.setText(newText);
        }
    }
    public class DatePickerListener implements DatePickerDialog.OnDateSetListener {
        private final int viewID;

        public DatePickerListener(int id) {
            viewID = id;
        }
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            Calendar calendar;
            if (viewID == startDateEditText.getId())
                calendar = startDateTime;
            else
                calendar = endDateTime;

            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            String[] dateStrings = calendar.getTime().toString().substring(0, 10).split(" ");
            String newText = dateStrings[0] + ", " + dateStrings[1] + " " + dateStrings[2] + " " + year;
            EditText editText = findViewById(viewID);
            editText.setText(newText);
        }
    }

    // async task to get events of requested people during search area
    @SuppressLint("StaticFieldLeak")
    private class RequestEvents extends AsyncTask<HttpRequest, String, String> {
        @Override
        protected String doInBackground(HttpRequest... httpRequests) {
            return httpRequests[0].getData(); // makes http request
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonResult = new JSONObject(result);

                // if all emails are valid, calculate free time with events
                // else, list emails responsible
                if (jsonResult.getBoolean("response")) {
                    String[] chosenDuration = durationDropdown.getSelectedItem().toString().split(" ");
                    int duration = Integer.parseInt(chosenDuration[0]); // given in minutes or hours, want in seconds
                    if (!chosenDuration[1].equals("minutes"))
                        duration *= 60; // hours to minutes
                    duration *= 60; // minutes to seconds

                    Bundle bundle = new Bundle();
                    bundle.putInt("duration", duration);
                    bundle.putLong("searchStart", startDateTime.getTimeInMillis() / 1000);
                    bundle.putLong("searchEnd", endDateTime.getTimeInMillis() / 1000);
                    bundle.putString("events", jsonResult.getJSONArray("body").toString());
                    bundle.putString("others", new Gson().toJsonTree(contactsList).getAsJsonArray().toString());

                    startNextActivity(bundle); // helper to interact with activity
                }
                else {
                    String emails = "";
                    JSONArray body = jsonResult.getJSONArray("body");

                    for (int i = 0; i < body.length(); i++) {
                        emails = emails.concat(body.getString(i) + ", ");
                    }
                    emails = emails.substring(0, emails.length() - 2);

                    notifyInvalidEmails(emails); // helper to interact with activity
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}