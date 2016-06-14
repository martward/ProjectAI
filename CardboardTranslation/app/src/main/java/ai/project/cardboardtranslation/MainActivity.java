package ai.project.cardboardtranslation;

import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.hardware.SensorEvent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

import Jama.*;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer, SensorEventListener {

    public static String IP = "192.168.0.105";
    private float floorDepth = 20f;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;
    private static final float CAMERA_Z = 0.01f;

    private static final String TAG = "MainActivity";

    protected float[] modelCube;
    protected float[] modelPosition;

    private FloatBuffer floorVertices;
    private FloatBuffer floorColors;
    private FloatBuffer floorNormals;

    private FloatBuffer cubeVertices;
    private FloatBuffer cubeColors;
    private FloatBuffer cubeFoundColors;
    private FloatBuffer cubeNormals;

    private int cubeProgram;
    private int floorProgram;

    private int cubePositionParam;
    private int cubeNormalParam;
    private int cubeColorParam;
    private int cubeModelParam;
    private int cubeModelViewParam;
    private int cubeModelViewProjectionParam;
    private int cubeLightPosParam;

    private int floorPositionParam;
    private int floorNormalParam;
    private int floorColorParam;
    private int floorModelParam;
    private int floorModelViewParam;
    private int floorModelViewProjectionParam;
    private int floorLightPosParam;

    private float[] modelFloor;
    private final float[] lightPosInEyeSpace = new float[4];
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] view;
    private float[] camera;


    private float[] position;
    private double exampleState;

    private float[] headView;

    private float[] translation = new float[3];
    private float[] velocity = new float[3];
    private long time;
    private float[] quaternion = new float[4];

    SensorManager sMgr;
    Sensor translationSensor;


    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[]{0.0f, 2.0f, 0.0f, 1.0f};

    private static final int COORDS_PER_VERTEX = 3;

    private static final float MIN_MODEL_DISTANCE = 3.0f;
    private static final float MAX_MODEL_DISTANCE = 7.0f;
    private float objectDistance = MAX_MODEL_DISTANCE / 2.0f;

    NetworkThread networkThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        modelCube = new float[16];
        modelFloor = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        view = new float[16];
        camera = new float[16];
        position = new float[3];
        modelPosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
        headView = new float[16];
        System.out.println("ONCREATE");

        time = System.currentTimeMillis();

        setContentView(R.layout.ui_common);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);

        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);

        setGvrView(gvrView);

        updateModelPosition();

        sMgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        translationSensor = sMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sMgr.registerListener(this, translationSensor, SensorManager.SENSOR_DELAY_NORMAL);


        networkThread = new NetworkThread();
        networkThread.start();

    }

    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     */
    private static void checkGLError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type  The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
    private int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    private String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setMessage(HeadTransform headTransform)
    {
        String msg = "absolute/";

        headTransform.getQuaternion(quaternion, 0);

        // Translation based on accelerometer
        msg = msg + quaternion[0] + "/" + quaternion[1] + "/" + quaternion[2] + "/"
                + quaternion[3] + "/" + translation[0] + "/" + translation[1] + "/"
                + translation[2];

        networkThread.setData(msg);
    }

    private void updatePosition()
    {
        float scale = 50.f;
        position[0] = scale * translation[0];
        position[2] = scale * translation[2];

        System.out.print("Trans: ");
        for( int i = 0; i < 3; i++ ) {System.out.print(translation[i] + ", ");}
        System.out.println("");

        System.out.print("Pos: ");
        System.out.println(position[0] + ", " + position[2]);
        for( int i = 0; i < 3; i++ ) {System.out.print(position[i] + ", ");}
        System.out.println("\n----");

    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {

        Matrix.rotateM(modelCube, 0, 0, 0.5f, 0.5f, 1.0f);

        setMessage(headTransform);
        updatePosition();

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, position[0], position[1], CAMERA_Z + position[2], position[0], position[1], position[2], 0.0f, 1.0f, 0.0f);

        headTransform.getHeadView(headView, 0);

        checkGLError("onReadyToDraw");

    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGLError("colorParam");

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        // Set the position of the light
        Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);


        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(modelView, 0, view, 0, modelCube, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawCube();

        // Set modelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, modelFloor, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawFloor();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(World.CUBE_COORDS.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        cubeVertices = bbVertices.asFloatBuffer();
        cubeVertices.put(World.CUBE_COORDS);
        cubeVertices.position(0);

        ByteBuffer bbColors = ByteBuffer.allocateDirect(World.CUBE_COLORS.length * 4);
        bbColors.order(ByteOrder.nativeOrder());
        cubeColors = bbColors.asFloatBuffer();
        cubeColors.put(World.CUBE_COLORS);
        cubeColors.position(0);

        ByteBuffer bbNormals = ByteBuffer.allocateDirect(World.CUBE_NORMALS.length * 4);
        bbNormals.order(ByteOrder.nativeOrder());
        cubeNormals = bbNormals.asFloatBuffer();
        cubeNormals.put(World.CUBE_NORMALS);
        cubeNormals.position(0);

        // make a floor
        ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(World.FLOOR_COORDS.length * 4);
        bbFloorVertices.order(ByteOrder.nativeOrder());
        floorVertices = bbFloorVertices.asFloatBuffer();
        floorVertices.put(World.FLOOR_COORDS);
        floorVertices.position(0);

        ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(World.FLOOR_NORMALS.length * 4);
        bbFloorNormals.order(ByteOrder.nativeOrder());
        floorNormals = bbFloorNormals.asFloatBuffer();
        floorNormals.put(World.FLOOR_NORMALS);
        floorNormals.position(0);

        ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(World.FLOOR_COLORS.length * 4);
        bbFloorColors.order(ByteOrder.nativeOrder());
        floorColors = bbFloorColors.asFloatBuffer();
        floorColors.put(World.FLOOR_COLORS);
        floorColors.position(0);

        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int gridShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
        int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        cubeProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(cubeProgram, vertexShader);
        GLES20.glAttachShader(cubeProgram, passthroughShader);
        GLES20.glLinkProgram(cubeProgram);
        GLES20.glUseProgram(cubeProgram);

        checkGLError("Cube program");

        cubePositionParam = GLES20.glGetAttribLocation(cubeProgram, "a_Position");
        cubeNormalParam = GLES20.glGetAttribLocation(cubeProgram, "a_Normal");
        cubeColorParam = GLES20.glGetAttribLocation(cubeProgram, "a_Color");

        cubeModelParam = GLES20.glGetUniformLocation(cubeProgram, "u_Model");
        cubeModelViewParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVMatrix");
        cubeModelViewProjectionParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVP");
        cubeLightPosParam = GLES20.glGetUniformLocation(cubeProgram, "u_LightPos");

        checkGLError("Cube program params");

        floorProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(floorProgram, vertexShader);
        GLES20.glAttachShader(floorProgram, gridShader);
        GLES20.glLinkProgram(floorProgram);
        GLES20.glUseProgram(floorProgram);

        checkGLError("Floor program");

        floorModelParam = GLES20.glGetUniformLocation(floorProgram, "u_Model");
        floorModelViewParam = GLES20.glGetUniformLocation(floorProgram, "u_MVMatrix");
        floorModelViewProjectionParam = GLES20.glGetUniformLocation(floorProgram, "u_MVP");
        floorLightPosParam = GLES20.glGetUniformLocation(floorProgram, "u_LightPos");

        floorPositionParam = GLES20.glGetAttribLocation(floorProgram, "a_Position");
        floorNormalParam = GLES20.glGetAttribLocation(floorProgram, "a_Normal");
        floorColorParam = GLES20.glGetAttribLocation(floorProgram, "a_Color");

        checkGLError("Floor program params");

        Matrix.setIdentityM(modelFloor, 0);
        Matrix.translateM(modelFloor, 0, 0, -floorDepth, 0); // Floor appears below user.

    }

    /**
     * Draw the floor.
     * <p/>
     * <p>This feeds in data for the floor into the shader. Note that this doesn't feed in data about
     * position of the light, so if we rewrite our code to draw the floor first, the lighting might
     * look strange.
     */
    public void drawFloor() {
        GLES20.glUseProgram(floorProgram);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(floorLightPosParam, 1, lightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(floorModelParam, 1, false, modelFloor, 0);
        GLES20.glUniformMatrix4fv(floorModelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(floorModelViewProjectionParam, 1, false, modelViewProjection, 0);
        GLES20.glVertexAttribPointer(
                floorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, floorVertices);
        GLES20.glVertexAttribPointer(floorNormalParam, 3, GLES20.GL_FLOAT, false, 0, floorNormals);
        GLES20.glVertexAttribPointer(floorColorParam, 4, GLES20.GL_FLOAT, false, 0, floorColors);

        GLES20.glEnableVertexAttribArray(floorPositionParam);
        GLES20.glEnableVertexAttribArray(floorNormalParam);
        GLES20.glEnableVertexAttribArray(floorColorParam);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 24);

        checkGLError("drawing floor");
    }

    public void drawCube() {
        GLES20.glUseProgram(cubeProgram);

        GLES20.glUniform3fv(cubeLightPosParam, 1, lightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(cubeModelParam, 1, false, modelCube, 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(cubeModelViewParam, 1, false, modelView, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(
                cubePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, cubeVertices);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(cubeModelViewProjectionParam, 1, false, modelViewProjection, 0);

        // Set the normal positions of the cube, again for shading
        GLES20.glVertexAttribPointer(cubeNormalParam, 3, GLES20.GL_FLOAT, false, 0, cubeNormals);
        GLES20.glVertexAttribPointer(cubeColorParam, 4, GLES20.GL_FLOAT, false, 0, cubeColors);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(cubePositionParam);
        GLES20.glEnableVertexAttribArray(cubeNormalParam);
        GLES20.glEnableVertexAttribArray(cubeColorParam);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        checkGLError("Drawing cube");
    }

    /**
     * Updates the cube model position.
     */
    protected void updateModelPosition() {
        Matrix.setIdentityM(modelCube, 0);
        Matrix.translateM(modelCube, 0, modelPosition[0], modelPosition[1], modelPosition[2]);

        checkGLError("updateCubePosition");
    }

    @Override
    public void onRendererShutdown() {
        networkThread.shutdown();
    }

    private void log(String s) {
        System.out.println(s);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float accX = event.values[0];
        float accY = event.values[1];
        float accZ = event.values[2];

        long currentTime = System.currentTimeMillis();
        float dt = (float)(currentTime - time) / (float)1000.0;
        time = currentTime;

        if (Math.sqrt(accX * accX + accY * accY + accZ * accZ) > 0.5) {
            double[][] acc = {{accX, accY, accZ}};

            float x = quaternion[0];
            float y = quaternion[1];
            float z = quaternion[2];
            float w = quaternion[3];

            float n = x*x + y*y + z*z + w*w;
            float s = 0;
            if (n != 0) {
                s = 2 / n;
            }
            float wx = s * x * w;
            float wy = s * y * w;
            float wz = s * z * w;
            float xx = s * x * x;
            float xy = s * x * y;
            float xz = s * x * z;
            float yy = s * y * y;
            float yz = s * y * z;
            float zz = s * z * z;
            double[][] R = {{1 - (yy + zz), xy - wz, xz + wy},
                    {xy + wz, 1 - (xx + zz), yz - wx},
                    {xz - wy, yz + wx, 1 - (xx + yy)}};
            Jama.Matrix Rot = new Jama.Matrix(R).inverse();
            Jama.Matrix Acc = new Jama.Matrix(acc);
            Jama.Matrix accel = Acc.times(Rot);
            double [][] acceleration = accel.getArrayCopy();
            System.out.println(acceleration[0][0]);
            System.out.println(acceleration[0][1]);
            System.out.println(acceleration[0][2]);

            velocity[0] = velocity[0] + (float)acceleration[0][0] * dt;
            velocity[1] = velocity[1] + (float)acceleration[0][1] * dt;
            velocity[2] = velocity[2] + (float)acceleration[0][2] * dt;
        } else {
            velocity[0] = 0;
            velocity[1] = 0;
            velocity[2] = 0;
        }

        float max = 2.0f;
        if( velocity[0] < max && velocity[1] < max && velocity[2] < max )
        {
            translation[0] = translation[0] + velocity[0] * dt;
            translation[1] = translation[1] + velocity[1] * dt;
            translation[2] = translation[2] + velocity[2] * dt;
        }
        //System.out.println(translation[0] + " " + translation[1] + " " + translation[2] + " ");
    }

    public float[] dot(float[][] R, float[] acc) {
        float[] result = new float[acc.length];
        for(int i = 0; i < 3; i++){
            float entry = 0;
            for(int j = 0; j < 3; j++){
                entry += R[j][i]*acc[i];
            }
            result[i] = entry;
        }
        return result;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    class NetworkThread extends Thread {
        private Socket socket;
        private DataOutputStream outputStream;
        private boolean running;

        String data;

        public NetworkThread() {
            running = true;
        }

        public void shutdown() {
            running = false;
        }

        public void setData(String data) {
            this.data = data;
        }

        @Override
        public void start() {
            super.start();
        }

        @Override
        public void run() {
            log("Trying to connect");
            while (running) {
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

                if (data != null && outputStream != null) {
                    try {
                        log("Send data: " + data);
                        outputStream.writeUTF(data);
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
