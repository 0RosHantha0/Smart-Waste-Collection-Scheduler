package com.example.wastecollectionscheduler;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RecommendationActivity extends AppCompatActivity {
    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load TFLite model
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load model", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Start background task to fetch data and make recommendation
        new RecommendationTask().execute();
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(getAssets().openFd("model.tflite").getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = getAssets().openFd("model.tflite").getStartOffset();
        long declaredLength = getAssets().openFd("model.tflite").getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private class RecommendationTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            try {
                Task<QuerySnapshot> queryTask = db.collection("schedules")
                        .whereEqualTo("userId", userId)
                        .orderBy("date", Query.Direction.DESCENDING)
                        .limit(1)
                        .get();

                // Blocking call to fetch data
                QuerySnapshot querySnapshot = Tasks.await(queryTask);

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                    String wasteType = document.getString("wasteType");
                    String date = document.getString("date");
                    String time = document.getString("time");

                    // Prepare input data for model
                    float[] inputData = prepareInputData(wasteType, date, time);

                    // Perform inference
                    float[][] outputData = new float[1][4]; // 4 output classes
                    tflite.run(inputData, outputData);

                    // Process output data to determine the predicted pickup schedule
                    String prediction = processOutputData(outputData);

                    // Determine the next recommended date
                    String recommendedDate = determineNextRecommendedDate(date);

                    // Return the prediction and recommended date
                    return prediction + "," + recommendedDate;
                }
            } catch (Exception e) {
                Log.e("FirestoreError", "Error getting documents: ", e);
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                String[] parts = result.split(",");
                String prediction = parts[0];
                String recommendedDate = parts[1];
                navigateToScheduleActivity(prediction, recommendedDate);
            } else {
                Toast.makeText(RecommendationActivity.this, "Please make your first schedule", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private float[] prepareInputData(String wasteType, String date, String time) {
        float[] inputData = new float[6]; // Adjust size based on your input features

        // Prepare Waste Type
        if ("Biodegradable".equalsIgnoreCase(wasteType)) {
            inputData[0] = 1;
            inputData[1] = 0;
        } else {
            inputData[0] = 0;
            inputData[1] = 1;
        }

        // Prepare Day of the Week
        if (isWeekend(date)) {
            inputData[4] = 0;
            inputData[5] = 1;
        } else {
            inputData[4] = 1;
            inputData[5] = 0;
        }

        // Prepare Time of Day
        if (isMorning(time)) {
            inputData[2] = 1;
            inputData[3] = 0;
        } else if (isEvening(time)) {
            inputData[2] = 0;
            inputData[3] = 1;
        }

        return inputData;
    }

    private boolean isWeekend(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date parsedDate = sdf.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isMorning(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            Date parsedTime = sdf.parse(time);
            int hour = parsedTime.getHours();
            return hour >= 0 && hour < 12;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isEvening(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            Date parsedTime = sdf.parse(time);
            int hour = parsedTime.getHours();
            return hour >= 12 && hour < 18;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String processOutputData(float[][] outputData) {
        int maxIndex = 0;
        for (int i = 1; i < outputData[0].length; i++) {
            if (outputData[0][i] > outputData[0][maxIndex]) {
                maxIndex = i;
            }
        }

        switch (maxIndex) {
            case 0:
                return "Weekday morning pickup";
            case 1:
                return "Weekday evening pickup";
            case 2:
                return "Weekend morning pickup";
            case 3:
                return "Weekend evening pickup";
            default:
                return "Unknown";
        }
    }

    private String determineNextRecommendedDate(String lastScheduleDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdf.parse(lastScheduleDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            // Calculate the days to add to reach the next recommended date
            int daysToAdd = 0;
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                // If the last schedule is on a weekend, recommend the next weekend
                daysToAdd = 7; // Next weekend (either Saturday or Sunday)
            } else {
                // If the last schedule is on a weekday, add days to reach the next weekend (Saturday)
                daysToAdd = 6 - dayOfWeek + Calendar.SATURDAY; // Calculate the days to Saturday of the next week
            }

            // Adjust the calendar to the next recommended date
            calendar.add(Calendar.DAY_OF_MONTH, daysToAdd);

            return sdf.format(calendar.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void navigateToScheduleActivity(String prediction, String recommendedDate) {
        Intent intent = new Intent(RecommendationActivity.this, ScheduleActivity.class);
        intent.putExtra("prediction", prediction);
        intent.putExtra("recommendedDate", recommendedDate);
        startActivity(intent);
        finish(); // Finish the current activity
    }
}
