package com.example.skillswapproject.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.example.skillswapproject.R;

public class NotificationBannerHelper {

    private static boolean sUserDismissedThisSession = false;

    public static void setupBanner(Activity activity, View bannerRoot, String title, String description) {
        if (activity == null || bannerRoot == null) return;

        TextView tvTitle = bannerRoot.findViewById(R.id.tvBannerTitle);
        TextView tvDesc = bannerRoot.findViewById(R.id.tvBannerDescription);
        View btnClose = bannerRoot.findViewById(R.id.btnBannerClose);

        if (tvTitle != null && title != null) {
            tvTitle.setText(title);
        }

        if (tvDesc != null && description != null) {
            String fullText = description.trim() + " Turn on";
            SpannableString spannable = new SpannableString(fullText);

            int startIndex = fullText.lastIndexOf("Turn on");
            if (startIndex != -1) {
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        openAppNotificationSettings(activity);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(Color.parseColor("#34D399"));
                        ds.setUnderlineText(false);
                        ds.setFakeBoldText(true);
                    }
                };
                spannable.setSpan(clickableSpan, startIndex, startIndex + "Turn on".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvDesc.setText(spannable);
                tvDesc.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                tvDesc.setText(description);
            }
        }

        // Tap on the banner (outside close button) also takes user to notification settings
        bannerRoot.setOnClickListener(v -> openAppNotificationSettings(activity));

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                sUserDismissedThisSession = true;
                bannerRoot.setVisibility(View.GONE);
            });
        }

        updateBannerVisibility(activity, bannerRoot);
    }

    public static void updateBannerVisibility(Context context, View bannerRoot) {
        if (context == null || bannerRoot == null) return;

        boolean enabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
        if (enabled) {
            sUserDismissedThisSession = false; // Reset if user turned it on
            bannerRoot.setVisibility(View.GONE);
        } else {
            if (sUserDismissedThisSession) {
                bannerRoot.setVisibility(View.GONE);
            } else {
                bannerRoot.setVisibility(View.VISIBLE);
            }
        }
    }

    public static void openAppNotificationSettings(Activity activity) {
        if (activity == null) return;
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
            }
            activity.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallback.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(fallback);
            } catch (Exception ignored) {}
        }
    }
}
