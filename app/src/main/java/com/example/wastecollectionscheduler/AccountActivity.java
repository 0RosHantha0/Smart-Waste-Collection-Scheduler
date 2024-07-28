package com.example.wastecollectionscheduler;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText, confirmPasswordEditText;
    private Button updateButton;
    private FirebaseUser user;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirm_password);
        updateButton = findViewById(R.id.updateButton);

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUsername = usernameEditText.getText().toString().trim();
                String newPassword = passwordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                if (!TextUtils.isEmpty(newUsername)) {
                    updateUsername(newUsername);
                }

                if (!TextUtils.isEmpty(newPassword) && !TextUtils.isEmpty(confirmPassword)) {
                    if (newPassword.equals(confirmPassword)) {
                        updatePassword(newPassword);
                    } else {
                        Toast.makeText(AccountActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void updateUsername(final String newUsername) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            updateFirestoreUsername(newUsername);
                        } else {
                            Toast.makeText(AccountActivity.this, "Failed to update username", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateFirestoreUsername(String newUsername) {
        String userId = user.getUid();
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update("username", newUsername)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AccountActivity.this, "Username updated successfully", Toast.LENGTH_SHORT).show();
                            redirectToHomePage();
                        } else {
                            Toast.makeText(AccountActivity.this, "Failed to update username", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updatePassword(String newPassword) {
        user.updatePassword(newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AccountActivity.this, "Password updated", Toast.LENGTH_SHORT).show();
                            redirectToHomePage();
                        } else {
                            Toast.makeText(AccountActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void redirectToHomePage() {
        Intent intent = new Intent(AccountActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: Close the current activity to prevent user from going back to it using the back button
    }
}
