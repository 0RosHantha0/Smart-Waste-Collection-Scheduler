package com.example.wastecollectionscheduler;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScheduleActivity extends AppCompatActivity {

    private EditText dateEditText, timeEditText;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private Button submitButton;
    private RadioGroup wasteTypeRadioGroup;
    private FirebaseUser user;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        dateEditText = findViewById(R.id.dateEditText);
        timeEditText = findViewById(R.id.timeEditText);
        datePicker = findViewById(R.id.datePicker);
        timePicker = findViewById(R.id.timePicker);
        submitButton = findViewById(R.id.submitButton);
        wasteTypeRadioGroup = findViewById(R.id.wasteTypeRadioGroup);

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Initialize the time picker to 24-hour format
        timePicker.setIs24HourView(true);

        // Hide the DatePicker and TimePicker initially
        datePicker.setVisibility(DatePicker.GONE);
        timePicker.setVisibility(TimePicker.GONE);

        // Show the DatePicker when the date EditText is clicked
        dateEditText.setOnClickListener(view -> datePicker.setVisibility(DatePicker.VISIBLE));

        // Show the TimePicker when the time EditText is clicked
        timeEditText.setOnClickListener(view -> timePicker.setVisibility(TimePicker.VISIBLE));

        // Set the selected date to the date EditText
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            datePicker.setOnDateChangedListener((view, year, monthOfYear, dayOfMonth) -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, monthOfYear, dayOfMonth);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                dateEditText.setText(dateFormat.format(calendar.getTime()));
                datePicker.setVisibility(DatePicker.GONE);
            });
        }

        // Set the selected time to the time EditText
        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            String time = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
            timeEditText.setText(time);
            timePicker.setVisibility(TimePicker.GONE);
        });

        // Handle the incoming intent and set the date and time based on the recommendation
        handleIncomingIntent();

        // Set a click listener on the submit button
        submitButton.setOnClickListener(view -> {
            int selectedWasteTypeId = wasteTypeRadioGroup.getCheckedRadioButtonId();
            if (selectedWasteTypeId == -1) {
                // No waste type selected
                Toast.makeText(ScheduleActivity.this, "Please select a waste type", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedWasteTypeRadioButton = findViewById(selectedWasteTypeId);
            String selectedWasteType = selectedWasteTypeRadioButton.getText().toString();
            String selectedDate = dateEditText.getText().toString();
            String selectedTime = timeEditText.getText().toString();
            String userId = user.getUid();

            if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(ScheduleActivity.this, "Please select date and time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save schedule to Firestore
            saveScheduleToFirestore(selectedWasteType, selectedDate, selectedTime, userId);
        });
    }

    private void handleIncomingIntent() {
        String prediction = getIntent().getStringExtra("prediction");
        String recommendedDate = getIntent().getStringExtra("recommendedDate");
        if (prediction != null && recommendedDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);

            Calendar calendar = Calendar.getInstance();
            try {
                Date date = dateFormat.parse(recommendedDate);
                calendar.setTime(date);

                // Set the time based on the prediction
                switch (prediction) {
                    case "Weekday morning pickup":
                        calendar.set(Calendar.HOUR_OF_DAY, 9);
                        break;

                    case "Weekday evening pickup":
                        calendar.set(Calendar.HOUR_OF_DAY, 18);
                        break;

                    case "Weekend morning pickup":
                        calendar.set(Calendar.HOUR_OF_DAY, 9);
                        break;

                    case "Weekend evening pickup":
                        calendar.set(Calendar.HOUR_OF_DAY, 18);
                        break;
                }

                dateEditText.setText(dateFormat.format(calendar.getTime()));
                timeEditText.setText(timeFormat.format(calendar.getTime()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveScheduleToFirestore(String wasteType, String date, String time, String userId) {
        String status = "Scheduled";  // Default status

        Map<String, Object> schedule = new HashMap<>();
        schedule.put("wasteType", wasteType);
        schedule.put("date", date);
        schedule.put("time", time);
        schedule.put("status", status);
        schedule.put("userId", userId);

        db.collection("schedules")
                .add(schedule)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ScheduleActivity.this, "Schedule saved successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ScheduleActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ScheduleActivity.this, "Failed to save schedule", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
