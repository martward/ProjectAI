package ai.project.martijn.imu;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
    Button button;
    float x,y,z;
    float thetaX,thetaY,thetaZ;
    // X = Pitch , Y = Roll, Z = Azimut
    String ip = "192.168.0.123";
    StreamThetas streamThetas;

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
        button = (Button) findViewById(R.id.calibration);
        streamThetas = new StreamThetas();
        streamThetas.start();


        this.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streamThetas.calibrate = true;

            }
        });
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
        streamThetas.newData = true;
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

    private class StreamThetas extends Thread {

        boolean newData = false;
        boolean calibrate = false;
        Socket socket;
        DataOutputStream dataOutputStream;

        public StreamThetas() {

        }


        protected Void send(int type){
            String out;
            if(type == 0) {
                out = "relative/" + thetaX + "/" + thetaY + "/" + thetaZ ;
            }else{
                out = "absolute/" + x + "/" + y + "/" + z ;
            }
            try {
                dataOutputStream.writeUTF(out);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
            System.out.println(out);
            return null;
        }



        @Override
        public void run() {
            System.out.println("Started other thread");
            while(true)
            {
                if(socket == null)
                {
                    try {
                        socket = new Socket(ip, 9090);
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        socket = null;
                        e.printStackTrace();
                    } catch (Exception e) {
                        socket = null;
                        e.printStackTrace();
                    }
                }
                if(newData) {
                    //System.out.println("send");
                    send(0);
                    newData = false;
                } else if(calibrate) {
                    send(1);
                    calibrate = false;
                }
            }
        }
    }
}
