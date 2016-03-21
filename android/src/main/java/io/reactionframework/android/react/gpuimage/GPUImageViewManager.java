package io.reactionframework.android.react.gpuimage;

import android.opengl.GLSurfaceView;
import android.util.Log;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

public class GPUImageViewManager extends SimpleViewManager<GLSurfaceView> {
    private static final String LOG_TAG = GPUImageViewManager.class.getSimpleName();

    public static final String REACT_CLASS = "RCTIONGPUImageView";
    public static final String PROP_GPU_IMAGE_ID = "gpuImageId";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected GLSurfaceView createViewInstance(ThemedReactContext themedReactContext) {
        return new GLSurfaceView(themedReactContext);
    }

    @ReactProp(name = PROP_GPU_IMAGE_ID)
    public void setGPUImageId(GLSurfaceView view, String gpuImageId) {
        Log.v(LOG_TAG, String.format("Property '%s' changed.", PROP_GPU_IMAGE_ID));
        GPUImageManager.getGPUImage(gpuImageId).setGLPreview(view);
    }
}
