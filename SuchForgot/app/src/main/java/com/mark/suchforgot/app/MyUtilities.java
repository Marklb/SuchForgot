package com.mark.suchforgot.app;

import android.content.res.Resources;
import android.util.Log;


public class MyUtilities{

    /*
        This is just a class that I put methods I commonly use in apps
        so I don't have to rewrite or find them when making a quick app.
     */

    public static double getAngle(double xStart, double yStart, double xEnd, double yEnd){
        double xDiff = xEnd - xStart;
        double yDiff = yEnd - yStart;
        double angle = Math.atan2(yDiff,  xDiff);

        return angle;
    }

    public static int dpToPx(int dp){
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px){
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    // x,y is the point to test
    // cx, cy is circle center, and radius is circle radius
    public static boolean pointInCircle(float x, float y, float cx, float cy, float radius) {
        float distancesquared = (x - cx) * (x - cx) + (y - cy) * (y - cy);
        return distancesquared <= radius * radius;
    }

    //Circle and Circle
    public static boolean circleIntersect(float c1x, float c1y, float c1Radius, float c2x, float c2y, float c2Radius){
        float distanceX = c2x - c1x;
        float distanceY = c2y - c1y;

        double magnitude = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

        return magnitude < c1Radius + c2Radius;
    }

    //Circle and Rectangle
    public static boolean circleRectangleIntersect(float cx, float cy, float cr, float rx, float ry, float rw, float rh){
        float circle_distance_x = Math.abs(cx - rx - rw/2);
        float circle_distance_y = Math.abs(cy - ry - rh/2);

        if (circle_distance_x > (rw/2 + cr)) { return false; }
        if (circle_distance_y > (rh/2 + cr)) { return false; }

        if (circle_distance_x <= (rw/2)) { return true; }
        if (circle_distance_y <= (rh/2)) { return true; }

        float corner_distance_sq = (float)(Math.pow(circle_distance_x - rw/2, 2) + Math.pow(circle_distance_y - rh/2, 2));

        return corner_distance_sq <= Math.pow(cr, 2);
    }

    public static void Log_i(String type, String msg, boolean enabled){
        if(enabled)Log.i(type, msg);
    }
    public static void Log_i(String type, String msg, Throwable t, boolean enabled){
        if(enabled)Log.i(type, msg, t);
    }
    public static void Log_e(String type, String msg, boolean enabled){
        if(enabled)Log.e(type, msg);
    }
    public static void Log_e(String type, String msg, Throwable t, boolean enabled){
        if(enabled)Log.e(type, msg, t);
    }

}
