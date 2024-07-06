package com.nabarup.college.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.nabarup.college.R;

public class Forgot_pass extends AppCompatActivity {

    EditText ForgotEmail;
    Button sendemail;
    TextView red_login, telegram;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pass);

        sendemail = findViewById(R.id.send_email_button);
        ForgotEmail = findViewById(R.id.for_email);
        red_login = findViewById(R.id.red_login);

        sendemail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    String email = ForgotEmail.getText().toString().trim();
                    sendPasswordResetEmail(email);
                }
            }
        });
        red_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Forgot_pass.this,Login.class);
                startActivity(intent);
                finish();
            }
        });


    }

    private boolean validateInput() {
        String email = ForgotEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            ForgotEmail.setError("Email cannot be empty");
            return false;
        }
        return true;
    }

    private void sendPasswordResetEmail(String email) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(Forgot_pass.this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(Forgot_pass.this, "Database Error.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } else {
                    Toast.makeText(Forgot_pass.this, "Database Error.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

