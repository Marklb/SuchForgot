package com.mark.suchforgot.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;


public class ActivityMain extends Activity implements OnClickListener{

    /*

    This is the main activity of the app.

    This activity will:
        - Start the Service.
        - Stop the Service.
        - Display information from the Service.

     */

    private static final String LOG_TAG = "[ActivityMain]"; //Filter to see logs from this class
    private static final boolean IS_DEBUGGING = true;

    private static Context mContext;

    /*
        Used to signify what the state of the vehicle is
     */
    public enum STATE{
        ON(R.string.state_on), //Vehicle is on
        OFF(R.string.state_off), //Vehicle is off
        IDLE(R.string.state_idle), //Vehicle is idle
        IGNORING(R.string.state_ignoring); //The device not in an important state

        int textId;
        STATE(int id){
            textId = id;
        }

        @Override
        public String toString(){
            return mContext.getString(textId);
        }
    }

    /*
        activity_main views
     */
    private Button btnStart;
    private Button btnStop;
    private TextView textViewServiceStatus;
    private TextView textViewAvgAccelerometerMag;
    private TextView textViewVehicleState;

    /*
        Used for getting messages from the service
     */
    private Messenger mService;
    private boolean mIsBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        btnStart = (Button)findViewById(R.id.activity_main_button_start);
        btnStop = (Button)findViewById(R.id.activity_main_button_stop);
        textViewServiceStatus = (TextView)findViewById(R.id.activity_main_textView_service_status);
        textViewAvgAccelerometerMag = (TextView)findViewById(R.id.activity_main_textView_avg_accel);
        textViewVehicleState = (TextView)findViewById(R.id.activity_main_textview_state);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        checkForSavedValues(savedInstanceState);

        checkIfServiceIsRunning();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("textStatus", textViewServiceStatus.getText().toString());
    }

    private void checkForSavedValues(Bundle state){
        if(state!=null){
            textViewServiceStatus.setText(state.getString("textStatus"));
        }
    }

    private void checkIfServiceIsRunning(){
        if(TheService.isRunning()){
            bindService();
        }
    }

    private void bindService(){
        bindService(new Intent(this, TheService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        textViewServiceStatus.setText("Binding.");
    }
    private void unbindService(){
        if(mIsBound){
            if(mService != null){
                try{
                    Message msg = Message.obtain(null, TheService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }catch(RemoteException e){
                    MyUtilities.Log_i(LOG_TAG, "Problem with service. (unbindService)", IS_DEBUGGING);
                }
            }
            unbindService(mConnection);
            mIsBound = false;
            textViewServiceStatus.setText("Unbinding.");
        }
    }

    private ServiceConnection mConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder){
            mService = new Messenger(iBinder);
            textViewServiceStatus.setText("Attached.");
            try{
                Message msg = Message.obtain(null, TheService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }catch(RemoteException e){
                MyUtilities.Log_i(LOG_TAG, "Problem with service. (ServiceConnection)", IS_DEBUGGING);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName){
            mService = null;
            textViewServiceStatus.setText("Disconnected.");
        }
    };

    public void onClick(View v){
        if(v == btnStart){
            MyUtilities.Log_i(LOG_TAG, "Starting", IS_DEBUGGING);
            startService(new Intent(ActivityMain.this, TheService.class));
            bindService();
        }else if(v == btnStop){
            MyUtilities.Log_i(LOG_TAG, "Stopping", IS_DEBUGGING);
            unbindService();
            stopService(new Intent(ActivityMain.this, TheService.class));
        }
    }

    private class IncomingHandler extends Handler{

        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case TheService.MSG_SET_STATUS:
                    textViewServiceStatus.setText(getString(R.string.service_status) + msg.getData().getString("strMsg"));
                    break;
                case TheService.MSG_SET_AVG:
                    textViewAvgAccelerometerMag.setText(getString(R.string.average_accelerometer_magnitude) + msg.getData().getString("strMsg"));
                    break;
                case TheService.MSG_SET_STATE:
                    textViewVehicleState.setText(msg.getData().getString("strMsg"));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
