package com.example.skillswapproject.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.example.skillswapproject.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Universal image loading utility for profile pictures.
 * Supports Base64 strings (stored in Firestore), HTTP/HTTPS URLs, and fallback drawable resources.
 */
public class ImageLoaderHelper {

    private static final String TAG = "ImageLoaderHelper";
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void loadProfileImage(ImageView imageView, String imageSource) {
        loadProfileImage(imageView, imageSource, R.drawable.ic_profile);
    }

    public static void loadProfileImage(ImageView imageView, String imageSource, @DrawableRes int fallbackRes) {
        if (imageView == null) return;

        if (imageSource == null || imageSource.trim().isEmpty()) {
            imageView.setImageResource(fallbackRes);
            return;
        }

        String source = imageSource.trim();

        // 1. Check if it's a Base64 image
        if (source.startsWith("data:image") || source.length() > 200 && !source.startsWith("http")) {
            try {
                String base64Data = source;
                if (source.contains(",")) {
                    base64Data = source.substring(source.indexOf(",") + 1);
                }
                byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to decode Base64 image", e);
            }
            imageView.setImageResource(fallbackRes);
            return;
        }

        // 2. Check if it's a web URL
        if (source.startsWith("http://") || source.startsWith("https://")) {
            imageView.setImageResource(fallbackRes);
            executor.execute(() -> {
                Bitmap bitmap = null;
                try {
                    URL url = new URL(source);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    bitmap = BitmapFactory.decodeStream(is);
                    is.close();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to load image from URL: " + source, e);
                }

                final Bitmap finalBitmap = bitmap;
                mainHandler.post(() -> {
                    if (finalBitmap != null && imageView != null) {
                        imageView.setImageBitmap(finalBitmap);
                    }
                });
            });
            return;
        }

        // Fallback default
        imageView.setImageResource(fallbackRes);
    }
}
