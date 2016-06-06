package com.company;

import javax.swing.*;
import java.awt.*;

/**
 * Created by sebastien on 6-6-16.
 */
public class GUI {

    private Point points[];
    private Point originalPoints[];

    private JFrame frame;
    private DrawablePanel panel;

    private int width = 700;
    private int height = 400;

    public GUI()
    {
        initPoints();
        panel = new DrawablePanel(width, height);
        panel.setPoints(points);

        frame = new JFrame("IMU Visualization");
        frame.setSize( width, height);
        frame.add( panel );
        frame.setVisible(true);

    }

    private void initPoints()
    {
        points = new Point[4];
        originalPoints = new Point[4];

        points[0] = new Point(0, 0, 0);
        points[1] = new Point(100, 0, 0);
        points[2] = new Point(100, 150, 0);
        points[3] = new Point(0, 150, 0);

        originalPoints[0] = points[0].clone();
        originalPoints[1] = points[1].clone();
        originalPoints[2] = points[2].clone();
        originalPoints[3] = points[3].clone();
    }

    void setRotationMatrix( Transform transform )
    {

    }
}

class DrawablePanel extends JPanel {

    private int x[];
    private int y[];

    private int width;
    private int height;

    public DrawablePanel(int width, int height)
    {
        super();
        x = new int[4];
        y = new int[4];

        this.width = width;
        this.height = height;
    }

    public void setPoints( Point points[] )
    {
        // TODO: convert to 2d projection

        // Calculate center of object
        double minX = width;
        double maxX = 0;

        double minY = height;
        double maxY = 0;

        for( int i = 0; i < 4; i++ )
        {
            Point point = points[i];
            if( point.getX() < minX ) minX = point.getX();
            if( point.getX() > maxX ) maxX = point.getX();
            if( point.getY() < minY ) minY = point.getY();
            if( point.getY() > maxY ) maxY = point.getY();
        }

        int rectWidth = (int)(maxX-minX);
        int rectHeight = (int)(maxY-minY);
        for( int i = 0; i < 4; i++ )
        {
            x[i] = (int)points[i].getX() + width / 2 - rectWidth / 2;
            y[i] = (int)points[i].getY() + height / 2 - rectHeight / 2;
        }

        this.repaint();
    }

    public void paint(Graphics g)
    {
        g.drawPolygon( x, y, 4);
    }
}

class Point
{
    private final double x;
    private final double y;
    private final double z;

    public Point(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String toString()
    {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public Point clone()
    {
        return new Point(x, y, z);
    }
}

class Transform
{
    public double pitch;
    public double roll;
    public double azimuth;
    public Mode mode;

    public enum Mode {RELATIVE, ABSOLUTE}

    public Transform() {}

    public Transform( double pitch, double roll, double azimuth)
    {
        this.pitch = pitch;
        this.roll = roll;
        this.azimuth = azimuth;
    }

    public Transform( Mode mode, double pitch, double roll, double azimuth)
    {
        this.mode = mode;
        this.pitch = pitch;
        this.roll = roll;
        this.azimuth = azimuth;
    }

    public Point[] tranformPoints(Point[] points)
    {
        Point[] transformedPoints = new Point[4];
        for( int i = 0; i < 4; i++ )
        {

        }

        return transformedPoints;
    }

    public String toString()
    {
        String modeString = "RELATIVE";
        if( mode == Mode.ABSOLUTE ) modeString = "ABSOLUTE";
        return "(" + modeString + ", " + pitch + ", " + roll + ", " + azimuth + ")";
    }
}