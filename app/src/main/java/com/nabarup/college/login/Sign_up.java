package com.nabarup.college.login;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.getkeepsafe.relinker.BuildConfig;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nabarup.college.R;
import com.nabarup.college.HelperClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sign_up extends AppCompatActivity {

    TextView login_txt;
    ImageView passwordVisibilityToggle;
    EditText password_edit_box, sign_name, sign_email, sign_number, activation_code;
    Button sign_up_btn;
    FirebaseDatabase database;
    ProgressBar progressBar;
    String emailPattern = "[a-zA-Z0-9._%+-]+@gmail\\.[a-z]+";
    Context context;
    DatabaseReference reference;
    private static final String FIREBASE_APP_NAME = "com.nabarup.avator";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        context = this;
        setContentView(R.layout.activity_sign_up);

        sign_email = findViewById(R.id.sign_email);
        sign_name = findViewById(R.id.sign_name);
        sign_number = findViewById(R.id.signup_number);
        activation_code = findViewById(R.id.activation_code);
        sign_up_btn = findViewById(R.id.sign_up_button);
        login_txt = findViewById(R.id.login_txt);
        progressBar = findViewById(R.id.progrssbar);
        passwordVisibilityToggle = findViewById(R.id.pass_view_img);
        password_edit_box = findViewById(R.id.sign_up_pass);

        sign_up_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                sign_up_btn.setVisibility(View.INVISIBLE);
                if (validateInput()) {
                    final String email = sign_email.getText().toString().trim();
                    final String phone_no = sign_number.getText().toString().trim();
                    final String activationCode = activation_code.getText().toString().trim();

                    // Check if the email is already registered
                    isEmailAlreadyRegistered(email, new EmailCheckCallback() {
                        @Override
                        public void onCallback(boolean isEmailRegistered) {
                            if (isEmailRegistered) {
                                // Email already registered
                                progressBar.setVisibility(View.INVISIBLE);
                                sign_up_btn.setVisibility(View.VISIBLE);
                                Toast.makeText(Sign_up.this, "User already registered with this email!", Toast.LENGTH_LONG).show();
                            } else {
                                // Check if the phone number is already registered
                                isPhoneNumberAlreadyRegistered(phone_no, new PhoneNumberCheckCallback() {
                                    @Override
                                    public void onCallback(boolean isPhoneRegistered) {
                                        if (isPhoneRegistered) {
                                            // Phone number already registered
                                            progressBar.setVisibility(View.INVISIBLE);
                                            sign_up_btn.setVisibility(View.VISIBLE);
                                            Toast.makeText(Sign_up.this, "Phone number already registered!", Toast.LENGTH_LONG).show();
                                        } else {
                                            // Proceed to sign-up
                                            progressBar.setVisibility(View.VISIBLE);
                                            sign_up_btn.setVisibility(View.INVISIBLE);
                                            FirebaseOptions options = new FirebaseOptions.Builder()
                                                    .setApplicationId(BuildConfig.APPLICATION_ID)
                                                   .setApiKey("Your_API_Key_Here")
                                                   .setDatabaseUrl("Your_FIREBASE_DATABASE_URL")
                                                   .setStorageBucket("Your_FIREBASE_STORAGE_BUCKET")
                                                   .setProjectId("Your_FIREBASE_PROJECT_ID")
                                                   .build();

                                            FirebaseApp app = FirebaseApp.initializeApp(context, options, FIREBASE_APP_NAME);
                                            database = FirebaseDatabase.getInstance(app);

                                            // Parse the activation code to extract year and department
                                            String[] parts = activationCode.split("\\*");
                                            if (parts.length == 3) {
                                                String department = parts[0];
                                                String year = parts[1];

                                                switch (department) {
                                                    case "cst":
                                                        reference = database.getReference("users").child("students").child(year).child("CST");
                                                        break;
                                                    case "ee":
                                                        reference = database.getReference("users").child("students").child(year).child("EE");
                                                        break;
                                                    case "ce":
                                                        reference = database.getReference("users").child("students").child(year).child("CE");
                                                        break;
                                                    case "arc":
                                                        reference = database.getReference("users").child("students").child(year).child("ARC");
                                                        break;
                                                    case "etc":
                                                        reference = database.getReference("users").child("students").child(year).child("ETC");
                                                        break;
                                                    case "eie":
                                                        reference = database.getReference("users").child("students").child(year).child("EIE");
                                                        break;
                                                    default:
                                                        reference = database.getReference("users");
                                                        break;
                                                }

                                                String name = sign_name.getText().toString();
                                                String number = sign_number.getText().toString();
                                                String password = password_edit_box.getText().toString();

                                                String hashedPassword = hashPassword(password);

                                                String uid = reference.push().getKey();

                                                HelperClass helperClass = new HelperClass(name, email, number, hashedPassword);

                                                reference.child(uid).setValue(helperClass);

                                                Toast.makeText(Sign_up.this, "You have signed up successfully!", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(Sign_up.this, Login.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(Sign_up.this, "Invalid activation code format", Toast.LENGTH_SHORT).show();
                                            }

                                            progressBar.setVisibility(View.INVISIBLE);
                                        }
                                    }
                                });
                            }
                        }
                    });
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                    sign_up_btn.setVisibility(View.VISIBLE);
                }
            }
        });

        login_txt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Sign_up.this, Login.class);
                startActivity(intent);
            }
        });

        passwordVisibilityToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });
    }

    private boolean validateInput() {
        String name = sign_name.getText().toString().trim();
        String email = sign_email.getText().toString().trim();
        String phone_no = sign_number.getText().toString().trim();
        String password = password_edit_box.getText().toString().trim();
        String activationCode = activation_code.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            sign_name.setError("Name can't be empty");
            return false;
        } else if (name.length() < 3) {
            sign_name.setError("Enter a valid name");
            return false;
        } else if (TextUtils.isEmpty(email)) {
            sign_email.setError("Email can't be empty");
            return false;
        } else if (email.length() < 15) {
            sign_email.setError("Enter a valid email");
            return false;
        } else if (!email.matches(emailPattern)) {
            sign_email.setError("Invalid email address");
            return false;
        } else if (TextUtils.isEmpty(phone_no)) {
            sign_number.setError("Phone Number can't be empty");
            return false;
        } else if (phone_no.length() != 10) {
            sign_number.setError("Phone Number must be 10 digits");
            return false;
        } else if (!phone_no.matches("[6-9]\\d{9}")) {
            // Check if the phone number don't starts with 6, 7, 8, or 9
            sign_number.setError("Please enter a valid phone number");
            return false;
        } else if (TextUtils.isEmpty(password)) {
            password_edit_box.setError("Password can't be empty");
            return false;
        } else if (password.length() < 5) {
            password_edit_box.setError("Password must be at least 5 characters");
            return false;
        } else if (TextUtils.isEmpty(activationCode)) {
            activation_code.setError("Code can't be empty");
            return false;
        } else if (!isValidActivationCode(activationCode)) {
            activation_code.setError("Invalid activation code");
            return false;
        }

        return true;
    }

    private boolean isValidActivationCode(String activationCode) {
        String[] validCodes = {"cst*1st*sgp", "cst*2nd*sgp", "cst*3rd*sgp",
                "ee*1st*sgp", "ee*2nd*sgp", "ee*3rd*sgp",
                "ce*1st*sgp", "ce*2nd*sgp", "ce*3rd*sgp",
                "arc*1st*sgp", "arc*2nd*sgp", "arc*3rd*sgp",
                "etc*1st*sgp", "etc*2nd*sgp", "etc*3rd*sgp",
                "eie*1st*sgp", "eie*2nd*sgp", "eie*3rd*sgp"};

        for (String code : validCodes) {
            if (activationCode.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void isEmailAlreadyRegistered(String email, final EmailCheckCallback callback) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        Query emailQuery = usersRef.orderByChild("email").equalTo(email);
        emailQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                callback.onCallback(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onCallback(false);
            }
        });
    }

    private void isPhoneNumberAlreadyRegistered(String phone_no, final PhoneNumberCheckCallback callback) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        Query phoneQuery = usersRef.orderByChild("phone_no").equalTo(phone_no);
        phoneQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                callback.onCallback(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onCallback(false);
            }
        });
    }

    private interface EmailCheckCallback {
        void onCallback(boolean isEmailRegistered);
    }

    private interface PhoneNumberCheckCallback {
        void onCallback(boolean isPhoneRegistered);
    }

    private void togglePasswordVisibility() {
        if (password_edit_box.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            password_edit_box.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            password_edit_box.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        password_edit_box.setSelection(password_edit_box.length());
    }
}
