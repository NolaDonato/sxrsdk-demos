/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf.arSimplesample;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.renderscript.Float3;
import android.renderscript.Float4;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;

import com.mps.cvcontroller.client.IInputService;
import com.mps.cvcontroller.client.PoseDataParcelable;
import com.samsungxr.SXRActivity;
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRTexture;
import com.samsungxr.SXRTransform;
import com.samsungxr.nodes.SXRTextViewNode;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import static java.lang.Math.toDegrees;

public class SampleActivity extends SXRActivity {


    private final static String INPUT_SERVICE_PACKAGE_NAME = "com.mps.cvcontroller.client";
    private final static String INPUT_SERVICE_COMPONENT_NAME = "com.mps.cvcontroller.client.InputServiceManager";
    private final static String INPUT_SERVICE_ACTION = "com.mps.cvcontroller.client.InputServiceManager.BIND";

    private SXRScene scene;
    private IInputService mInputService;
    private SXRTextViewNode hud;
    private SXRTextViewNode pos_hud;
    private SXRTextViewNode cameraRigPos_hud;
    private SXRTextViewNode cameraRigRot_hud;
    private SXRTextViewNode rot_hud;
    private SXRTextViewNode frameRate_hud;

    public int frame_rate = 0;

    private final static String TAG = "Vince_test";


    public float x_pos = 0.0f;
    public float y_pos = 0.0f;
    public float z_pos = 0.0f;
    public float w_rot = 0.0f;
    public float x_rot = 0.0f;
    public float y_rot = 0.0f;
    public float z_rot = 0.0f;
    public int mode = 0;

