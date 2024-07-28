package com.example.wastecollectionscheduler;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class SchedulesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ScheduleAdapter scheduleAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedules);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleAdapter = new ScheduleAdapter();
        recyclerView.setAdapter(scheduleAdapter);

        db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("schedules")
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        List<Schedule> schedules = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Schedule schedule = doc.toObject(Schedule.class);
                            schedule.setId(doc.getId()); // Set the document ID
                            schedules.add(schedule);
                        }
                        scheduleAdapter.setSchedules(schedules);
                    }
                });
    }

    private class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

        private List<Schedule> schedules = new ArrayList<>();

        @NonNull
        @Override
        public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
            return new ScheduleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
            Schedule schedule = schedules.get(position);
            holder.wasteTypeTextView.setText(schedule.getWasteType());
            holder.dateTextView.setText(schedule.getDate());
            holder.timeTextView.setText(schedule.getTime());
            holder.statusTextView.setText(schedule.getStatus());

            holder.completeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    schedule.setStatus("Completed");
                    db.collection("schedules").document(schedule.getId())
                            .update("status", "Completed")
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    notifyDataSetChanged();
                                }
                            });
                }
            });
        }

        @Override
        public int getItemCount() {
            return schedules.size();
        }

        public void setSchedules(List<Schedule> schedules) {
            this.schedules = schedules;
            notifyDataSetChanged();
        }

        class ScheduleViewHolder extends RecyclerView.ViewHolder {

            TextView wasteTypeTextView, dateTextView, timeTextView, statusTextView;
            Button completeButton;

            ScheduleViewHolder(View itemView) {
                super(itemView);
                wasteTypeTextView = itemView.findViewById(R.id.wasteType);
                dateTextView = itemView.findViewById(R.id.date);
                timeTextView = itemView.findViewById(R.id.time);
                statusTextView = itemView.findViewById(R.id.status);
                completeButton = itemView.findViewById(R.id.completeButton);
            }
        }
    }
}
