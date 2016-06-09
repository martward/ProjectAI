package ai.project.cardboardtranslation;

import android.os.Bundle;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.microedition.khronos.egl.EGLConfig;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer {

    public static String IP = "192.168.0.105";

    NetworkThread networkThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkThread = new NetworkThread();
        networkThread.start();

        setContentView(R.layout.ui_common);

        GvrView gvrView = (GvrView)findViewById(R.id.gvr_view);

        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);

        setGvrView(gvrView);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        String msg = "absolute/";
        float[] rot = new float[3];

        headTransform.getEulerAngles(rot, 0);

        msg = msg + rot[0] + "/" + rot[1] + "/" +rot[2];

        networkThread.setData(msg);

    }

    @Override
    public void onDrawEye(Eye eye) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

    }

    @Override
    public void onRendererShutdown() {
        networkThread.shutdown();
    }

    private void log(String s)
    {
        System.out.println(s);
    }

    class NetworkThread extends Thread
    {
        private Socket socket;
        private DataOutputStream outputStream;
        private boolean running;

        String data;

        public NetworkThread()
        {
            running = true;
        }

        public void shutdown()
        {
            running = false;
        }

        public void setData(String data)
        {
            this.data = data;
        }

        @Override
        public void start()
        {
            super.start();
        }

        @Override
        public void run()
        {
            log("Trying to connect");
            while(running)
            {
                if (outputStream == null || socket == null) {
                    try {
                        socket = new Socket(IP, 9090);
                        outputStream = new DataOutputStream(socket.getOutputStream());

                        log("Connected");
                    } catch (IOException e) {
                        try {
                            sleep(100);
                        } catch (InterruptedException e1) {
                            log("Could not sleep");
                        }
                    }
                }

                if (data != null && outputStream != null ) {
                    try {
                        log("Send data: " + data);
                        outputStream.writeUTF(data);
                        //sleep( 100 );
                        
                        data = null;
                    } catch (IOException e) {
                        log("Connection lost");

                        socket = null;
                        outputStream = null;
                    }
                }
            }
        }
    }
}
