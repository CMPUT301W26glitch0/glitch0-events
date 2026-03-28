package com.example.cmput301_app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Utility methods for encoding images to Base64 strings for Firestore storage
 * and decoding them back for display.
 *
 * Firestore documents have a 1 MB limit, so images are always scaled down and
 * compressed before encoding.
 */
public class ImageUtils {

    /**
     * Reads an image from {@code uri}, scales it to at most {@code maxDim}×{@code maxDim}
     * pixels, compresses it as JPEG at the given quality, and returns the Base64 string.
     *
     * @return Base64-encoded JPEG string, or null on failure
     */
    public static String compressToBase64(Context context, Uri uri, int maxDim, int quality) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            // Decode size first
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            // Calculate sample size
            int sampleSize = 1;
            while (opts.outWidth / sampleSize > maxDim || opts.outHeight / sampleSize > maxDim) {
                sampleSize *= 2;
            }

            // Decode scaled bitmap
            is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
            is.close();

            if (bitmap == null) return null;

            // Scale precisely if still too large
            if (bitmap.getWidth() > maxDim || bitmap.getHeight() > maxDim) {
                float scale = (float) maxDim / Math.max(bitmap.getWidth(), bitmap.getHeight());
                int w = Math.round(bitmap.getWidth() * scale);
                int h = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            byte[] bytes = out.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Loads an image into {@code imageView}. {@code data} can be either a Base64 string
     * (stored without a URL scheme) or an https:// URL (legacy data).
     *
     * @param circle if true applies a circular crop (for profile pictures)
     */
    public static void loadImage(Context context, String data, ImageView imageView, boolean circle) {
        if (data == null || data.isEmpty()) return;

        if (data.startsWith("http://") || data.startsWith("https://")) {
            // Legacy: stored as a Firebase Storage download URL
            var req = Glide.with(context).load(data);
            if (circle) req.circleCrop().into(imageView);
            else req.into(imageView);
        } else {
            // Base64-encoded JPEG
            byte[] bytes = Base64.decode(data, Base64.NO_WRAP);
            var req = Glide.with(context).load(bytes);
            if (circle) req.circleCrop().into(imageView);
            else req.into(imageView);
        }
    }
}