    public boolean reset = false;


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setMain(new SampleMain());
    }



    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Service connected");
            mInputService = IInputService.Stub.asInterface(service);

            Thread th1 = new Thread() {
                @Override
                public void run() {
                    try {
                        while (!isInterrupted()) {
                            Thread.sleep(100);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mInputService != null) {

                                        try {
                                            PoseDataParcelable dt = mInputService.getHeadPose();
                                            if (dt != null) {
                                                x_pos = dt.position.x;
                                                y_pos = dt.position.y;
                                                z_pos = dt.position.z;

                                                w_rot = dt.quaternion.w;
                                                x_rot = dt.quaternion.x;
                                                y_rot = dt.quaternion.y;
                                                z_rot = dt.quaternion.z;

                                                // convert to pitch, yaw, roll
                                                Quaternionf cameraQuat = new Quaternionf(x_rot, y_rot, z_rot, w_rot);
                                                float pitch  = (float)Math.atan2(2.0 * (cameraQuat.z * cameraQuat.y + cameraQuat.w * cameraQuat.x) , 1.0 - 2.0 * (cameraQuat.x * cameraQuat.x + cameraQuat.y * cameraQuat.y));
                                                float yaw = (float)Math.asin(2.0 * (cameraQuat.y * cameraQuat.w - cameraQuat.z * cameraQuat.x));
                                                float roll   = (float)Math.atan2(2.0 * (cameraQuat.z * cameraQuat.w + cameraQuat.x * cameraQuat.y) , - 1.0 + 2.0 * (cameraQuat.w * cameraQuat.w + cameraQuat.x * cameraQuat.x));

                                                String s = String.format("C_POS X: %.2f, Y: %.2f, Z: %.2f", x_pos, y_pos, z_pos);
                                                String s_rot = String.format("C_ROT P: %.2f, Y: %.2f, R: %.2f",(float)toDegrees(pitch), (float)toDegrees(yaw), (float)toDegrees(roll));
                                                if (pos_hud != null && rot_hud != null){
                                                    pos_hud.setText(s);
                                                    rot_hud.setText(s_rot);

                                                }

                                                frame_rate = mInputService.getRate();
                                                if (frameRate_hud != null) {
                                                    String s_rate = String.format("CV Frame Rate: %d", frame_rate);
                                                    frameRate_hud.setText(s_rate);
                                                }
                                            }
                                            mode = mInputService.getMode();
                                        }catch (RemoteException e) {
                                            e.printStackTrace();
                                        }

                                        if (hud != null){
                                            switch (mode) {
                                                case -1:
                                                    hud.setText("CV service not working properly");
                                                    break;
                                                case 0:
                                                    hud.setText("No IMU and no CV");
                                                    break;
                                                case 1:
                                                    hud.setText("Imu only");
                                                    break;
                                                case 2:
                                                    hud.setText("Marker only");
                                                    break;
                                                case 3:
                                                    hud.setText("MarkerTraining Mode on");
                                                    break;
                                                case 4:
                                                    hud.setText("OrbMode on");
                                                    break;
                                                case 5:
                                                    hud.setText("Multi Marker detected");
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }

                                    }
                                }
                            });
                        }

                    }catch (Exception e) {}
                }
            };
            th1.start();


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected");
            mInputService = null;


        }
    };



    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "start of onStart");
        ComponentName comp_name = new ComponentName(INPUT_SERVICE_PACKAGE_NAME, INPUT_SERVICE_COMPONENT_NAME);
        Intent service = new Intent();
        service.setComponent(comp_name);
        bindService(service, mConnection, Service.BIND_AUTO_CREATE);
        Log.i(TAG, "end of onStart");
    }


    @Override
    protected void onStop(){
        super.onStop();
        if (mInputService != null) unbindService(mConnection);
    }

    public void makeHuds(){

        SXRRenderData hud_data = hud.getRenderData();
        hud.setTextSize(6.0f);
        hud.setTextColor(Color.YELLOW);
        hud.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        hud.getTransform().setPosition(-0.03f, -0.13f, -0.5f);
        hud.getTransform().setScale(0.2f, 0.2f, 0.2f);
        hud_data.setDepthTest(false);
        hud_data.setAlphaBlend(true);
        hud_data.setRenderingOrder(SXRRenderData.SXRRenderingOrder.OVERLAY);



        SXRRenderData pos_hud_data = pos_hud.getRenderData();
        pos_hud.setTextSize(3.5f);
        pos_hud.setTextColor(Color.YELLOW);
        pos_hud.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        pos_hud.getTransform().setPosition(-0.03f, 0.12f, -0.5f);
        pos_hud.getTransform().setScale(0.2f, 0.2f, 0.2f);
        pos_hud_data.setDepthTest(false);
        pos_hud_data.setAlphaBlend(true);
        pos_hud_data.setRenderingOrder(SXRRenderData.SXRRenderingOrder.OVERLAY);


        SXRRenderData rot_hud_data = rot_hud.getRenderData();
        rot_hud.setTextSize(3.5f);
        rot_hud.setTextColor(Color.YELLOW);
        rot_hud.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        rot_hud.getTransform().setPosition(-0.03f, 0.02f, -0.5f);
        rot_hud.getTransform().setScale(0.2f, 0.2f, 0.2f);
        rot_hud_data.setDepthTest(false);
        rot_hud_data.setAlphaBlend(true);
        rot_hud_data.setRenderingOrder(SXRRenderData.SXRRenderingOrder.OVERLAY);


        // ---

        SXRRenderData cameraRigPose_data = cameraRigPos_hud.getRenderData();
        cameraRigPos_hud.setTextSize(3.5f);
        cameraRigPos_hud.setTextColor(Color.YELLOW);
        cameraRigPos_hud.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        cameraRigPos_hud.getTransform().setPosition(-0.03f, 0.07f, -0.5f);
        cameraRigPos_hud.getTransform().setScale(0.2f, 0.2f, 0.2f);
        cameraRigPose_data.setDepthTest(false);
        cameraRigPose_data.setAlphaBlend(true);
        cameraRigPose_data.setRenderingOrder(SXRRenderData.SXRRenderingOrder.OVERLAY);


        SXRRenderData cameraRigRot_data = cameraRigRot_hud.getRenderData();
        cameraRigRot_hud.setTextSize(3.5f);
        cameraRigRot_hud.setTextColor(Color.YELLOW);
        cameraRigRot_hud.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        cameraRigRot_hud.getTransform().setPosition(-0.03f, -0.03f, -0.5f);
        cameraRigRot_hud.getTransform().setScale(0.2f, 0.2f, 0.2f);
        cameraRigRot_data.setDepthTest(false);
        cameraRigRot_data.setAlphaBlend(true);
        cameraRigRot_data.setRenderingOrder(SXRRenderData.SXRRenderingOrder.OVERLAY);

        SXRRenderData rate_hud_data = frameRate_hud.getRenderData();
        frameRate_hud.setTextSize(3.5f);
        frameRate_hud.setTextColor(Color.YELLOW);
        frameRate_hud.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        frameRate_hud.getTransform().setPosition(-0.03f, -0.09f, -0.5f);
        frameRate_hud.getTransform().setScale(0.2f, 0.2f, 0.2f);
        rate_hud_data.setDepthTest(false);
        rate_hud_data.setAlphaBlend(true);
        rate_hud_data.setRenderingOrder(SXRRenderData.SXRRenderingOrder.OVERLAY);

    }



    private class SampleMain extends SXRMain {
        @Override
        public void onInit(SXRContext gvrContext) {


            scene = gvrContext.getMainScene();
            scene.setBackgroundColor(0, 0, 0, 0);

            try {


                // Logo
                //SXRTexture texture = gvrContext.getAssetLoader().loadTexture(new SXRAndroidResource(gvrContext, R.drawable.gearvr_logo));

                // create a scene object (this constructor creates a rectangular scene
                // object that uses the standard texture shader
                //SXRNode sceneObject = new SXRNode(gvrContext, 0.15f, 0.15f, texture);



                // Go Board
                SXRTexture texture = gvrContext.getAssetLoader().loadTexture(new SXRAndroidResource(gvrContext, "bamboo_block_9x9.png"));
                SXRNode sceneObject = new SXRNode(gvrContext, gvrContext.getAssetLoader().loadMesh(new SXRAndroidResource(gvrContext, "GoBoard9x9.fbx")), texture);
                sceneObject.getTransform().setScale(0.1f, 0.1f, 0.1f);


                // set the scene object position
                sceneObject.getTransform().setPosition(0.0f, 0.0f, 0.0f);
                sceneObject.getTransform().rotateByAxis(-90, 1, 0, 0);
                //sceneObject.getTransform().setPosition(0.0f, 0.0f, -0.5f);

                // add the scene object to the scene graph
                scene.addNode(sceneObject);
                //scene.getMainCameraRig().addChildObject(sceneObject);

            }
            catch (Exception e) {}

            hud = new SXRTextViewNode(gvrContext, 2.0f, 1.5f, "Testing");
            pos_hud = new SXRTextViewNode(gvrContext, 2.0f, 1.5f,"C_POS X:0, Y:0, Z:0" );
            rot_hud = new SXRTextViewNode(gvrContext, 2.0f, 1.5f, "C_ROT: W:0, X:0, Y:0, Z:0");
            cameraRigPos_hud = new SXRTextViewNode(gvrContext, 2.0f, 1.5f,"E_POS X:0, Y:0, Z:0" );
            cameraRigRot_hud = new SXRTextViewNode(gvrContext, 2.0f, 1.5f, "E_ROT W:0, X:0, Y:0, Z:0");
            frameRate_hud = new SXRTextViewNode(gvrContext, 2.0f, 2.0f, "CV Frame Rate: 0");

            makeHuds();

            scene.getMainCameraRig().addChildObject(hud);
            scene.getMainCameraRig().addChildObject(pos_hud);
            scene.getMainCameraRig().addChildObject(cameraRigPos_hud);
            scene.getMainCameraRig().addChildObject(rot_hud);
            scene.getMainCameraRig().addChildObject(cameraRigRot_hud);
            scene.getMainCameraRig().addChildObject(frameRate_hud);
        }

        @Override
        public void onStep(){
            SXRTransform cameraRigTransform = scene.getMainCameraRig().getHeadTransform();

            String s = String.format("E_POS X: %.2f, Y: %.2f, Z: %.2f", cameraRigTransform.getPositionX(), cameraRigTransform.getPositionY(), cameraRigTransform.getPositionZ());
            String s1 = String.format("E_ROT P: %.2f, Y: %.2f, R: %.2f", cameraRigTransform.getRotationPitch(), cameraRigTransform.getRotationYaw(), cameraRigTransform.getRotationRoll());

            if (cameraRigPos_hud != null && cameraRigRot_hud != null){
                cameraRigPos_hud.setText(s);
                cameraRigRot_hud.setText(s1);

            }
        }

    }
}
