package com.samsungxr.avatardemo;

import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

import com.samsungxr.SXRActivity;
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRBoxCollider;
import com.samsungxr.SXRCameraRig;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDirectLight;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRSpotLight;
import com.samsungxr.SXRTexture;
import com.samsungxr.SXRTransform;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.animation.SXRAvatar;
import com.samsungxr.animation.SXRRepeatMode;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.nodes.SXRCubeNode;
import com.samsungxr.nodes.SXRSphereNode;
import com.samsungxr.physics.SXRCollisionMatrix;
import com.samsungxr.physics.SXRPhysicsJoint;
import com.samsungxr.physics.SXRPhysicsLoader;
import com.samsungxr.physics.SXRRigidBody;
import com.samsungxr.physics.SXRWorld;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AvatarMain extends SXRMain
{
//    private final String mModelPath = "YBot/ybot.fbx";
    //private final String[] mAnimationPaths =  { "YBot/Zombie_Stand_Up_mixamo.com.bvh", "YBot/Football_Hike_mixamo.com.bvh" };
    //private final String mBoneMapPath = "animation/mixamo/mixamo_map.txt";
    private final String mModelPath = "female_caucasian_adult/FemaleBody.gltf";
    private final String[] mAnimationPaths =  { "animation/Motion_Body_HappyDance.bvh" };
    private final String mBoneMapPath = "female_caucasian_adult/DMbonemap.txt";
    private static final String TAG = "AVATAR";
    private SXRContext mContext;
    private SXRScene mScene;
    private SXRActivity mActivity;
    private int mNumAnimsLoaded = 0;
    private String mBoneMap;
    private SXRWorld mWorld;
    private SXRNode mPhysicsRoot;
    private SXRAvatar mAvatar;

    public AvatarMain(SXRActivity activity) {
        mActivity = activity;
    }

    private SXRAvatar.IAvatarEvents mAvatarListener = new SXRAvatar.IAvatarEvents()
    {
        @Override
        public void onAvatarLoaded(final SXRAvatar avatar, final SXRNode avatarRoot, String filePath, String errors)
        {
            mAvatar = avatar;
            if (avatarRoot.getParent() == null)
            {
                mPhysicsRoot.addChildObject(avatarRoot);
//                loadPhysics("female_caucasian_adult/FemaleBody.avt", avatar.getSkeleton());
                loadPhysics("TestNoLimbs.avt", avatar.getSkeleton());
                //loadNextAnimation(avatar, mBoneMap);
            }
        }


        @Override
        public void onAnimationLoaded(SXRAvatar avatar, SXRAnimator animation, String filePath, String errors)
        {
            SXRAnimation anim = animation.getAnimation(0);
            if (anim instanceof SXRSkeletonAnimation)
            {
                ((SXRSkeletonAnimation) anim).scaleKeys(0.01f);
            }
            animation.setRepeatMode(SXRRepeatMode.ONCE);
            animation.setSpeed(1f);
            avatar.setBlend(1);
            ++mNumAnimsLoaded;
            if (!avatar.isRunning())
            {
                avatar.startAll(SXRRepeatMode.REPEATED);
            }
            else
            {
                avatar.start(animation.getName());
            }
            loadNextAnimation(avatar, mBoneMap);
        }

        public void onModelLoaded(SXRAvatar avatar, final SXRNode avatarRoot, String filePath, String errors) { }

        public void onAnimationFinished(SXRAvatar avatar, SXRAnimator animator) { }

        public void onAnimationStarted(SXRAvatar avatar, SXRAnimator animator) { }
    };


    @Override
    public void onInit(SXRContext ctx)
    {
        mContext = ctx;
        mScene = ctx.getMainScene();
        mPhysicsRoot = new SXRNode(ctx);
        SXRCameraRig rig = mScene.getMainCameraRig();
        rig.getOwnerObject().getTransform().setPosition(0, 0.8fx, 0.5f);
        rig.setNearClippingDistance(0.1f);
        rig.setFarClippingDistance(50);
        mScene.addNode(mPhysicsRoot);
        makeEnvironment(ctx, mScene);
        mScene.setFrustumCulling(false);
        ctx.getInputManager().selectController();
    }

    @Override
    public void onAfterInit()
    {
        loadAvatar("female_caucasian_adult");
        //loadPhysics("Test.avt", null);
    }

    private void loadAvatar(String avatarFile)
    {
//        SXRAvatar avatar = new SXRAvatar(ctx, "YBot");
        SXRAvatar avatar = new SXRAvatar(mContext, avatarFile);
        avatar.getEventReceiver().addListener(mAvatarListener);
        mBoneMap = readFile(mBoneMapPath);

        try
        {
            avatar.loadModel(new SXRAndroidResource(mContext, mModelPath));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            mActivity.finish();
            mActivity = null;
        }
    }

    private SXRNode makeEnvironment(SXRContext ctx, SXRScene scene)
    {
        SXRNode env = new SXRNode(ctx);
        SXRDirectLight topLight = new SXRDirectLight(ctx);
        SXRSpotLight headLight = new SXRSpotLight(ctx);
        SXRNode topLightObj = new SXRNode(ctx);
        SXRCameraRig rig = scene.getMainCameraRig();
        SXRMaterial floorMtl = new SXRMaterial(ctx, SXRMaterial.SXRShaderType.Phong.ID);
        SXRMaterial skyMtl = new SXRMaterial(ctx, SXRMaterial.SXRShaderType.Phong.ID);
        SXRNode skyBox = new SXRSphereNode(ctx, false, skyMtl);
        SXRNode floor = new SXRCubeNode(ctx, true, new Vector3f(30, 10, 30));
        SXRBoxCollider floorCollider = new SXRBoxCollider(ctx);
        SXRRigidBody floorBody = new SXRRigidBody(ctx, 0, 2);

        floorBody.setSimulationType(SXRRigidBody.STATIC);
        floorBody.setRestitution(0.5f);
        floorBody.setFriction(1.0f);
        try
        {
            SXRTexture  floorTex = ctx.getAssetLoader().loadTexture(new SXRAndroidResource(ctx, "checker.png"));
            floorMtl.setMainTexture(floorTex);
        }
        catch (IOException ex)
        {
            Log.e(TAG, ex.getMessage());
        }
        headLight.setInnerConeAngle(50.0f);
        headLight.setOuterConeAngle(60.0f);
        floorMtl.setAmbientColor(0.7f, 0.6f, 0.5f, 1);
        floorMtl.setDiffuseColor(0.7f, 0.6f, 0.5f, 1);
        floorMtl.setSpecularColor(1, 1, 0.8f, 1);
        floorMtl.setSpecularExponent(4.0f);
        floor.getRenderData().setMaterial(floorMtl);
        floor.getTransform().setPositionY(-5);
        floor.getRenderData().setCastShadows(false);
        skyBox.getRenderData().setCastShadows(false);
        skyBox.getTransform().setScale(20,  20, 20);
        skyMtl.setAmbientColor(0.1f, 0.25f, 0.25f, 1.0f);
        skyMtl.setDiffuseColor(0.3f, 0.5f, 0.5f, 1.0f);
        skyMtl.setSpecularColor(0, 0, 0, 1);
        skyMtl.setSpecularExponent(0);
        rig.getHeadTransformObject().attachComponent(headLight);
//        headLight.setShadowRange(0.1f, 20);
        topLightObj.attachComponent(topLight);
        topLightObj.getTransform().rotateByAxis(-90, 1, 0, 0);
        topLightObj.getTransform().setPosition(0, 2, -1);
//        topLight.setShadowRange(0.1f, 50);
        env.addChildObject(topLight);
        env.addChildObject(skyBox);
        env.addChildObject(floor);
        mPhysicsRoot.addChildObject(env);

        SXRCollisionMatrix cm = new SXRCollisionMatrix();
        cm.enableCollision(0, 1);
        cm.enableCollision(0, 2);
        cm.enableCollision(1, 0);
        cm.enableCollision(1, 2);
        mWorld = new SXRWorld(mScene, cm, false);
        floor.attachCollider(floorCollider);
        floor.attachComponent(floorBody);
        return env;
    }

    public void loadPhysics(String physicsFile)
    {
        try
        {
            SXRAndroidResource res = new SXRAndroidResource(mContext, physicsFile);
            SXRNode root = SXRPhysicsLoader.loadPhysicsFile(res, mScene.getRoot(), false);
            mAvatar.getSkeleton().poseFromBones();
            mWorld.setEnable(true);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            mContext.getActivity().finish();
        }
    }

    private void loadNextAnimation(SXRAvatar avatar, String bonemap)
    {
        if (mNumAnimsLoaded >= mAnimationPaths.length)
        {
            return;
        }
        try
        {
            SXRAndroidResource res =
                new SXRAndroidResource(mContext, mAnimationPaths[mNumAnimsLoaded]);
            avatar.loadAnimation(res, bonemap);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            mActivity.finish();
            mActivity = null;
            Log.e(TAG, "Animation could not be loaded from " + mAnimationPaths[mNumAnimsLoaded]);
        }
    }

    public void loadPhysics(String physicsFile, SXRSkeleton avatarSkel)
    {
        try
        {
            SXRNode physicsRoot = SXRPhysicsLoader.loadPhysicsFile(mScene, physicsFile);

            if ((physicsRoot != null) && (avatarSkel != null))
            {
                List<SXRComponent> components = physicsRoot.getAllComponents(SXRPhysicsJoint.getComponentType());

                for (SXRComponent c : components)
                {
                    SXRPhysicsJoint rootJoint = (SXRPhysicsJoint) c;
                    if (rootJoint.getBoneID() == 0)
                    {
                        rootJoint.getSkeleton();
//                        rootJoint.mapPoseToSkeleton(avatarSkel, 10000);
                    }
                }
            }
            else
            {
                List<SXRComponent> components = mScene.getRoot().getAllComponents(SXRRigidBody.getComponentType());

                for (SXRComponent c : components)
                {
                    SXRRigidBody body = (SXRRigidBody) c;
                    SXRNode owner = body.getOwnerObject();

                    if (owner.getRenderData() == null)
                    {
                        continue;
                    }
                    String name = owner.getName();
                    SXRTransform t = owner.getTransform();
                    Matrix4f m = t.getModelMatrix4f();
                    Vector3f p = new Vector3f();
                    m.getTranslation(p);
                    float red = 0;
                    float green = Math.abs(p.y / 5.0f);
                    float blue = 0;

                    if (p.x > 0)
                    {
                        blue = 8.0f * p.x;
                    }
                    else if (p.x < 0)
                    {
                        red = 8.0f * Math.abs(p.x);
                    }
                    if (name.contains("head"))
                    {
                        green = 0;
                        red = blue = 0.5f;
                    }
                    if (name.contains("neck"))
                    {
                        green = 0.8f;
                        red = blue = 0.5f;
                    }

                    SXRMaterial mtl = new SXRMaterial(mContext, SXRMaterial.SXRShaderType.Phong.ID);

                    mtl.setDiffuseColor(red, green, blue, 1);
                    if (body.getMass() > 0)
                    {
                        owner.getRenderData().setMaterial(mtl);
                    }
                }
            }
        }
        catch (IOException ex)
        {
            Log.e(TAG, "Problem loading physics file " + physicsFile + " " + ex.getMessage());
        }
    }

    @Override
    public void onSingleTapUp(MotionEvent event)
    {
        SXRNode debugDraw = mWorld.setupDebugDraw();

//        debugDraw.getTransform().setPositionZ(1.5f);
        mScene.addNode(debugDraw);
        mWorld.setDebugMode(-1);
        mWorld.setEnable(true);
    }


    private String readFile(String filePath)
    {
        try
        {
            SXRAndroidResource res = new SXRAndroidResource(getSXRContext(), filePath);
            InputStream stream = res.getStream();
            byte[] bytes = new byte[stream.available()];
            stream.read(bytes);
            stream.close();
            return new String(bytes);
        } catch (IOException ex)
        {
            return null;
        }
    }

    public void onStep()
    {
        if (mAvatar != null)
        {
//            mAvatar.getSkeleton().poseFromBones();
        }

    }

    }
