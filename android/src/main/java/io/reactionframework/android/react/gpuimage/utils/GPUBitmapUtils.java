package io.reactionframework.android.react.gpuimage.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.FileOutputStream;
import java.io.IOException;

public class GPUBitmapUtils {
    private static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getImageFromPath(String path, int maxWidth, int maxHeight) {
        Bitmap bitmap = new BitmapFromPathDecoder(path, maxWidth, maxHeight).decode();

        int orientation = ExifInterface.ORIENTATION_UNDEFINED;
        try {
            ExifInterface exif = new ExifInterface(path);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rotateBitmap(bitmap, orientation);
    }

    public static void saveImageToPath(Bitmap bitmap, String path) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (out != null) {
                out.close();
            }
            throw e;
        }
    }

    private static abstract class BitmapDecoder {
        private final int mMaxWidth;
        private final int mMaxHeight;

        public BitmapDecoder(int maxWidth, int maxHeight) {
            mMaxWidth = maxWidth;
            mMaxHeight = maxHeight;
        }

        protected abstract Bitmap decode(BitmapFactory.Options options);

        public Bitmap decode() {
            if (mMaxWidth <= 0 && mMaxHeight <= 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                return decode(options);
            }

            // Decode bounds only
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            decode(options);

            // Calculate scale
            int scale = 1;
            while (options.outWidth / scale > mMaxWidth ||
                    options.outHeight / scale > mMaxHeight) {
                scale++;
            }

            scale--;
            if (scale < 1) {
                scale = 1;
            }

            // Decode full scaled bitmap
            options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = decode(options);
            if (bitmap == null) {
                return null;
            }

            // Resize bitmap to required max with or max height
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            if (width <= mMaxWidth && height <= mMaxHeight) {
                return bitmap;
            }

            float newWidth = mMaxWidth;
            float newHeight = mMaxHeight;

            if ((float) width / mMaxWidth < (float) height / mMaxHeight) {
                newWidth = (newHeight / height) * width;
            } else {
                newHeight = (newWidth / width) * height;
            }

            Bitmap workBitmap = Bitmap.createScaledBitmap(bitmap, Math.round(newWidth), Math.round(newHeight), true);

            if (workBitmap != bitmap) {
                bitmap.recycle();
                bitmap = workBitmap;
            }

            return bitmap;
        }
    }

    private static class BitmapFromPathDecoder extends BitmapDecoder {
        private String mPath;

        public BitmapFromPathDecoder(String path, int maxWidth, int maxHeight) {
            super(maxWidth, maxHeight);
            mPath = path;
        }

        @Override
        protected Bitmap decode(BitmapFactory.Options options) {
            return BitmapFactory.decodeFile(mPath, options);
        }
    }
}
