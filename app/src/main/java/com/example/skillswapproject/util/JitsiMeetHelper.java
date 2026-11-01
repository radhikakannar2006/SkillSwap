package com.example.skillswapproject.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class JitsiMeetHelper {

    public static void launchJitsiMeet(Context context, String meetUrl) {
        if (meetUrl == null || meetUrl.isEmpty()) {
            Toast.makeText(context, "No Jitsi Meet link provided", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(meetUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open Jitsi Meet", Toast.LENGTH_SHORT).show();
        }
    }
}
