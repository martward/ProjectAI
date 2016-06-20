package com.example.android.camera2basic;

import android.view.View;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Sébastien Negrijn on 20-6-16.
 */
public class ImageProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final OpenCVActivity.PixelOnTouchListener pixelOnTouchListener;

    public ImageProcessor(OpenCVActivity.PixelOnTouchListener pixelListener) {
        this.pixelOnTouchListener = pixelListener;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();

        Point latestPoint = pixelOnTouchListener.getLatestPoint();
        if( latestPoint != null)
        {
            System.out.println("CLICKED AT: " + latestPoint.toString());
            /*
            int x = (int)latestPoint.x;
            int y = (int)latestPoint.y;

            Mat rgb = new Mat(rgba.size(), CvType.CV_8UC3);
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB);

            Mat roi = new Mat(rgb.size(), CvType.CV_32SC1);
            int[] pixel = {0,0,1};

            roi.put(y, x, pixel);

            if(rgb.type() == CvType.CV_8UC1 ) System.out.println("rgb correct 1");
            if(rgb.type() == CvType.CV_8UC2 ) System.out.println("rgb correct 2");
            if(rgb.type() == CvType.CV_8UC3 ) System.out.println("rgb correct 3");
            if(rgb.type() == CvType.CV_8UC4 ) System.out.println("rgb correct 4");
            if(roi.type() == CvType.CV_32SC1 ) System.out.println("roi correct");

            System.out.println("rgb depth: " + rgb.depth());
            System.out.println("roi depth: " + roi.depth() + ", " + CvType.CV_8U + ", " + CvType.CV_16U + ", " + CvType.CV_32F + ", ");

            System.out.println(rgb.size() + "   " + roi.size());

            Imgproc.watershed(rgb, roi);

            Imgproc.cvtColor(roi, rgb, Imgproc.COLOR_GRAY2RGB);


            return rgb;
            */
        }

        return rgba;
    }
}
