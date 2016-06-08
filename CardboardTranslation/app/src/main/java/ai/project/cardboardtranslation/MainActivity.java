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
import java.net.UnknownHostException;

import javax.microedition.khronos.egl.EGLConfig;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer {

    public static String IP = "192.168.0.100";

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
        float[] rot = new float[4];

        headTransform.getQuaternion(rot, 0);

        msg = msg + rot[0] + "/" + rot[1] + "/" +rot[2] + "/" +rot[3];

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

    }

    private void log(String s)
    {
        System.out.println(s);
    }

    class NetworkThread extends Thread
    {
        private Socket socket;
        private DataOutputStream outputStream;

        String data;

        public NetworkThread()
        {

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
            while(true)
            {
                if (outputStream == null || socket == null) {
                    try {
                        socket = new Socket(IP, 9090);
                        outputStream = new DataOutputStream(socket.getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (data != null) {
                    try {
                        log("send data: " + data);
                        outputStream.writeUTF(data);
                        data = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                        socket = null;
                        outputStream = null;
                    }
                }
            }
        }
    }
}
