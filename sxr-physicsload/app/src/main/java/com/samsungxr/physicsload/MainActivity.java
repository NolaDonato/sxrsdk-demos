/* Copyright 2018 Samsung Electronics Co., LTD
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

package com.samsungxr.physicsload;

import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;

import com.samsungxr.SXRActivity;
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRBoxCollider;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDirectLight;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.physics.SXRPhysicsContent;
import com.samsungxr.physics.SXRPhysicsLoader;
import com.samsungxr.physics.SXRRigidBody;
import com.samsungxr.physics.SXRWorld;
import com.samsungxr.nodes.SXRCubeNode;
import com.samsungxr.utility.Log;

import java.io.IOException;

/*
 * This is a simple application to demonstrate how to use SXRPhysicsLoader to load bullet files.
 *
 * Before loading any bullet file it is necessary to init physics world (create SXRWorld and attach
 * it to main scene root object). Also it is required to create and add to the scene all objects
 * that will have rigid body attached to it. These objects must have a name and this name must
 * match the rigid body name set on bullet file. If you are using an authoring tool like Blender
 * a name is automatically set to each object, and the same name will be used for the rigid body
 * when exporting to bullet file.
 *
 * After initializing physics world and creating the required objects bullet file can be loaded
 * using a single method call. SXRPhysicsLoader will look for the required objects in the scene
 * and attach rigid bodies and constraints to them. Some physics components present in the file
 * may not be used and will be discarded.
 *
 * This sample application uses objects and bullet file exported by Blender (see Blender project
 * in 'extras' directory) and also another bullet file created from a bullet application.
 */

