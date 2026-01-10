package com.example.car2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.InputStream;

public class Utils {

    public static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) {
        try {
            InputStream input = context.getContentResolver().openInputStream(selectedImage);
            ExifInterface ei;
            if (android.os.Build.VERSION.SDK_INT > 23) {
                ei = new ExifInterface(input);
            } else {
                ei = new ExifInterface(selectedImage.getPath());
            }
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateBitmap(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateBitmap(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateBitmap(img, 270);
                default:
                    return img;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return img;
        }
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static String parseImgBBUrl(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.getJSONObject("data").getString("url");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap drawableToBitmap(Context context, int drawableRes) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}