package ai.project.martijn.imu;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Home extends AppCompatActivity implements SensorEventListener {

    SensorManager sMgr;
    Sensor orientation;
    TextView xvalue;
    TextView yvalue;
    TextView zvalue;
    TextView thetax;
    TextView thetay;
    TextView thetaz;
    float x,y,z;
    float thetaX,thetaY,thetaZ;
    // X = Pitch , Y = Roll, Z = Azimut

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sMgr = (SensorManager)this.getSystemService(SENSOR_SERVICE);
        orientation = sMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sMgr.registerListener(this, orientation, SensorManager.SENSOR_DELAY_NORMAL);
        xvalue = (TextView) findViewById(R.id.xvalue);
        yvalue = (TextView) findViewById(R.id.yvalue);
        zvalue = (TextView) findViewById(R.id.zvalue);
        thetax = (TextView) findViewById(R.id.thetax);
        thetay = (TextView) findViewById(R.id.thetay);
        thetaz = (TextView) findViewById(R.id.thetaz);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {
        // Set values in textvield
        xvalue.setText(event.values[0]*180 + "");
        yvalue.setText(event.values[1]*180 + "");
        zvalue.setText(event.values[2]*180 + "");
        // Calculate the angles of rotation along all axes
        thetaX = calcTheta(x, event.values[0]*180);
        thetaY = calcTheta(y, event.values[1]*180);
        thetaZ = calcTheta(z, event.values[2]*180);
        thetax.setText(thetaX + "");
        thetay.setText(thetaY + "");
        thetaz.setText(thetaZ + "");
        new StreamThetas().execute();
        // Update the x,y and z values for the next iteration
        x = event.values[0]*180;
        y = event.values[1]*180;
        z = event.values[2]*180;
    }

    public float calcTheta(float oldValue, float newValue){
        float value = oldValue - newValue;
        float theta;
        if(value < -180){
            theta = 180 - (value + 180);
        }else if( value > 180) {
            theta = -180 + (value-180);
        }else {
            theta = value;
        }
        return theta;
    }

    private class StreamThetas extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            try {
                System.out.println( "testerino");
                socket = new Socket("192.168.0.123", 9090);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                String out = thetaX + "/" + thetaY + "/" + thetaZ;
                dataOutputStream.writeUTF(out);
                //dataOutputStream.writeFloat(thetaX);
                //dataOutputStream.writeFloat(thetaY);
                //dataOutputStream.writeFloat(thetaZ);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finally{
                if (socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                if (dataOutputStream != null){
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }

}