public class MainActivity extends SXRActivity {
    private SXRWorld mWorld;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setMain(new Main());
        enableGestureDetector();
    }

    SXRPhysicsLoader mPhysicsLoader;

    private final class Main extends SXRMain {
        private SXRWorld mWorld;

        private SXRPhysicsLoader.IPhysicsLoaderEvents mLoadHandler = new SXRPhysicsLoader.IPhysicsLoaderEvents() {
            @Override
            public void onPhysicsLoaded(SXRPhysicsContent world, String filename)
            {
                SXRNode root = world.getOwnerObject();

                world.getSXRContext().getMainScene().addNode(root);
                mWorld.merge(world);
            }

            @Override
            public void onLoadError(String filename, String errors)
            {
                Log.e("PhysicsLoad",  errors);
            }
        };

        @Override
        public void onInit(SXRContext sxrContext) {
            initScene(sxrContext);
            initPhysics(sxrContext);
//            loadBlenderAssets(sxrContext);
//            complementScene(sxrContext);
//            mWorld.enable();
        }

        void initScene(SXRContext sxrContext) {
            SXRScene mainScene = sxrContext.getMainScene();

            // Camera and light settings were copied from Blender project available in 'extras'
            // directory
            mainScene.getMainCameraRig().getHeadTransform().setPosition(0f, 2.4f, 40f);
            mainScene.getMainCameraRig().setFarClippingDistance(100f);
            mainScene.getMainCameraRig().setNearClippingDistance(0.1f);

            SXRNode sunObj = new SXRCubeNode(sxrContext);
            sunObj.getTransform().setPosition(8f, 3.4f, 41.7f);
            sunObj.getTransform().setRotation(0.8683812142694567f, -0.3738122646181239f, -0.06100199997212902f, -0.32008938364834f);
            SXRDirectLight sun = new SXRDirectLight(sxrContext);
            sun.setDiffuseIntensity(1f, 1f, 1f, 1f);
            sun.setSpecularIntensity(1f, 1f, 1f, 1f);
            sunObj.attachComponent(sun);

            SXRNode sun1Obj = new SXRCubeNode(sxrContext);
            sun1Obj.getTransform().setPosition(-15f, -1.38f, -32f);
            sun1Obj.getTransform().setRotation(0.7071067811865476f, -0.7071067811865476f, 0.0f, 0.0f);
            SXRDirectLight sun1 = new SXRDirectLight(sxrContext);
            sun1.setDiffuseIntensity(1f, 1f, 1f, 1f);
            sun1.setSpecularIntensity(1f, 1f, 1f, 1f);
            sun1Obj.attachComponent(sun1);
        }

        void initPhysics(SXRContext sxrContext) {
            SXRScene mainScene = sxrContext.getMainScene();

            mWorld = new SXRWorld(mainScene, true);
            mWorld.setGravity(0f, -9.81f, 0f);
            mainScene.getRoot().attachComponent(mWorld);
            // Include the following 3 lines for Bullet debug draw
            SXRNode debugDraw = mWorld.setupDebugDraw();
            mainScene.addNode(debugDraw);
            createFloor(sxrContext);
            mWorld.setDebugMode(-1);

            try
            {
                SXRPhysicsLoader loader = new SXRPhysicsLoader(sxrContext);
                loader.getEventReceiver().addListener(mLoadHandler);
                SXRAndroidResource res = new SXRAndroidResource(sxrContext, "quadruped.urdf");

                loader.loadPhysics(res, false);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        void loadModel(SXRContext sxrContext, String fname) throws IOException {
            sxrContext.getAssetLoader().loadModel(fname, sxrContext.getMainScene());
        }

        void loadBlenderAssets(SXRContext sxrContext) {
            SXRScene mainScene = sxrContext.getMainScene();

            try {
                // 'Cone' and 'Cone.001' will be linked by a Hinge constraint
                loadModel(sxrContext,"Cone.fbx");
                loadModel(sxrContext,"Cone_001.fbx");

                // 'Cube' and 'Cube.001' will be linked by a Cone-twist constraint
                loadModel(sxrContext,"Cube.fbx");
                loadModel(sxrContext,"Cube_001.fbx");

                // 'Cube.002' and 'Cube.003' will be linked by a Generic 6DoF constraint
                loadModel(sxrContext,"Cube_002.fbx");
                loadModel(sxrContext,"Cube_003.fbx");

                loadModel(sxrContext,"Cube_004.fbx");

                // 'Cylinder' and 'Sphere' will be linked by a Point-to-point constraint
                loadModel(sxrContext,"Cylinder.fbx");
                loadModel(sxrContext,"Sphere.fbx");

                // Plane object is not being loaded due to an issue when exporting this kind of
                // object from Blender to SXRf with physics properties
//                loadModel(sxrContext,"Plane.fbx");

                // Up-axis must be ignored because scene objects were rotated when exported
                SXRAndroidResource r = new SXRAndroidResource(sxrContext, "scene3.bullet");
                SXRPhysicsLoader loader = new SXRPhysicsLoader(sxrContext);
                loader.loadPhysics(mainScene, r, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void complementScene(SXRContext sxrContext)
        {
            SXRScene mainScene = sxrContext.getMainScene();

            // 'bodyA' and 'bodyB' will be linked by a Fixed constraint
            SXRMaterial redMat = new SXRMaterial(sxrContext, SXRMaterial.SXRShaderType.Phong.ID);
            redMat.setDiffuseColor(1f, 0f, 0f, 1f);
            SXRNode box1 = new SXRCubeNode(sxrContext, true, redMat);
            box1.getTransform().setPosition(5f, 5f, 10f);
            box1.setName("bodyA");
//            box1.attachComponent(new SXRMeshCollider(sxrContext, true));
            mainScene.addNode(box1);

            SXRMaterial whiteMat = new SXRMaterial(sxrContext, SXRMaterial.SXRShaderType.Phong.ID);
            whiteMat.setDiffuseColor(1f, 1f, 1f, 1f);
            SXRNode box2 = new SXRCubeNode(sxrContext, true, whiteMat);
            box2.getTransform().setPosition(5f, 10f, 10f);
            box2.setName("bodyB");
//            box2.attachComponent(new SXRMeshCollider(sxrContext, true));
            mainScene.addNode(box2);

            // 'bodyP' and 'bodyQ' will be linked by a Slider constraint
            SXRMaterial blueMat = new SXRMaterial(sxrContext, SXRMaterial.SXRShaderType.Phong.ID);
            blueMat.setDiffuseColor(0f, 0f, 1f, 1f);
            SXRNode box3 = new SXRCubeNode(sxrContext, true, blueMat);
            box3.getTransform().setPosition(-5f, 10f, 10f);
            box3.setName("bodyP");
//            box3.attachComponent(new SXRMeshCollider(sxrContext, true));
            mainScene.addNode(box3);

            SXRMaterial greenMat = new SXRMaterial(sxrContext, SXRMaterial.SXRShaderType.Phong.ID);
            greenMat.setDiffuseColor(0f, 1f, 0f, 1f);
            SXRNode box4 = new SXRCubeNode(sxrContext, true, greenMat);
            box4.getTransform().setPosition(-10f, 10f, 10f);
            box4.setName("bodyQ");
//            box4.attachComponent(new SXRMeshCollider(sxrContext, true));
            mainScene.addNode(box4);

            SXRMaterial yellowMat = new SXRMaterial(sxrContext, SXRMaterial.SXRShaderType.Phong.ID);
            yellowMat.setDiffuseColor(1f, 1f, 0f, 1f);
            SXRNode box5 = new SXRCubeNode(sxrContext, true, yellowMat);
            box5.getTransform().setPosition(-4.5f, 5f, 10.5f);
            box5.setName("barrier");
//            box5.attachComponent(new SXRMeshCollider(sxrContext, true));
            mainScene.addNode(box5);

            // This bullet file was created from a bullet application to add fixed and slider
            // constraints that are not available on Blender
            try {
                SXRPhysicsLoader loader = new SXRPhysicsLoader(sxrContext);

                loader.loadPhysics(mainScene, "fixed_slider.bullet");
            } catch (IOException e) {
                e.printStackTrace();
            }
            createFloor(sxrContext);
            // This object will replace the "Plane" exported by Blender as the floor of this scene
            SXRMaterial orangeMat = new SXRMaterial(sxrContext, SXRMaterial.SXRShaderType.Phong.ID);
            orangeMat.setDiffuseColor(0.7f, 0.3f, 0f, 1f);
            SXRNode floor = new SXRNode(sxrContext, 100f, 100f);
            floor.getTransform().setPosition(0f, -10f, 0f);
            floor.getTransform().setRotationByAxis(-90f, 1f, 0f, 0f);
            floor.getRenderData().setMaterial(orangeMat);
            floor.attachComponent(new SXRBoxCollider(sxrContext));
            mainScene.addNode(floor);
            SXRRigidBody floorRb = new SXRRigidBody(sxrContext, 0f);
            floor.attachComponent(floorRb);
        }

        private SXRNode createFloor(SXRContext ctx)
        {
            SXRMaterial orangeMat = new SXRMaterial(ctx, SXRMaterial.SXRShaderType.Phong.ID);
            orangeMat.setDiffuseColor(0.7f, 0.3f, 0f, 1f);
            SXRNode floor = new SXRNode(ctx, 100f, 100f);
            floor.getTransform().setPosition(0f, -10f, 0f);
            floor.getTransform().setRotationByAxis(-90f, 1f, 0f, 0f);
            floor.getRenderData().setMaterial(orangeMat);
            floor.attachComponent(new SXRBoxCollider(ctx));
            ctx.getMainScene().addNode(floor);
            SXRRigidBody floorRb = new SXRRigidBody(ctx, 0f);
            floor.attachComponent(floorRb);
            return floor;
        }

        @Override
        public void onSingleTapUp(MotionEvent event)
        {
            // Include the following 3 lines for Bullet debug draw
//            SXRNode debugDraw = mWorld.setupDebugDraw();
//            mWorld.getSXRContext().getMainScene()/addNode(debugDraw);
//            mWorld.setDebugMode(-1);
            mWorld.setEnable(true);
        }
    }


}
