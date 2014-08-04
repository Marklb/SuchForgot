package com.mark.suchforgot.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class TheService extends Service implements SensorEventListener{

    /*

    This is the Service which is the main purpose of this app.

    This activity will:
        - Continuously run on the device monitoring the accelerometer data.
        - If the accelerometer data is within the expected driving range, check gps.
        - Send what it knows about the accelerometer to the bound activities.
        - Set off an alarm when the vehicle has parked and been turned off.

     */

    private static final String LOG_TAG = "[TheService]"; //Filter to see logs from this class
    private static final boolean IS_DEBUGGING = true;

    private NotificationManager nm;
    private static boolean isRunning = false;
    private static ActivityMain.STATE currState = ActivityMain.STATE.IDLE;

    public static boolean isAlarming = false;

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_SET_STATUS = 3;
    public static final int MSG_SET_AVG = 4;
    public static final int MSG_SET_STATE = 5;


    private float speed = 0;
    private boolean isCheckingLocation = false;

    private LocationManager locationManager;
    private MyLocationListener locationListener;

    private Queue<Integer> lastData = new LinkedList<Integer>();
    private int N = 128;

    private int[] countXYZ = {0, 0, 0};

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private SuchLinked data = new SuchLinked();

    private double ax, ay, az = 0.0;
    private int avg = 0;

    private Queue<Double> ax_list = new LinkedList<Double>();
    private double ax_last = 0;
    private Queue<Double> ay_list = new LinkedList<Double>();
    private double ay_last = 0;
    private Queue<Double> az_list = new LinkedList<Double>();
    private double az_last = 0;

    private boolean isDriving = false;
    private long locationStartedTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        MyUtilities.Log_i(LOG_TAG, "Service Creating.", IS_DEBUGGING);
        startAccelerometerTracking();


        showNotification();
        isRunning = true;
        MyUtilities.Log_i(LOG_TAG, "Timer Started", IS_DEBUGGING);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyUtilities.Log_i(LOG_TAG, "Received start id " + startId + ": " + intent, IS_DEBUGGING);
        return START_STICKY; // run until explicitly stopped.
    }


    private void showNotification() {
        MyUtilities.Log_i(LOG_TAG, "showingNotification", IS_DEBUGGING);
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.custom_btn_arsenic, getText(R.string.service_started_notification), System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, ActivityMain.class), 0);
        notification.setLatestEventInfo(this, getText(R.string.service_label_notification), getText(R.string.service_started_notification), contentIntent);
        nm.notify(R.string.service_started_notification, notification);
    }

    public static boolean isRunning(){
        return isRunning;
    }


    @Override
    public IBinder onBind(Intent intent) {
        MyUtilities.Log_i(LOG_TAG, "Binding-onBind", IS_DEBUGGING);
        return mMessenger.getBinder();
    }



    private void sendUpdateMessageToActivity() {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                Bundle b = new Bundle();
                b.putString("strMsg", avg+"");
                Message msg = Message.obtain(null, MSG_SET_AVG);
                msg.setData(b);
                mClients.get(i).send(msg);

                b = new Bundle();
                b.putString("strMsg", currState+"");
                msg = Message.obtain(null, MSG_SET_STATE);
                msg.setData(b);
                mClients.get(i).send(msg);


            } catch (RemoteException e) {
                mClients.remove(i);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyUtilities.Log_i(LOG_TAG, "Destroying", IS_DEBUGGING);

        stopAccelerometerTracking();
        stopLocationTracking();

        nm.cancel(R.string.service_started_notification);
        MyUtilities.Log_i(LOG_TAG, "Service Stopped.", IS_DEBUGGING);
        isRunning = false;
    }


    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            MyUtilities.Log_i(LOG_TAG, "Handling Message", IS_DEBUGGING);
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }



    /*
        Service Main Function Methods
     */
    public double getAvg(Queue<Double> q){
        Iterator<Double> it = q.iterator();
        double total = 0;
        while(it.hasNext()){
            total += it.next();
        }
        total = total/q.size();
        return total;
    }

    /*
        This method makes sure that the phone is idle
            If one of the axis change to much then the phone isn't sitting idle and probably got
            picked up, so it will reset and not alert cause an alert when the frequency goes back
            into the off range
     */
    public double checkRangeXYZ(double last, double curr, int index){
        double min = last-3;
        double max = last+3;
        if(curr < max && curr > min) {
            countXYZ[index]++;
        }else{
            countXYZ[0] = 0;
            countXYZ[1] = 0;
            countXYZ[2] = 0;
            isDriving = false;
        }
        return countXYZ[index];
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(isAlarming)return;
        if (event.sensor.getType()== Sensor.TYPE_ACCELEROMETER){
            ax=(float)event.values[0];//If x is 9, phone is held landscape
            ay=(float)event.values[1];//If y is 9, phone is held portrait
            az=(float)event.values[2];//If z is 9, phone is laying flat


            //Only need to use the result of the check on one of the axis because if any
            //  of them were out of range they all would have reset, so the last one
            //  would not be high enough if either of the others we our of range
            //This probably would have been fine combined into one method or only checking
            //  one axis, but it is just a small quick little check so we just left it.
            checkRangeXYZ(ax_last, ax, 0);
            checkRangeXYZ(ay_last, ay, 1);
            double count = checkRangeXYZ(az_last, az, 2);

            if(!isCheckingLocation) {
                if (count > 500 && currState == ActivityMain.STATE.IGNORING) {
                    startLocationTracking();
                    locationStartedTime = System.currentTimeMillis();
                }
            }else{
                if(System.currentTimeMillis() - locationStartedTime > 10000){
                    stopLocationTracking();
                }
                //If the speed is greater than 5 MPH then the phone is probably in a moving vehicle
                if(speed >= 5){
                    stopLocationTracking();
                    isDriving = true;
                }
            }

            if(ax_list.size() > 10)ax_list.remove();
            if(ay_list.size() > 10)ay_list.remove();
            if(az_list.size() > 10)az_list.remove();
            ax_last = ax;
            ay_last = ay;
            az_last = az;

            double accel = Math.sqrt((ax*ax)+(ay*ay)+(az*az));
            data.add(accel);
            doFFT();
            sendUpdateMessageToActivity();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}


    private void startAccelerometerTracking(){
        mSensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }
    private void stopAccelerometerTracking(){
        mSensorManager.unregisterListener(this);
    }


    private void doFFT(){
        if(data.size() > N){
            double[] x = new double[N];
            double[] y = new double[N];
            SuchLinked.SuchIterator it = data.head();
            for(int i = 0; i < x.length; i++){
                x[i] = it.next();
                y[i] = 0;
            }
            data.removeFirstHalf();
            //Perform FFT on the last N points
            FFT fft = new FFT(N);
            fft.fft(x, y);

            int n = N/2;
            double[] res = new double[n];
            double total = 0;
            //Get the magnitude of the frequency data points
            double last = Math.sqrt((x[1]*x[1])+(y[1]*y[1]));
            for(int i = 2; i < n; i++){
                res[i] = Math.sqrt((x[i]*x[i])+(y[i]*y[i]));
                total += Math.abs(last - res[i]);
                last = res[i];
            }
            avg = (int)total;
            lastData.add((int)total);
            if(lastData.size() > 5)lastData.remove();
            Iterator<Integer> iter = lastData.iterator();
            double lastAvg = 0;
            while(iter.hasNext()){
                lastAvg += iter.next();
            }
            //Get the average of the collected frequencies
            lastAvg = lastAvg/lastData.size();
            if(lastAvg < 20){
                //If the average vibration frequency is less than 20 then the engine is off
                //  So, if the  person was driving before it got down to here set off the alert
                if(isDriving){
                    //TriggeringAlarm
                    MyUtilities.Log_i(LOG_TAG, "About to alarm", IS_DEBUGGING);
                    isAlarming = true;
                    startAlert();
                }
                currState = ActivityMain.STATE.OFF;
                isDriving = false;
            }else if(lastAvg < 70){
                //If the average vibration frequency is greater than 20 but less than 70
                //  then engine is on
                currState = ActivityMain.STATE.ON;
            }else if(lastAvg > 100){
                //If the average vibration frequency is greater than 100 we don't care what it is
                //  doing because we know it isn't in the range we care about so just ignore it
                currState = ActivityMain.STATE.IGNORING;
            }else{
                currState = ActivityMain.STATE.IDLE;
            }

        }
    }
    /////////////////////////////
    //GPS
    /////////////////////////////
    private void startLocationTracking(){
        //Setup the location manager
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        //Initialize the location listener
        locationListener = new MyLocationListener();

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        isCheckingLocation = true;
        speed = 0;
        MyUtilities.Log_i(LOG_TAG, "Started Location", IS_DEBUGGING);
    }
    private void stopLocationTracking(){
        try {
            locationManager.removeUpdates(locationListener);
        }catch(Exception e){}
        locationStartedTime = 0;
        isCheckingLocation = false;
    }


    public class MyLocationListener implements LocationListener {

        //called when the location service reports a change in location
        public void onLocationChanged(Location location) {

            speed = (float)(location.getSpeed()*2.2369);//MPH
        }

        //called when the provider is disabled
        public void onProviderDisabled(String provider) {}
        //called when the provider is enabled
        public void onProviderEnabled(String provider) {}
        //called when the provider changes state
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }

    ////////////////////
    // Alarm code
    ////////////////////
    public void startAlert() {
        Intent dialogIntent = new Intent(getBaseContext(), ActivityAlarming.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication().startActivity(dialogIntent);
    }

}
