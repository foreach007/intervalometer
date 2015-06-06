//from http://stackoverflow.com/questions/13772242/android-camera-previewcallback-not-called-in-4-1
package balazs.intervalometer;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

public class CameraPreview implements SurfaceHolder.Callback,
        Camera.PreviewCallback {
    static final String TAG = "CAMERAPREVIEW";
    int PreviewSizeWidth;
    int PreviewSizeHeight;
    SurfaceHolder mSurfHolder;
    Camera mCamera;

    public CameraPreview(int PreviewlayoutWidth, int PreviewlayoutHeight) {
        PreviewSizeWidth = PreviewlayoutWidth;
        PreviewSizeHeight = PreviewlayoutHeight;
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        Camera.Parameters parameters;
        mSurfHolder = arg0;

        parameters = mCamera.getParameters();
        parameters.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    public void surfaceCreated(SurfaceHolder arg0) {
        mCamera = Camera.open();
        try {
            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(arg0);
            mCamera.setPreviewCallback(this);
            Camera.Parameters p = mCamera.getParameters();
            int maxId = 0, maxW = 0;
            List<Camera.Size> sizes = p.getSupportedPictureSizes();
            for (int i = 0; i < sizes.size(); i++) {
                if (sizes.get(i).width > maxW) {
                    maxId = i;
                    maxW = sizes.get(i).width;
                }
            }
            p.setPictureSize(sizes.get(maxId).width, sizes.get(maxId).height);
            p.setRotation(90);
            mCamera.setDisplayOrientation(90);
            Log.i(TAG, "Supported Exposure Modes:" + p.get("exposure-mode-values"));
            Log.i(TAG, "Supported White Balance Modes:" + p.get("whitebalance-values"));
            Log.i(TAG, "Exposure setting = " + p.get("exposure"));
            Log.i(TAG, "White Balance setting = " + p.get("whitebalance"));
            Log.i(TAG, "ALL PARAMS = " + p.flatten());
            p.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);
            mCamera.setParameters(p);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void takePicture(Camera.PictureCallback jpegCallback) {
        mCamera.stopPreview();
        mCamera.takePicture(null, null, jpegCallback);
    }

    public void resumePreview() {
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
}
