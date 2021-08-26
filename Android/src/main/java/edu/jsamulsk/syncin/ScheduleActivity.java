package edu.jsamulsk.syncin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ScheduleActivity extends AppCompatActivity {

    private int duration;
    private long searchStart;
    private long searchEnd;
    private JSONArray events;
    private JSONArray others;

    private RadioGroup freeTimeRadioGroup;
    private Map<Integer, Long> freeTimesById;
    private int selectedFreeTimeId;

    private CheckBox inviteOthersCheckBox;
    private Button scheduleActivityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // initialize radio group
        freeTimeRadioGroup = (RadioGroup) findViewById(R.id.freeTimeRadioGroup);
        freeTimesById = new HashMap<>();
        selectedFreeTimeId = -1;

        // initialize submit buttons at bottom of page
        inviteOthersCheckBox = (CheckBox) findViewById(R.id.inviteOthersCheckBox);
        scheduleActivityButton = (Button) findViewById(R.id.scheduleActivityButton);

        scheduleActivityButton.setOnClickListener(new ButtonClickListener());

        // begin string of helper functions to populate radio group with free time
        try {
            populateEventData();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // populate event data from previous activity's http response
    private void populateEventData() throws JSONException {
        if (getString(R.string.ACTION_FOUND).equals(getIntent().getAction())) {
            Bundle extras = getIntent().getExtras();

            duration = extras.getInt("duration");
            searchStart = extras.getLong("searchStart");
            searchEnd = extras.getLong("searchEnd");
            events = new JSONArray(extras.getString("events"));
            others = new JSONArray(extras.getString("others"));

            findFreeTime();
        }
    }
    // find available free time given search duration and event data
    private void findFreeTime() throws JSONException {
        ArrayList<Calendar> freeTimes = new ArrayList<>();

        long currStart = searchStart;
        while ((currStart + duration) <= searchEnd) {
            boolean eventFits = true;

            for (int i = 0; i < events.length(); i++)
                if (((currStart + duration) > events.getJSONObject(i).getLong("start")) && (currStart < events.getJSONObject(i).getLong("end")))
                    eventFits = false;

            if (eventFits) {
                Calendar currFreeTime = Calendar.getInstance();
                currFreeTime.setTimeInMillis(currStart * 1000L);

                freeTimes.add(currFreeTime);
            }

            currStart += (15*60); // increment by 15 minutes
        }

        addFreeTime(freeTimes);
    }
    // populate radio group with free times found
    private void addFreeTime(ArrayList<Calendar> freeTimes) throws JSONException {
        for (Calendar freeTime : freeTimes) {
            int tempId = ViewCompat.generateViewId();
            freeTimesById.put(tempId, freeTime.getTimeInMillis() / 1000L);

            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(tempId);
            radioButton.setText(freeTime.getTime().toString());
            radioButton.setOnClickListener(new ButtonClickListener());

            freeTimeRadioGroup.addView(radioButton);
        }
    }

    private void scheduleMeeting() {
        if (selectedFreeTimeId != -1) {
            JsonObject othersParam = new Gson().toJsonTree(others).getAsJsonObject();

            // create request
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.uri = getString(R.string.root_address) + "/api/schedule/"; // my API address for getting list of events
            httpRequest.method = HttpRequest.METHOD_POST;
            httpRequest.params.put("owner", getSharedPreferences(getString(R.string.my_preferences), Context.MODE_PRIVATE).getString("email", null));
            httpRequest.params.put("start", "" + freeTimesById.get(selectedFreeTimeId));
            httpRequest.params.put("end", "" + (freeTimesById.get(selectedFreeTimeId) + duration));
            httpRequest.params.put("sendInvites", String.valueOf(inviteOthersCheckBox.isChecked()));
            httpRequest.arrayParams.put("others", othersParam.getAsJsonArray("values"));
            Log.d("YEEHAW", httpRequest.getBody());

            ScheduleMeetingTask scheduleMeetingTask = new ScheduleMeetingTask();
            scheduleMeetingTask.execute(httpRequest);
        }
        else
            Toast.makeText(this, "Please select a time to schedule", Toast.LENGTH_LONG).show();
    }

    // referenced by async task, in main class for access to context
    private void onMeetingScheduled() {
        Toast.makeText(this, "Meeting Scheduled", Toast.LENGTH_LONG).show();
        finish();
    }

    private class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId() == scheduleActivityButton.getId())
                scheduleMeeting();
            else
                selectedFreeTimeId = v.getId();
        }
    }

    // async task to get events of requested people during search area
    @SuppressLint("StaticFieldLeak")
    private class ScheduleMeetingTask extends AsyncTask<HttpRequest, String, String> {
        @Override
        protected String doInBackground(HttpRequest... httpRequests) {
            return httpRequests[0].getData(); // makes http request
        }

        @Override
        protected void onPostExecute(String result) {
            onMeetingScheduled();
        }
    }
}