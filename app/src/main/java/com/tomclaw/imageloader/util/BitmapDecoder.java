package com.tomclaw.imageloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.support.annotation.Nullable;

import com.tomclaw.imageloader.core.Decoder;
import com.tomclaw.imageloader.core.Result;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapDecoder implements Decoder {
    @Override
    public boolean probe(File file) {
        try(InputStream inputStream = new FileInputStream(file)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            return true;
        }
        catch(Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @Override
    public @Nullable Result decode(File file, int width, int height) {
        Bitmap bitmap = null;
        try(InputStream inputStream = new FileInputStream(file)) {
            bitmap = decodeSampledBitmapFromStream(inputStream, width, height);
            int rotation = getRotation(file);
            if(bitmap != null && rotation != 0) {
                Matrix m = new Matrix();
                m.setRotate(
                        (float)rotation,
                        (float)(bitmap.getWidth() / 2),
                        (float)(bitmap.getHeight() / 2)
                );
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);
            }
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
        return bitmap != null ? new BitmapResult(bitmap) : null;
    }

    private @Nullable Bitmap decodeSampledBitmapFromStream(
            InputStream stream,
            int reqWidth,
            int reqHeight) {
        try {
            InputStream inputStream = new BufferedInputStream(stream, THUMBNAIL_BUFFER_SIZE);
            inputStream.mark(THUMBNAIL_BUFFER_SIZE);

            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            // Calculate inSampleSize
            float widthSample = (float)(options.outWidth / reqWidth);
            float heightSample = (float)(options.outHeight / reqHeight);
            float scaleFactor = Math.max(widthSample, heightSample);
            if(scaleFactor < 1.0f) scaleFactor = 1.0f;
            options.inJustDecodeBounds = false;
            options.inSampleSize = (int)scaleFactor;
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            // Decode bitmap with inSampleSize set
            inputStream.reset();
            return BitmapFactory.decodeStream(inputStream, null, options);
        }
        catch(Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static int getRotation(File file) {
        switch(obtainFileOrientation(file.getAbsolutePath())) {
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_TRANSPOSE:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
            case ExifInterface.ORIENTATION_TRANSVERSE:
                return 270;
            default:
                return 0;
        }
    }

    private static int obtainFileOrientation(String fileName) {
        try {
            return new ExifInterface(fileName).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        }
        catch(IOException e) {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
    }

    static class BitmapResult implements Result {

        private final Bitmap bitmap;

        public BitmapResult(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public int getByteCount() {
            return bitmap.getByteCount();
        }

        @Override
        public boolean isRecycled() {
            return bitmap.isRecycled();
        }

        @Override
        public Drawable getDrawable() {
            return new BitmapDrawable(null, bitmap);
        }
    }

    /**
     * Buffer is large enough to rewind past any EXIF headers.
     */
    private static final int THUMBNAIL_BUFFER_SIZE = 128 * 1024;
}
