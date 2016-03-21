package io.reactionframework.android.react.gpuimage.filters;

import android.graphics.Bitmap;

public interface GPUBitmapFilter {
    Bitmap getFilteredBitmap(Bitmap image);
}
