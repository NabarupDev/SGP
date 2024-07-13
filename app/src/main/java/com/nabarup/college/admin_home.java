package com.nabarup.college;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;

public class admin_home extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;
    private static final String PREFS_NAME = "com.nabarup.college.PREFS";
    private static final String KEY_UPLOADED_FILES = "uploaded_files";
    private static final long CHECK_INTERVAL = 5000;

    private FirebaseStorage storage;
    private ArrayList<UploadedFile> uploadedFiles;
    private UploadedFilesAdapter uploadedFilesAdapter;

    private Handler handler;
    private Runnable checkInternetRunnable;
    private Runnable checkFilesRunnable;
    private AlertDialog alertDialog;

    private boolean doubleBackToExitPressedOnce = false;
    private Handler backPressHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("Your_APP_ID_Here")
                    .setApiKey("Your_API_Key_Here")
                    .setDatabaseUrl("Your_FIREBASE_DATABASE_URL")
                    .setStorageBucket("Your_FIREBASE_STORAGE_BUCKET")
                    .setProjectId("Your_FIREBASE_PROJECT_ID")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }

        storage = FirebaseStorage.getInstance();

        GridView uploadedFilesGridView = findViewById(R.id.uploadedFilesGridView);
        uploadedFiles = loadUploadedFiles(); // Load files from SharedPreferences
        uploadedFilesAdapter = new UploadedFilesAdapter(this, uploadedFiles, true); // isAdmin = true for admin
        uploadedFilesGridView.setAdapter(uploadedFilesAdapter);

        uploadedFilesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openFile(uploadedFiles.get(position).getUrl());
            }
        });

        RelativeLayout uploadLayout = findViewById(R.id.upload);
        uploadLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        handler = new Handler();
        checkInternetRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isConnected(admin_home.this)) {
                    if (alertDialog == null || !alertDialog.isShowing()) {
                        alertDialog = buildDialog(admin_home.this).show();
                    }
                } else {
                    if (alertDialog != null && alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                }
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(checkInternetRunnable);

        checkFilesRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndRemoveMissingFiles();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.post(checkFilesRunnable);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "image/*"});
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    uploadFile(fileUri);
                }
            }
        }
    }

    private void uploadFile(Uri fileUri) {
        String fileType = getFileType(fileUri);
        String fileName = fileUri.getLastPathSegment();
        StorageReference storageRef = storage.getReference().child("uploaded_files/" + fileName);

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading File");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        storageRef.putFile(fileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                progressDialog.dismiss();
                                String fileUrl = uri.toString();
                                UploadedFile uploadedFile = new UploadedFile(fileUrl, fileType, fileName); // Pass fileName to UploadedFile constructor
                                uploadedFiles.add(uploadedFile);
                                uploadedFilesAdapter.notifyDataSetChanged();
                                saveUploadedFiles();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(admin_home.this, "File upload failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getFileType(Uri uri) {
        String type = getContentResolver().getType(uri);
        if (type != null) {
            if (type.startsWith("image/")) {
                return "image";
            } else if (type.equals("application/pdf")) {
                return "pdf";
            }
        }
        return "unknown";
    }

    private void openFile(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No application found to open this file type.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUploadedFiles() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        JSONArray jsonArray = new JSONArray();
        for (UploadedFile uploadedFile : uploadedFiles) {
            try {
                jsonArray.put(uploadedFile.toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        editor.putString(KEY_UPLOADED_FILES, jsonArray.toString());
        editor.apply();
    }

    private ArrayList<UploadedFile> loadUploadedFiles() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_UPLOADED_FILES, "[]");
        ArrayList<UploadedFile> files = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                files.add(UploadedFile.fromJson(jsonArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return files;
    }

    private void checkAndRemoveMissingFiles() {
        Iterator<UploadedFile> iterator = uploadedFiles.iterator();
        while (iterator.hasNext()) {
            UploadedFile file = iterator.next();
            StorageReference storageRef = storage.getReferenceFromUrl(file.getUrl());
            storageRef.getDownloadUrl().addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    iterator.remove();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            uploadedFilesAdapter.notifyDataSetChanged();
                            saveUploadedFiles();
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(checkInternetRunnable);
            handler.removeCallbacks(checkFilesRunnable);
        }
    }

    public boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public AlertDialog.Builder buildDialog(Context c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("No Internet Connection");
        builder.setMessage("You need to have Mobile Data or wifi to access this. Press ok to Exit");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        return builder;
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        backPressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }
}
