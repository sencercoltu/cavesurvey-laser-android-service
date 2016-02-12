package net.speleomaniac.mapit.sencemeterservice;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class OrientationDeviceModel {

    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mTextureBuffer;
    private final ByteBuffer mIndexBuffer;

    private final int[] Textures = new int[1];

    private final byte[] indices = {
            0, 1, 2, 0, 2, 3, //arka
            4, 5, 6, 4, 6, 7, //ön
            8, 9, 10, 8, 10, 11, //sağ
            12, 13, 14, 12, 14, 15, //sol
            16, 17, 18, 16, 18, 19, //üst
            20, 21, 22, 20, 22, 23 //alt
    };

    public OrientationDeviceModel() {
        float[] vertices = {
                //ön
                -0.48f, -0.25f, 1.12f,
                0.48f, -0.25f, 1.12f,
                0.48f, 0.25f, 1.12f,
                -0.48f, 0.25f, 1.12f,

                //arka
                0.48f, -0.25f, -1.12f,
                -0.48f, -0.25f, -1.12f,
                -0.48f, 0.25f, -1.12f,
                0.48f, 0.25f, -1.12f,

                //sağ
                0.48f, -0.25f, 1.12f,
                0.48f, -0.25f, -1.12f,
                0.48f, 0.25f, -1.12f,
                0.48f, 0.25f, 1.12f,

                //sol
                -0.48f, -0.25f, -1.12f,
                -0.48f, -0.25f, 1.12f,
                -0.48f, 0.25f, 1.12f,
                -0.48f, 0.25f, -1.12f,

                //üst
                -0.48f, 0.25f, 1.12f,
                0.48f, 0.25f, 1.12f,
                0.48f, 0.25f, -1.12f,
                -0.48f, 0.25f, -1.12f,

                //alt
                0.48f, -0.25f, 1.12f,
                -0.48f, -0.25f, 1.12f,
                -0.48f, -0.25f, -1.12f,
                0.48f, -0.25f, -1.12f,
        };
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mVertexBuffer = byteBuf.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        /* The initial texture coordinates (u, v) */
        float[] texture = {
                //Mapping coordinates for the vertices
                //arka
                0.26f, 0.13f,
                0.51f, 0.13f,
                0.51f, 0.00f,
                0.26f, 0.00f,

                //ön
                0.00f, 0.13f,
                0.25f, 0.13f,
                0.25f, 0.00f,
                0.00f, 0.00f,

                //sol
                0.00f, 1.00f,
                0.60f, 1.00f,
                0.60f, 0.87f,
                0.00f, 0.87f,

                //sağ
                0.00f, 0.86f,
                0.60f, 0.86f,
                0.60f, 0.73f,
                0.00f, 0.73f,

                //üst
                0.0f, 0.73f,
                0.25f, 0.73f,
                0.25f, 0.13f,
                0.0f, 0.13f,

                //alt
                0.26f, 0.73f,
                0.51f, 0.73f,
                0.51f, 0.13f,
                0.26f, 0.13f,

        };
        byteBuf = ByteBuffer.allocateDirect(texture.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mTextureBuffer = byteBuf.asFloatBuffer();
        mTextureBuffer.put(texture);
        mTextureBuffer.position(0);
        mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
        mIndexBuffer.put(indices);
        mIndexBuffer.position(0);
    }

    public void draw(GL10 gl) {
        gl.glBindTexture(GL10.GL_TEXTURE_2D, Textures[0]);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glFrontFace(GL10.GL_CCW);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);

        gl.glDrawElements(GL10.GL_TRIANGLES, indices.length, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }

    public void loadGLTexture(GL10 gl, Context context) {
        //Get the texture from the Android resource directory
        InputStream is = context.getResources().openRawResource(R.raw.device);
        Bitmap bitmap = null;
        try {
            //BitmapFactory is an Android graphics utility for images
            bitmap = BitmapFactory.decodeStream(is);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            //Always clear and close
            try {
                is.close();
                //is = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Generate one texture pointer...
        gl.glGenTextures(1, Textures, 0);
        //...and bind it to our array
        gl.glBindTexture(GL10.GL_TEXTURE_2D, Textures[0]);

        //Create Nearest Filtered Texture
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        //Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        if (bitmap != null)
        {
            //Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

            //Clean up
            bitmap.recycle();
        }
    }

}
