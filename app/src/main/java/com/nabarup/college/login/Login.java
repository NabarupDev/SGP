package com.nabarup.college.login;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nabarup.college.R;
import com.nabarup.college.Home;
import com.nabarup.college.admin_home;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Login extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private ProgressBar progressBar;
    private TextView signupRedirectText, adminRedirectText, Forgot_password;
    private ImageView passwordVisibilityToggle;
    private SharedPreferences sharedPref;

    private static final String FIREBASE_APP_NAME = "com.nabarup.avator";
    private static final String GMAIL_PATTERN = "[a-zA-Z0-9._%+-]+@gmail\\.[a-z]+";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE);

        if (sharedPref.getBoolean("loggedInBefore", false)) {
            navigateToHomeScreen(sharedPref.getString("userRole", ""));
        } else {
            FirebaseApp.initializeApp(this);
            setContentView(R.layout.activity_login);

            initializeUI();

            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (validateInput()) {
                        String email = loginEmail.getText().toString().trim();
                        String password = loginPassword.getText().toString().trim();
                        progressBar.setVisibility(View.VISIBLE);
                        loginButton.setVisibility(View.INVISIBLE);
                        checkUser(email, password);
                    }
                }
            });

            signupRedirectText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Login.this, Sign_up.class));
                    finish();
                }
            });

            adminRedirectText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Login.this, admin_signup.class));
                    finish();
                }
            });

            Forgot_password.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Login.this, Forgot_pass.class));
                }
            });


        }
    }

    private void initializeUI() {
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_pass);
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progrssbar);
        passwordVisibilityToggle = findViewById(R.id.pass_view_img);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        adminRedirectText = findViewById(R.id.adminRedirectText);
        Forgot_password = findViewById(R.id.forgot_pass);
    }

    private boolean validateInput() {
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            loginEmail.setError("Email cannot be empty");
            return false;
        } else if (!email.matches(GMAIL_PATTERN)) {
            loginEmail.setError("Invalid email address");
            return false;
        } else if (TextUtils.isEmpty(password)) {
            loginPassword.setError("Password cannot be empty");
            return false;
        }
        return true;
    }

    private void checkUser(String email, String password) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        String[] categories = {"students", "teachers"};
        String[] years = {"1st", "2nd", "3rd"};
        String[] departments = {"CST", "EE", "CE", "ARC", "ETC", "EIE"};

        for (String category : categories) {
            for (String year : years) {
                checkUserInCategory(usersRef, category, year, email, password, departments);
            }
        }

        // Also check the admin categories
        checkAdminUser(usersRef, email, password);
    }

    private void checkAdminUser(DatabaseReference usersRef, String email, String password) {
        String[] adminDepartments = {"CST", "EE", "CE", "ARC", "ETC", "EIE"};

        for (String dept : adminDepartments) {
            Query query = usersRef.child("teachers").child(dept).orderByChild("email").equalTo(email);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String passwordFromDB = snapshot.child("password").getValue(String.class);
                            if (passwordFromDB.equals(hashPassword(password))) {
                                storeUserRoleAndNavigate("teachers");
                                return;
                            } else {
                                showToast("Incorrect password");
                            }
                        }
                    } else {
                        Log.d("LoginActivity", "Admin user not found in department: " + dept);
                    }
                    progressBar.setVisibility(View.INVISIBLE);
                    loginButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("LoginActivity", "DatabaseError: " + databaseError.getMessage());
                    showToast("DatabaseError");
                    progressBar.setVisibility(View.INVISIBLE);
                    loginButton.setVisibility(View.VISIBLE);
                }
            });
        }
    }


    private void checkUserInCategory(DatabaseReference usersRef, String category, String year, String email, String password, String[] departments) {
        for (String dept : departments) {
            Query query = usersRef.child(category).child(year).child(dept).orderByChild("email").equalTo(email);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String passwordFromDB = snapshot.child("password").getValue(String.class);
                            if (passwordFromDB.equals(hashPassword(password))) {
                                storeUserRoleAndNavigate(category);
                                return;
                            } else {
                                showToast("Incorrect password");
                            }
                        }
                    }
                    progressBar.setVisibility(View.INVISIBLE);
                    loginButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("LoginActivity", "DatabaseError: " + databaseError.getMessage());
                    showToast("DatabaseError");
                    progressBar.setVisibility(View.INVISIBLE);
                    loginButton.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void storeUserRoleAndNavigate(String category) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("loggedInBefore", true);
        editor.putString("userRole", category);
        editor.apply();

        navigateToHomeScreen(category);
    }

    private void navigateToHomeScreen(String userRole) {
        Intent intent;
        if ("students".equals(userRole)) {
            intent = new Intent(Login.this, Home.class);
        } else if ("teachers".equals(userRole)) {
            intent = new Intent(Login.this, admin_home.class);
        } else {
            showToast("Unknown user role");
            return;
        }
        startActivity(intent);
        finish();
    }

    private void togglePasswordVisibility() {
        if (loginPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        loginPassword.setSelection(loginPassword.length());
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showToast(String message) {
        Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
    }
}
