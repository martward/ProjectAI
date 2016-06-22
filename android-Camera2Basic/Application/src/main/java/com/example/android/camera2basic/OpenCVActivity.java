package com.example.android.camera2basic;

import android.app.Activity;
import android.opengl.GLES20;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenCVActivity extends GvrActivity implements GvrView.StereoRenderer {

    ImageProcessor processor;
    PixelOnTouchListener pixelOnTouchListener;
    CameraBridgeViewBase base;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    base.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_cv);

        base = (CameraBridgeViewBase)findViewById(R.id.image_manipulations_activity_surface_view);
        base.setVisibility(View.VISIBLE);
        pixelOnTouchListener = new PixelOnTouchListener();
        processor = new ImageProcessor(pixelOnTouchListener, this);
        base.setCvCameraViewListener(processor);
        base.setOnTouchListener(pixelOnTouchListener);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setSettingsButtonEnabled(false);
        gvrView.setVRModeEnabled(false);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);
        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);
        setGvrView(gvrView);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {

            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (base != null)
            base.disableView();
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        float[] angles = new float[3];
        headTransform.getEulerAngles(angles, 0);
        //System.out.println(angles[0] + ", " + angles[1] + ", " + angles[2]);
    }

    @Override
    public void onDrawEye(Eye eye) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        System.out.println("Surface created");
        GLES20.glClearColor(1.0f,1.0f,1.0f,0);
        GLES20.glEnable(GL10.GL_CULL_FACE);
    }

    @Override
    public void onRendererShutdown() {

    }

    class PixelOnTouchListener implements View.OnTouchListener {

        private boolean read = false;
        private Point clickedPoint;

        Point getLatestPoint() {
            if(read) return null;

            read = true;
            return clickedPoint;
        }

        private void setLatestClickedPoint(double x, double y) {
            read = false;
            clickedPoint = new Point(x, y);
        }

        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float y = event.getY();

                setLatestClickedPoint(x, y);
                return true;
            }
            return false;
        }
    }
}
