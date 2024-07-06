package com.nabarup.college.login;

import android.content.Context;

import com.getkeepsafe.relinker.BuildConfig;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class FirebaseManager {
    public static void init(Context context) {
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.APPLICATION_ID)
                .setApiKey("AIzaSyAfYpssU3DkjCH11hC4dfc22TWwa8bB4yA")
                .setDatabaseUrl("https://college-519f8-default-rtdb.firebaseio.com/")
                .setProjectId("college-519f8")
                .build();

        FirebaseApp.initializeApp(context, options, "com.nabarup.college");
    }
}


