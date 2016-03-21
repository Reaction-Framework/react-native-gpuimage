package io.reactionframework.android.react.gpuimage.renderer;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.OpenGlUtils;
import jp.co.cyberagent.android.gpuimage.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Queue;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

@TargetApi(11)
public class GPUImageRenderer implements GLSurfaceView.Renderer {
    static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private GPUImageFilter mImageFilter;
    private int mGLImageTextureId;
    private final FloatBuffer mGLImageBuffer;
    private final FloatBuffer mGLTextureBuffer;

    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;

    private int mOutputWidth;
    private int mOutputHeight;
    private int mImageWidth;
    private int mImageHeight;

    public GPUImageRenderer(final GPUImageFilter filter) {
        mRunOnDraw = new LinkedList<>();
        mRunOnDrawEnd = new LinkedList<>();

        mImageFilter = filter;
        mGLImageTextureId = OpenGlUtils.NO_TEXTURE;
        mGLImageBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLImageBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        mImageFilter.init();
    }

    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;

        GLES20.glViewport(0, 0, width, height);

        GLES20.glUseProgram(mImageFilter.getProgram());
        mImageFilter.onOutputSizeChanged(width, height);

        setupScaling();
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        runAll(mRunOnDraw);
        mImageFilter.onDraw(mGLImageTextureId, mGLImageBuffer, mGLTextureBuffer);
        runAll(mRunOnDrawEnd);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    public void setFilter(final GPUImageFilter filter) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                if (mImageFilter != null) {
                    mImageFilter.destroy();
                }

                mImageFilter = filter;
                mImageFilter.init();
                GLES20.glUseProgram(mImageFilter.getProgram());
                mImageFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            }
        });
    }

    public void setImageBitmap(final Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        runOnDraw(new Runnable() {
            @Override
            public void run() {
                if (mImageFilter != null) {
                    GLES20.glUseProgram(mImageFilter.getProgram());
                    mImageFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
                }

                mGLImageTextureId = deleteTexture(mGLImageTextureId);
                mGLImageTextureId = createTexture(bitmap);

                mImageWidth = bitmap.getWidth();
                mImageHeight = bitmap.getHeight();

                bitmap.recycle();

                setupScaling();
            }
        });
    }

    public void deleteImageBitmap() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                mGLImageTextureId = deleteTexture(mGLImageTextureId);
            }
        });
    }

    private void setupScaling() {
        mGLTextureBuffer.clear();
        float[] textureCords = TextureRotationUtil.getRotation(Rotation.NORMAL, false, false);
        mGLTextureBuffer.put(textureCords).position(0);

        mGLImageBuffer.clear();

        if (mOutputWidth == mImageWidth && mOutputHeight == mImageHeight) {
            mGLImageBuffer.put(CUBE).position(0);
            return;
        }

        float outputWidth = mOutputWidth;
        float outputHeight = mOutputHeight;
        float ratio1 = outputWidth / mImageWidth;
        float ratio2 = outputHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;

        mGLImageBuffer.put(new float[]{
                CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
        }).position(0);
    }

    private void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }

    public void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.add(runnable);
        }
    }

    public int createTexture(final Bitmap img) {
        int textures[] = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        return textures[0];
    }

    public int deleteTexture(int textureId) {
        if (textureId == OpenGlUtils.NO_TEXTURE) {
            return OpenGlUtils.NO_TEXTURE;
        }

        GLES20.glDeleteTextures(1, new int[]{ textureId }, 0);
        return OpenGlUtils.NO_TEXTURE;
    }

    public void destroy() {
        deleteImageBitmap();

        if (mImageFilter != null) {
            mImageFilter.destroy();
            mImageFilter = null;
        }
    }
}
