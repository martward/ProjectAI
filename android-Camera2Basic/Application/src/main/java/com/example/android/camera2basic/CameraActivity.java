package com.example.android.camera2basic;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;

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
import javax.microedition.khronos.opengles.GL10;

import java.nio.ShortBuffer;

public class CameraActivity extends GvrActivity implements GvrView.StereoRenderer, SensorEventListener {

    public static String IP = "192.168.0.111";
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
    private float[] headView;

    private float[] quaternion = new float[4];
    private float[] rot_accelerometer = new float[3];
    private float[] previous_velocity = new float[3];
    private float[] velocity = new float[3];
    private float[] translation = new float[3];
    private float[] position = new float[3];
    private float[] staticPosition = new float[3];
    private float[] staticTranslation = new float[3];
    private long time;

    private float[][] move_detection = new float[3][2];
    private boolean[] end_of_move = new boolean[]{false, false, false};
    private int[] translated = new int[]{10, 10, 10};
    private int[] detection_time = new int[]{10, 10, 10};

    Button resetButton;
    Button translationButton;
    private boolean doTranslation = false;

    SensorManager sMgr;
    Sensor translationSensor;

    // Display camera on surface
    private SurfaceTexture surface;
    private int texture;
    private short drawOrder[] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17}; // order to draw vertices
    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer drawListBuffer;
    private int mProgram;

    private float[] mCamera;
    private float[] mView;

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[]{0.0f, 2.0f, 0.0f, 1.0f};

    private static final int COORDS_PER_VERTEX = 3;
    private static final float MAX_MODEL_DISTANCE = 7.0f;

    NetworkThread networkThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        texture = createTexture();
        surface = new SurfaceTexture(texture);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance(surface))
                    .commit();
        }

        modelCube = new float[16];
        modelFloor = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        view = new float[16];
        camera = new float[16];
        position = new float[3];
        modelPosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
        headView = new float[16];

        setContentView(R.layout.activity_camera);
        translationButton = (Button) findViewById(R.id.toggleTranslation);
        resetButton = (Button) findViewById(R.id.resetButton);

        translationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!doTranslation) {
                    doTranslation = true;
                    position = staticPosition;
                    translation = staticTranslation;
                    velocity = new float[3];
                }else{
                    doTranslation = false;
                    staticPosition = position.clone();
                    staticTranslation = translation.clone();
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translation = new float[3];
                velocity = new float[3];
                position = new float[3];
            }
        });

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setSettingsButtonEnabled(false);
        gvrView.setVRModeEnabled(false);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);
        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);
        setGvrView(gvrView);

        updateModelPosition();

        sMgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        translationSensor = sMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sMgr.registerListener(this, translationSensor, SensorManager.SENSOR_DELAY_NORMAL);

        mCamera = new float[16];
        mView = new float[16];

        networkThread = new NetworkThread();
        networkThread.start();
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        Matrix.rotateM(modelCube, 0, 0, 0.5f, 0.5f, 1.0f);

        setMessage(headTransform);
        if(doTranslation){
            updatePosition();
        }
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, position[0], position[1], CAMERA_Z + position[2], position[0], position[1], position[2], 0.0f, 1.0f, 0.0f);
        headTransform.getHeadView(headView, 0);

        checkGLError("onReadyToDraw");

        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        surface.updateTexImage();
        surface.getTransformMatrix(mtx);
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        checkGLError("colorParam");

        // Camera
        drawCamera();
        Matrix.multiplyMM(mView, 0, eye.getEyeView(), 0, mCamera, 0);

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
        //drawFloor();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

        GLES20.glClearColor(1.0f,1.0f,1.0f,0);
        GLES20.glEnable(GL10.GL_CULL_FACE);
        //GLES20.glEnable(GL10.GL_DEPTH_TEST);

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
        Matrix.translateM(modelFloor, 0, 0, -20, 0); // Floor appears below user.

        // Camera

        ByteBuffer bb = ByteBuffer.allocateDirect(World.CUBE_COORDS.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(World.CUBE_COORDS);
        vertexBuffer.position(0);

        checkGLError("Camera bb");

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        checkGLError("Camera dlb");

        ByteBuffer bb2 = ByteBuffer.allocateDirect(World.CUBE_COORDS.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(World.CUBE_COORDS);
        textureVerticesBuffer.position(0);

        checkGLError("Camera bb2");

        int vertexShader2 = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.cameravertex);
        int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.fragment);

        checkGLError("Camera shader");

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader2);   // add the vertex shader to program
        checkGLError("Camera vertex");
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        checkGLError("Camera fragment");
        GLES20.glLinkProgram(mProgram);
        checkGLError("Camera link");
        GLES20.glUseProgram(mProgram);
        checkGLError("Camera use");
    }

    @Override
    public void onRendererShutdown() {
        networkThread.shutdown();
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

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type  The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
    private int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        return loadGLShader(type, code);
    }

    private int loadGLShader(int type, String code) {
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
     * Updates the cube model position.
     */
    protected void updateModelPosition() {
        Matrix.setIdentityM(modelCube, 0);
        Matrix.translateM(modelCube, 0, modelPosition[0], modelPosition[1], modelPosition[2]-15.f);
        Matrix.rotateM(modelCube, 0, 0, 5.f, 0, 0);
        //Matrix.scaleM(modelCube,0,0.3f, 0.3f, 0.3f);

        checkGLError("updateCubePosition");
    }

    private void setMessage(HeadTransform headTransform) {
        headTransform.getQuaternion(quaternion, 0);

        String msg = quaternion[0] + "/" + quaternion[1] + "/" + quaternion[2] + "/"
                + quaternion[3] + "/" + rot_accelerometer[0] + "/" + rot_accelerometer[1] + "/"
                + rot_accelerometer[2] + "/" + velocity[0] + "/" + velocity[1] + "/"
                + velocity[2] + "/" + translation[0] + "/" + translation[1] + "/"
                + translation[2];
        networkThread.setData(msg);
    }

    private void updatePosition()
    {
        float scale = 100.f;
        position[0] = scale * translation[1];
        position[2] = scale * translation[2];
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        float accX = event.values[0];
        float accY = event.values[1];
        float accZ = event.values[2];

        if (time == 0) {
            time = System.currentTimeMillis();
            return;
        }
        long currentTime = System.currentTimeMillis();
        float dt = (float) (currentTime - time) / (float) 1000.0;
        time = currentTime;

        double[][] acc = {{accX, accY, accZ}};
        double[][] R = getRotationMatrix();
        Jama.Matrix Rot = new Jama.Matrix(R).inverse();
        Jama.Matrix Acc = new Jama.Matrix(acc);
        Jama.Matrix accel = Rot.times(Acc.transpose());
        double[][] acceleration = accel.getArrayCopy();

        rot_accelerometer[0] = (float) acceleration[0][0];
        rot_accelerometer[1] = (float) acceleration[1][0];
        rot_accelerometer[2] = (float) acceleration[2][0];

        if (Math.sqrt(rot_accelerometer[0] * rot_accelerometer[0] + rot_accelerometer[1] * rot_accelerometer[1] +
                rot_accelerometer[2] * rot_accelerometer[2]) > 0.5) {
            velocity[0] = previous_velocity[0] + rot_accelerometer[0] * dt;
            velocity[1] = previous_velocity[1] + rot_accelerometer[1] * dt;
            velocity[2] = previous_velocity[2] + rot_accelerometer[2] * dt;
        }
        handleTranslation(0, dt);
        handleTranslation(1, dt);
        handleTranslation(2, dt);
    }

    public void handleTranslation(int i, float dt) {
        if (translated[i] == 0) {
            // MOVE DETECTION
            float move_threshold = 1.f;
            if (rot_accelerometer[i] > move_threshold) {
                System.out.println("Upper Peak...");
                if (rot_accelerometer[i] >= move_detection[i][1] && !end_of_move[i]) {
                    move_detection[i][1] = rot_accelerometer[i];
                } else if (move_detection[i][0] != 0) {
                    velocity[i] = 0;
                    previous_velocity[i] = 0;
                    end_of_move[i] = true;
                    System.out.println("End of move...");
                }
            } else if (rot_accelerometer[i] < -move_threshold) {
                System.out.println("lower Peak...");
                if (rot_accelerometer[i] <= move_detection[i][0] && !end_of_move[i]) {
                    move_detection[i][0] = rot_accelerometer[i];
                } else if (move_detection[i][1] != 0) {
                    velocity[i] = 0;
                    previous_velocity[i] = 0;
                    end_of_move[i] = true;
                    System.out.println("End of move...");
                }
            } else if (move_detection[i][0] != 0 && move_detection[i][1] != 0) {
                previous_velocity[i] = 0;
                velocity[i] = 0;
                move_detection[i] = new float[2];
                translated[i] = 100;
                end_of_move[i] = false;
            }

            translation[i] = translation[i] + ((previous_velocity[i] + velocity[i]) / 2) * dt;
            previous_velocity[i] = velocity[i];
        } else {
            translated[i] -= 1;
        }
    }

    public double [][] getRotationMatrix() {
        float x = quaternion[1];
        float y = quaternion[0];
        float z = quaternion[2];
        float w = quaternion[3];

        float n = x * x + y * y + z * z + w * w;
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
        return new double[][]{{1 - (yy + zz), xy - wz, xz + wy},
                {xy + wz, 1 - (xx + zz), yz - wx},
                {xz - wy, yz + wx, 1 - (xx + yy)}};
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void drawCamera()
    {
        int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

        surface.updateTexImage();
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        checkGLError("Camera update");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        checkGLError("Camera bits");

        GLES20.glUseProgram(mProgram);
        checkGLError("Camera use prog");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkGLError("Camera active tex");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        checkGLError("Camera bind");

        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        checkGLError("Camera get pos");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        checkGLError("Camera enable poshandle");
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        checkGLError("Camera attr points poshandle");

        int mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        checkGLError("Camera get inputtex");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        checkGLError("Camera enable vertex coord");
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, textureVerticesBuffer);
        checkGLError("Camera attr points tex handle");

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        checkGLError("Camera draw el");

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        checkGLError("Camera disable poshandle");
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
        checkGLError("Camera disable tex handle");
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
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

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

    static private int createTexture()
    {
        int[] texture = new int[1];

        GLES20.glGenTextures(1,texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE0, texture[0] );
        GLES20.glTexParameterf(GLES20.GL_TEXTURE0, GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE0, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE0, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE0, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }


    class NetworkThread extends Thread {

        public boolean writing = false;

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

        private void log(String s) {
            System.out.println(s);
        }

        public boolean setData(String data)
        {
            if (writing) return false;

            this.data = data;
            return true;
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
                        writing = true;
                        //log("Send data: " + data);
                        outputStream.writeChars(data+'\n');
                        outputStream.flush();
                        writing = false;
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
