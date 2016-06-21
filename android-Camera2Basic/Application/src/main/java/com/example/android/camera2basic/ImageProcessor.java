package com.example.android.camera2basic;

import android.app.Activity;
import android.view.Display;
import android.view.View;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.Queue;
import java.util.Stack;

/**
 * Created by Sébastien Negrijn on 20-6-16.
 */
public class ImageProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final OpenCVActivity.PixelOnTouchListener pixelOnTouchListener;
    Mat test;
    int imageWidth; // in landscape mode
    int screenWidth; // in landscape mode

    int[] upperPoint = new int[2];
    int[] lowerPoint = new int[2];
    int[] leftPoint = new int[2];
    int[] rightPoint = new int[2];

    int[][] expandDirections = {{2, 2}, {-2, 2}, {-2, -2}, {2, -2}};

    public ImageProcessor(OpenCVActivity.PixelOnTouchListener pixelListener, Activity activity) {
        this.pixelOnTouchListener = pixelListener;
        Display display = activity.getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        screenWidth = (int)size.x;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        imageWidth = width;
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (test != null) return test;
        Mat rgba = inputFrame.rgba();

        Point latestPoint = pixelOnTouchListener.getLatestPoint();
        if( latestPoint != null)
        {
            System.out.println("CLICKED AT: " + latestPoint.toString());

            int x = (int)latestPoint.x - (screenWidth - imageWidth)/2;
            int y = (int)latestPoint.y;

            Mat rgb = new Mat(rgba.size(), CvType.CV_8UC3);
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB);

            Mat dst = new Mat();
            rgb.copyTo(dst);

            int maxDist = 20;
            double[] pixelColor = rgb.get(y,x);

            upperPoint[0] = rgb.rows();
            leftPoint[1] = rgb.cols();

            double[] color = {0.0,255.0,0.0};

            for( int[] direction : expandDirections ) {

                System.out.println("x");
                Stack<int[]> stack = new Stack<>();
                int[] originalPoint = {y+direction[0],x+direction[1]};
                stack.push(originalPoint);

                while (!stack.empty()) {
                    int[] point = stack.pop();

                    if(point[0] < upperPoint[0]) upperPoint = point;
                    if(point[0] > lowerPoint[0]) lowerPoint = point;
                    if(point[1] < leftPoint[1]) leftPoint = point;
                    if(point[1] > rightPoint[1]) rightPoint = point;

                    pixelColor = rgb.get(point[0],point[1]);
                    if(isSameColor(pixelColor, color)) continue;

                    if(pixelColor.equals(color)) System.out.println("already passed");
                    int newX;
                    int newY;
                    if (point[1] + direction[1] > 0 && point[1] + direction[1] < rgb.cols()) {
                        newY = point[0];
                        newX = point[1] + direction[1];
                        int distance = getColorDistance(pixelColor, rgb.get(newY, newX));
                        //System.out.println("dist: " + distance);
                        if (distance < maxDist || isSameColor(pixelColor, color)) {
                            int[] newPoint = {newY, newX};
                            stack.push(newPoint);
                        }
                    }
                    if (point[0] + direction[0] > 0 && point[0] + direction[0] < rgb.rows()) {
                        newY = point[0] + direction[0];
                        newX = point[1];
                        int distance = getColorDistance(pixelColor, rgb.get(newY, newX));
                        //System.out.println("dist: " + distance);
                        if (distance < maxDist || isSameColor(pixelColor, color)) {
                            int[] newPoint = {newY, newX};
                            stack.push(newPoint);
                        }
                    }
                    if (point[0] + direction[0] > 0 && point[1] + direction[1] > 0
                            && point[0] + direction[0] < rgb.rows() && point[1] + direction[1] < rgb.cols()) {
                        newY = point[0] + direction[0];
                        newX = point[1] + direction[1];
                        int distance = getColorDistance(pixelColor, rgb.get(newY, newX));
                        //System.out.println("dist: " + distance);
                        if (distance < maxDist || isSameColor(pixelColor, color)) {
                            int[] newPoint = {newY, newX};
                            stack.push(newPoint);
                        }
                    }
                    rgb.put(point[0], point[1], color);
                }
            }

            Scalar colorScalar = new Scalar(255, 0, 255);
            int radius = 20;
            Point center;
            center = new Point(upperPoint[1], upperPoint[0]);
            Imgproc.circle(rgb, center, radius, colorScalar );

            center = new Point(lowerPoint[1], lowerPoint[0]);
            Imgproc.circle(rgb, center, radius, colorScalar );

            center = new Point(leftPoint[1], leftPoint[0]);
            Imgproc.circle(rgb, center, radius, colorScalar );

            center = new Point(rightPoint[1], rightPoint[0]);
            Imgproc.circle(rgb, center, radius, colorScalar );

            System.out.println("found patch");

            test = rgb;
            return test;



            /*

            Mat roi = Mat.zeros(rgb.size(), CvType.CV_32SC1);

            int[] pixel = {1};
            for( int i = -20; i < 20; i++) {
                for( int j = -20; j < 20; j++ ) {
                    roi.put(y+i, x+j, pixel);
                }
            }

            // fill edge
            pixel[0] = 2;
            for( int i = 0; i < roi.rows(); i++ ) {
                roi.put(i, 0, pixel);
                roi.put(i, roi.cols()-1, pixel);
            }
            for( int i = 0; i < roi.cols(); i++ ) {
                roi.put(0, i, pixel);
                roi.put(roi.rows()-1, i, pixel);
            }

            Imgproc.watershed(rgb, roi);

            // Create the result image
            Mat dst = new Mat(roi.size(), CvType.CV_8UC3);
            // Fill labeled objects with random colors
            double[] color1 = {0,255,0};
            double[] color2 = {0,0,255};
            double[] color3 = {255,0,0};
            for (int i = 0; i < roi.rows(); i++)
            {
                for (int j = 0; j < roi.cols(); j++)
                {
                    double[] index = roi.get(i,j);
                    //if( index[0] != 1 && index[0] != -1 ) System.out.println(index[0]);
                    if (index[0] == 1) {
                        dst.put(i, j, color1);
                    } else if (index[0] == 2) {
                        dst.put(i, j, color2);
                    } else {
                        double[] original = rgb.get(i,j);
                        dst.put(i, j, original);
                    }
                }
            }

            test = dst;

            return rgb;
            */
        }

        return rgba;
    }

    private int getColorDistance( double[] color1, double[] color2 ) {
        int dist = 0;
        for( int i = 0; i < 3; i++ ) {
            dist += Math.abs(color1[i] - color2[i]);
        }
        return dist;
    }

    private boolean isSameColor( double[] color1, double[] color2 ) {
        for( int i = 0; i < 3; i++ ) {
            if( color1[i] != color2[i] ) return false;
        }
        return true;
    }
}
