package net.speleomaniac.mapit.sencemeterservice;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OrientationRenderer implements GLSurfaceView.Renderer
{
    // --Commented out by Inspection (19.8.2014 23:42):public final static int LockedButtonColor = Color.argb(0, 255, 0, 0);
    // --Commented out by Inspection (19.8.2014 23:42):public final static int UnlockedButtonColor = Color.argb(0, 0, 0, 255);

    private float yaw, pitch, roll;

    private final OrientationDeviceModel mModel = new OrientationDeviceModel();
    private final GLSurfaceView mView;
    private final Context context;

    public OrientationRenderer(Context ctx, GLSurfaceView view)
    {
        context = ctx;
        mView = view;
    }

    public void setOrientation(float compass, float inclination, float roll)
    {
        this.yaw = -compass;
        this.pitch = inclination;
        this.roll = roll;
        mView.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig eglConfig) {
        yaw = pitch = roll = 0.0f;
        mModel.loadGLTexture(gl, context);


        gl.glEnable(GL10.GL_TEXTURE_2D);			//Enable Texture Mapping ( NEW )
        gl.glShadeModel(GL10.GL_SMOOTH); 			//Enable Smooth Shading
        //gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); 	//Black Background
        gl.glClearDepthf(1.0f); 					//Depth Buffer Setup
        gl.glEnable(GL10.GL_DEPTH_TEST); 			//Enables Depth Testing
        gl.glFrontFace(GL10.GL_CCW);
        gl.glDepthFunc(GL10.GL_LEQUAL); 			//The Type Of Depth Testing To Do
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glCullFace(GL10.GL_BACK);

        //Really Nice Perspective Calculations
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 45.0f, (float) width / (float) height, 0.1f, 100.0f);
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        //draw ground plane and coordinate axes

        gl.glTranslatef(0.0f, 0.0f, -3.5f);
        gl.glRotatef(yaw, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(pitch, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(roll, 0.0f, 0.0f, 1.0f);

        mModel.draw(gl);


        gl.glLoadIdentity();


    }
}
