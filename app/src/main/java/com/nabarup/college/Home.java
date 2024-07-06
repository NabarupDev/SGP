package com.nabarup.college;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.GridView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashSet;

public class Home extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 123;
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds

    private GridView uploadedFilesGridView;
    private UploadedFilesAdapter uploadedFilesAdapter;
    private ArrayList<UploadedFile> uploadedFiles;
    private HashSet<String> fileUrls;

    private Handler handler;
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toast.makeText(this, "Failed to find user name", Toast.LENGTH_SHORT).show();

        uploadedFilesGridView = findViewById(R.id.uploadedFilesGridView);
        uploadedFiles = new ArrayList<>();
        fileUrls = new HashSet<>();
        uploadedFilesAdapter = new UploadedFilesAdapter(this, uploadedFiles, false); // isAdmin = false for student
        uploadedFilesGridView.setAdapter(uploadedFilesAdapter);


        handler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (checkPermissions()) {
                    loadUploadedFiles();
                }
                handler.postDelayed(this, REFRESH_INTERVAL); // Schedule the next execution
            }
        };

        if (checkPermissions()) {
            loadUploadedFiles();
            startPeriodicRefresh();
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPeriodicRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPeriodicRefresh();
    }

    private void startPeriodicRefresh() {
        handler.post(refreshRunnable);
    }

    private void stopPeriodicRefresh() {
        handler.removeCallbacks(refreshRunnable);
    }

    private boolean checkPermissions() {
        int permissionInternet = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionReadStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        return permissionInternet == PackageManager.PERMISSION_GRANTED &&
                permissionWriteStorage == PackageManager.PERMISSION_GRANTED &&
                permissionReadStorage == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.INTERNET,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                },
                REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Permissions granted
                loadUploadedFiles();
                startPeriodicRefresh();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void loadUploadedFiles() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("uploaded_files");

        storageRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        // Clear the list and hash set before loading new files
                        uploadedFiles.clear();
                        fileUrls.clear();

                        for (StorageReference item : listResult.getItems()) {
                            item.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String fileUrl = uri.toString();
                                    if (!fileUrls.contains(fileUrl)) {
                                        fileUrls.add(fileUrl);
                                        String fileName = item.getName();
                                        String fileType = getFileType(fileName);
                                        UploadedFile uploadedFile = new UploadedFile(fileUrl, fileType, fileName);
                                        uploadedFiles.add(uploadedFile);
                                        uploadedFilesAdapter.notifyDataSetChanged();
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(Home.this, "Failed to retrieve file URL", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Home.this, "Failed to list uploaded files", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getFileType(String fileName) {
        // Determine file type based on file extension
        String fileType = "unknown";
        if (fileName.endsWith(".pdf")) {
            fileType = "pdf";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
            fileType = "image";
        }
        return fileType;
    }

    private void showPermissionDeniedDialog() {
        Toast.makeText(this, "Permissions are required to use this feature", Toast.LENGTH_SHORT).show();

        // Example: You can also show a dialog explaining the need for permissions
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions Required")
                .setMessage("Please grant the necessary permissions to continue.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Request permissions again if needed
                        requestPermissions();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle cancellation
                    }
                })
                .show();
    }
}
