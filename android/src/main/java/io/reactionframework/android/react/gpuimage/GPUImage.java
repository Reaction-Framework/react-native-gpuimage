package io.reactionframework.android.react.gpuimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import io.reactionframework.android.react.gpuimage.filters.GPUBitmapFilter;
import io.reactionframework.android.react.gpuimage.renderer.GPUImageRenderer;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.PixelBuffer;

import java.util.UUID;

public class GPUImage {
    private final String mId;
    private final GPUImageRenderer mRenderer;
    private GLSurfaceView mGLPreview;
    private Bitmap mImage;
    private GPUImageFilter mFilter;

    public GPUImage(Context context) {
        mId = UUID.randomUUID().toString();
        mFilter = new GPUImageFilter();
        mRenderer = new GPUImageRenderer(mFilter);
    }

    public String getId() {
        return mId;
    }

    public void setGLPreview(final GLSurfaceView glPreview) {
        mGLPreview = glPreview;
        if (mGLPreview != null) {
            mGLPreview.setEGLContextClientVersion(2);
            mGLPreview.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            mGLPreview.getHolder().setFormat(PixelFormat.RGBA_8888);
            mGLPreview.setRenderer(mRenderer);
            mGLPreview.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            requestRender();
        }
    }

    public void requestRender() {
        if (mGLPreview != null) {
            mRenderer.setImageBitmap(createRenderingImage());
            mGLPreview.requestRender();
        }
    }

    public void setFilter(final GPUImageFilter filter) {
        mFilter = filter;
        mRenderer.setFilter(mFilter);
        requestRender();
    }

    public void setImage(final Bitmap bitmap) {
        mImage = bitmap;
        requestRender();
    }

    public Bitmap getFilteredImage() {
        Bitmap renderingImage = createRenderingImage();
        if (renderingImage == null) {
            return null;
        }

        if (mGLPreview != null) {
            mRenderer.deleteImageBitmap();
            mRenderer.runOnDrawEnd(new Runnable() {
                @Override
                public void run() {
                    synchronized(mFilter) {
                        mFilter.destroy();
                        mFilter.notify();
                    }
                }
            });
            synchronized(mFilter) {
                requestRender();
                try {
                    mFilter.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        GPUImageRenderer renderer = new GPUImageRenderer(mFilter);

        PixelBuffer buffer = new PixelBuffer(renderingImage.getWidth(), renderingImage.getHeight());
        buffer.setRenderer(renderer);

        renderer.setImageBitmap(renderingImage);
        Bitmap renderedImage = buffer.getBitmap();
        buffer.destroy();
        renderer.destroy();

        mRenderer.setFilter(mFilter);
        requestRender();

        return renderedImage;
    }

    private Bitmap createRenderingImage() {
        if (mImage == null) {
            return null;
        }

        Bitmap renderingImage = mImage.copy(mImage.getConfig(), true);

        if (mFilter instanceof GPUBitmapFilter) {
            Bitmap filteredBitmap = ((GPUBitmapFilter) mFilter).getFilteredBitmap(renderingImage);
            renderingImage.recycle();
            renderingImage = filteredBitmap;
        } else if (mFilter instanceof GPUImageFilterGroup) {
            for (GPUImageFilter filter:((GPUImageFilterGroup)mFilter).getFilters()) {
                if (filter instanceof GPUBitmapFilter) {
                    Bitmap filteredBitmap = ((GPUBitmapFilter) filter).getFilteredBitmap(renderingImage);
                    renderingImage.recycle();
                    renderingImage = filteredBitmap;
                }
            }
        }

        return renderingImage;
    }

    public GPUImageFilter getFilter() {
        return mFilter;
    }

    public float getImageWidth() {
        return mImage.getWidth();
    }

    public float getImageHeight() {
        return mImage.getHeight();
    }

    public void release() {
        if (mImage != null) {
            mImage.recycle();
            mImage = null;
        }

        if (mGLPreview != null) {
            mGLPreview = null;
        }

        mRenderer.destroy();
    }
}
