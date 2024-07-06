package com.nabarup.college;

import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.OnProgressListener;

import java.io.File;
import java.util.ArrayList;

public class UploadedFilesAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<UploadedFile> mFilesList;
    private boolean isAdmin;

    public UploadedFilesAdapter(Context context, ArrayList<UploadedFile> filesList, boolean isAdmin) {
        this.mContext = context;
        this.mFilesList = filesList;
        this.isAdmin = isAdmin;
    }

    @Override
    public int getCount() {
        return mFilesList.size();
    }

    @Override
    public UploadedFile getItem(int position) {
        return mFilesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.grid_item_uploaded_file, parent, false);
            holder = new ViewHolder();
            holder.imageViewIcon = convertView.findViewById(R.id.imageView_icon);
            holder.imageViewDelete = convertView.findViewById(R.id.imageView_delete);
            holder.imageViewDownload = convertView.findViewById(R.id.imageView_download);
            holder.progressBar = convertView.findViewById(R.id.progressBar);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        UploadedFile uploadedFile = getItem(position);

        if (uploadedFile != null) {
            if (uploadedFile.getFileType().equals("image")) {
                // Load image using Glide
                // Load image using Glide
                Glide.with(mContext)
                        .load(uploadedFile.getUrl()) // Make sure uploadedFile.getUrl() returns a valid URL
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.imageViewIcon);

            } else {
                holder.imageViewIcon.setImageResource(R.drawable.ic_pdf); // Set PDF icon
            }

            holder.imageViewIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFileInBrowser(uploadedFile.getUrl());
                }
            });

            if (isAdmin) {
                holder.imageViewDelete.setVisibility(View.VISIBLE);
                holder.imageViewDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteFile(position, uploadedFile);
                    }
                });
            } else {
                holder.imageViewDelete.setVisibility(View.GONE);
            }

            holder.imageViewDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    downloadFile(uploadedFile);
                }
            });
        }

        return convertView;
    }


    private void deleteFile(int position, UploadedFile uploadedFile) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(uploadedFile.getUrl());

        storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mFilesList.remove(position);
                notifyDataSetChanged();
                Toast.makeText(mContext, "File deleted successfully", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(mContext, "Failed to delete file", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFile(UploadedFile uploadedFile) {
        File localFile = new File(mContext.getExternalFilesDir(null), uploadedFile.getFileName());

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(uploadedFile.getUrl());

        storageRef.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(mContext, "File downloaded successfully", Toast.LENGTH_SHORT).show();
                        openDownloadedFile(localFile);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, "Failed to download file", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        // TODO: Update progress if needed
                    }
                });
    }

    private void openDownloadedFile(File file) {
        Uri uri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, "No application available to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileInBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, "No application available to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private static class ViewHolder {
        ImageView imageViewIcon;
        ImageView imageViewDelete;
        ImageView imageViewDownload;
        ProgressBar progressBar;
    }
}
