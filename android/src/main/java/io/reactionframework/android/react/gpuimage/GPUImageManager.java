package io.reactionframework.android.react.gpuimage;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReadableMap;

import io.reactionframework.android.react.gpuimage.utils.GPUBitmapUtils;
import io.reactionframework.android.react.gpuimage.utils.GPUFilterUtils;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;

import java.util.HashMap;
import java.util.Map;

public class GPUImageManager extends ReactContextBaseJavaModule {
    private static final String LOG_TAG = GPUImageManager.class.getSimpleName();
    private static final String REACT_MODULE = "RCTIONGPUImageManager";

    private static Map<String, GPUImage> sGPUImages;

    private static Map<String, GPUImage> getGPUImages() {
        return sGPUImages == null ? (sGPUImages = new HashMap<>()) : sGPUImages;
    }

    private final ReactApplicationContext mContext;

    public GPUImageManager(ReactApplicationContext reactContext) {
        super(reactContext);

        mContext = reactContext;
    }

    @Override
    public String getName() {
        return REACT_MODULE;
    }

    private GPUImage createGPUImage() {
        GPUImage gpuImage = new GPUImage(mContext);
        getGPUImages().put(gpuImage.getId(), gpuImage);
        return gpuImage;
    }

    public static GPUImage getGPUImage(String id) {
        return getGPUImages().get(id);
    }

    private void releaseGPUImage(String id) {
        if (getGPUImages().containsKey(id)) {
            GPUImage gpuImage = getGPUImage(id);
            gpuImage.release();
            getGPUImages().remove(id);
        }
    }

    private void rejectWithException(Promise promise, Throwable exception) {
        String message = exception.getMessage();
        if (!TextUtils.isEmpty(message)) {
            Log.e(LOG_TAG, exception.getMessage());
        } else {
            Log.e(LOG_TAG, "GPUImage unknown exception.");
        }

        exception.printStackTrace();
        promise.reject(exception);
    }

    private ReadableMap getParams(ReadableMap options) {
        return options.getMap("params");
    }

    @ReactMethod
    public void create(final ReadableMap options, final Promise promise) {
        GPUImage gpuImage = createGPUImage();

        try {
            if (!options.hasKey("path")) {
                throw new IllegalArgumentException("GPUImage requires path argument.");
            }

            String path = options.getString("path");
            int maxWidth = options.hasKey("maxWidth") ? options.getInt("maxWidth") : 0;
            int maxHeight = options.hasKey("maxHeight") ? options.getInt("maxHeight") : 0;

            Bitmap bitmap = GPUBitmapUtils.getImageFromPath(path, maxWidth, maxHeight);
            gpuImage.setImage(bitmap);

            promise.resolve(gpuImage.getId());
        } catch (Exception e) {
            releaseGPUImage(gpuImage.getId());
            rejectWithException(promise, e);
        }
    }

    @ReactMethod
    public void save(final ReadableMap options, final Promise promise) {
        try {
            GPUImage gpuImage = getGPUImage(options.getString("id"));

            ReadableMap params = getParams(options);
            if (!params.hasKey("path")) {
                throw new IllegalArgumentException("GPUImage save requires path argument.");
            }

            String path = params.getString("path");
            Bitmap bitmap = gpuImage.getFilteredImage();

            if (bitmap == null) {
                throw new IllegalArgumentException("GPUImage could not create image to save.");
            }

            GPUBitmapUtils.saveImageToPath(bitmap, path);
            bitmap.recycle();

            promise.resolve(null);
        } catch (Exception e) {
            rejectWithException(promise, e);
        }
    }

    @ReactMethod
    public void getImageSize(final ReadableMap options, final Promise promise) {
        try {
            GPUImage gpuImage = getGPUImage(options.getString("id"));

            WritableNativeMap output = new WritableNativeMap();
            output.putDouble("width", gpuImage.getImageWidth());
            output.putDouble("height", gpuImage.getImageHeight());

            promise.resolve(output);
        } catch (Exception e) {
            rejectWithException(promise, e);
        }
    }

    @ReactMethod
    public void addFilter(final ReadableMap options, final Promise promise) {
        try {
            GPUImage gpuImage = getGPUImage(options.getString("id"));

            if (!(gpuImage.getFilter() instanceof GPUImageFilterGroup)) {
                gpuImage.setFilter(new GPUImageFilterGroup());
            }

            ReadableMap params = getParams(options);
            String filterId = params.getString("id");
            Class filterType = GPUFilterUtils.getFilterTypeForId(filterId);

            if (filterType != null) {
                GPUImageFilterGroup group = (GPUImageFilterGroup)gpuImage.getFilter();

                if (GPUFilterUtils.getFilterFromGroup(group, filterType) != null) {
                    promise.resolve(null);
                    return;
                }

                params = getParams(params);
                GPUImageFilter filter = GPUFilterUtils.getFilterForId(filterId, params);

                if (filter != null) {
                    group.addFilter(filter);
                    gpuImage.requestRender();
                    promise.resolve(null);
                    return;
                }
            }

            throw new RuntimeException("Filter could not be added to gpuImage.");
        } catch (Exception e) {
            rejectWithException(promise, e);
        }
    }

    @ReactMethod
    public void updateFilter(final ReadableMap options, final Promise promise) {
        try {
            GPUImage gpuImage = getGPUImage(options.getString("id"));

            if (gpuImage.getFilter() instanceof GPUImageFilterGroup) {
                ReadableMap params = getParams(options);
                String filterId = params.getString("id");
                Class filterType = GPUFilterUtils.getFilterTypeForId(filterId);

                if (filterType != null) {
                    GPUImageFilterGroup group = (GPUImageFilterGroup)gpuImage.getFilter();
                    GPUImageFilter filter = GPUFilterUtils.getFilterFromGroup(group, filterType);

                    if (filter != null) {
                        GPUFilterUtils.updateFilter(filter, getParams(params));
                        gpuImage.requestRender();
                        promise.resolve(null);
                        return;
                    }
                }
            }

            throw new RuntimeException("Filter could not be updated.");
        } catch (Exception e) {
            rejectWithException(promise, e);
        }
    }

    @ReactMethod
    public void releaseView(final ReadableMap options, final Promise promise) {
        try {
            GPUImage gpuImage = getGPUImage(options.getString("id"));

            if (gpuImage != null) {
                gpuImage.setGLPreview(null);
            }

            promise.resolve(null);
        } catch (Exception e) {
            rejectWithException(promise, e);
        }
    }

    @ReactMethod
    public void release(final ReadableMap options, final Promise promise) {
        try {
            releaseGPUImage(options.getString("id"));
            promise.resolve(null);
        } catch (Exception e) {
            rejectWithException(promise, e);
        }
    }
}
