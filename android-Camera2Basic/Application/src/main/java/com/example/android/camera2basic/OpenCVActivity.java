package com.example.android.camera2basic;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;

public class OpenCVActivity extends Activity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_cv);

        base = (CameraBridgeViewBase)findViewById(R.id.image_manipulations_activity_surface_view);
        base.setVisibility(View.VISIBLE);
        pixelOnTouchListener = new PixelOnTouchListener();
        processor = new ImageProcessor(pixelOnTouchListener);
        base.setCvCameraViewListener(processor);
        base.setOnTouchListener(pixelOnTouchListener);
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
}
