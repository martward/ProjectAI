package com.example.android.camera2basic;

import android.app.Activity;
import android.view.Display;
import android.view.View;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    int[] centerPoint = new int[2];

    int[][] expandDirections = {{5, 5}, {-5, 5}, {-5, -5}, {5, -5}};

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
        /*
        if (test != null) return test;
        */
        Mat rgba = inputFrame.rgba();

        Point latestPoint = pixelOnTouchListener.getLatestPoint();
        if(centerPoint[0] != 0 && centerPoint[1] != 0) {
            if (latestPoint == null) latestPoint = new Point();
            latestPoint.x = centerPoint[1];
            latestPoint.y = centerPoint[0];
        }

        upperPoint = new int[2];
        lowerPoint = new int[2];
        leftPoint = new int[2];
        rightPoint = new int[2];
        centerPoint = new int[2];

        if (latestPoint != null)
        {
            System.out.println("CLICKED AT: " + latestPoint.toString());

            int x = (int)latestPoint.x - (screenWidth - imageWidth)/2;
            int y = (int)latestPoint.y;

            Mat rgb = new Mat(rgba.size(), CvType.CV_8UC3);
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB);

            Mat dst = new Mat();
            rgb.copyTo(dst);

            int maxDist = 15;
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

            centerPoint[0] = upperPoint[0]/2 + lowerPoint[0]/2;
            centerPoint[1] = leftPoint[1]/2 + rightPoint[1]/2;

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

            drawOpenCVCube(rgb);

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

    private void drawOpenCVCube(Mat img) {
        float xScale = 10.f/21.0f;
        float yScale = 10.f/29.0f;

        if( dist(leftPoint,upperPoint) < dist(rightPoint,upperPoint)) {
            float temp = yScale;
            yScale = xScale;
            xScale = temp;
        }

        System.out.println( "Scales: " + xScale + ", " + yScale);

        int x1 = upperPoint[1];
        int y1 = upperPoint[0];

        System.out.println( "P1: (" + x1 + ", " + y1 );



        int x2 = x1 + (int)(xScale*(rightPoint[1] - upperPoint[1]));
        int y2 = y1 + (int)(xScale*(rightPoint[0] - upperPoint[0]));

        System.out.println( "P2: (" + x2 + ", " + y2 );

        int x3 = x2 + (int)(yScale*(leftPoint[1] - upperPoint[1]));
        int y3 = y2 + (int)(yScale*(leftPoint[0] - upperPoint[0]));

        System.out.println( "P4: (" + x3 + ", " + y3 );

        int x4 = x1 + (int)(yScale*(leftPoint[1] - upperPoint[1]));
        int y4 = y1 + (int)(yScale*(leftPoint[0] - upperPoint[0]));

        int cubeHeight = (int)(dist(x1,y1,x2,y2));

        // TODO determine vertical axis with headTransform;
        int x5 = x1;
        int y5 = y1 - cubeHeight;

        int x6 = x2;
        int y6 = y2 - cubeHeight;

        int x7 = x3;
        int y7 = y3 - cubeHeight;

        int x8 = x4;
        int y8 = y4 - cubeHeight;

        System.out.println( "P4: (" + x4 + ", " + y4 );

        Point point1 = new Point(x1,y1);
        Point point2 = new Point(x2,y2);
        Point point3 = new Point(x3,y3);
        Point point4 = new Point(x4,y4);
        Point point5 = new Point(x5,y5);
        Point point6 = new Point(x6,y6);
        Point point7 = new Point(x7,y7);
        Point point8 = new Point(x8,y8);

        List<MatOfPoint> plane = new ArrayList<>(4);

        plane.add(new MatOfPoint(point3, point4, point8, point7));
        Imgproc.fillPoly(img, plane, new Scalar(0,0,255));
        plane.clear();

        plane.add(new MatOfPoint(point2, point3, point7, point6));
        Imgproc.fillPoly(img, plane, new Scalar(0,255,0));
        plane.clear();

        plane.add(new MatOfPoint(point5, point6, point7, point8));
        Imgproc.fillPoly(img, plane, new Scalar(255,0,0));
    }

    private float dist(int[] p1, int[] p2) {
        return dist(p1[0], p1[1], p2[0], p2[1]);
    }

    private float dist(int x1, int y1, int x2, int y2) {
        return (float)Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
}
