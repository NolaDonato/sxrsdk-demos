package com.samsungxr.avatardemo;

import android.view.MotionEvent;

import com.samsungxr.SXRActivity;
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRBoxCollider;
import com.samsungxr.SXRCameraRig;;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDirectLight;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRSpotLight;
import com.samsungxr.SXRTexture;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.animation.SXRAvatar;
import com.samsungxr.animation.SXRRepeatMode;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.nodes.SXRCubeNode;
import com.samsungxr.nodes.SXRSphereNode;
import com.samsungxr.physics.PhysicsAVTConverter;
import com.samsungxr.physics.SXRAvatarPhysics;
import com.samsungxr.physics.SXRCollisionMatrix;
import com.samsungxr.physics.SXRPhysicsContent;
import com.samsungxr.physics.SXRPhysicsLoader;
import com.samsungxr.physics.SXRRigidBody;
import com.samsungxr.physics.SXRWorld;
import com.samsungxr.utility.Log;

import org.joml.Vector3f;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AvatarMain extends SXRMain
{
//    private final String mModelPath = "YBot/ybot.fbx";
    //private final String[] mAnimationPaths =  { "YBot/Zombie_Stand_Up_mixamo.com.bvh", "YBot/Football_Hike_mixamo.com.bvh" };
    //private final String mBoneMapPath = "animation/mixamo/mixamo_map.txt";
    private final String mAvatarDesc = "female/FemaleBody.json";
    private final String mHairDesc = "hair/hair_long29.json";
    private final String[] mAnimationPaths =  { "animations/Motion_Body_HappyDance.bvh" };
    private static final String TAG = "AVATAR";
    private SXRContext mContext;
    private SXRScene mScene;
    private SXRActivity mActivity;
    private int mNumAnimsLoaded = 0;
    private String mBoneMap;
    private SXRWorld mWorld;
    private SXRNode mGeometryRoot;
    private SXRAvatar mAvatar;
    private SXRAvatarPhysics mAvatarPhysics;
    private final int HAIR_COLLISION_GROUP = SXRCollisionMatrix.NEXT_COLLISION_GROUP;

    public AvatarMain(SXRActivity activity) {
        mActivity = activity;
    }

    private SXRAvatar.IAvatarEvents mAvatarListener = new SXRAvatar.IAvatarEvents()
    {
        @Override
        public void onAvatarLoaded(final SXRAvatar avatar, final SXRNode avatarRoot, String filePath, String errors)
        {
            if (avatarRoot.getParent() == null)
            {
                SXRSkeleton skel = avatar.getSkeleton();

                mGeometryRoot.addChildObject(avatarRoot);
                mBoneMap = avatar.getProperty("bonemap");
                if (mBoneMap != null)
                {
                    mBoneMap = readFile(mBoneMap);
                }
                synchronized (skel)
                {
                    skel.poseToBones();
                    for (int i = 0; i < skel.getNumBones(); ++i)
                    {
                        String name = skel.getBoneName(i);
                        skel.setBoneOptions(i, name.startsWith("hair") ?
                            SXRSkeleton.BONE_PHYSICS : SXRSkeleton.BONE_ANIMATE);
                    }
                }
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

        public void onModelLoaded(SXRAvatar avatar, final SXRNode modelRoot, String filePath, String errors)
        {
            String modelType = avatar.findModelType(modelRoot);

            if ("hair".equals(modelType))
            {
                SXRSkeleton skel = avatar.getSkeleton();
                synchronized (skel)
                {
                    for (int i = 0; i < skel.getNumBones(); ++i)
                    {
                        String name = skel.getBoneName(i);
                        if (name.startsWith("hair"))
                        {
                            skel.setBoneOptions(i, SXRSkeleton.BONE_PHYSICS);
                        }
                    }
                }
            }
        }

        public void onAnimationFinished(SXRAvatar avatar, SXRAnimator animator) { }

        public void onAnimationStarted(SXRAvatar avatar, SXRAnimator animator) { }
    };

    private SXRPhysicsLoader.IPhysicsLoaderEvents mPhysicsListener = new SXRPhysicsLoader.IPhysicsLoaderEvents()
    {
        @Override
        public void onPhysicsLoaded(SXRPhysicsContent world, SXRSkeleton skel, String filename)
        {
            if (!mAvatar.isRunning())
            {
                loadHair(mHairDesc);
                loadNextAnimation(mAvatar, mBoneMap);
            }
            mAvatarPhysics.getPhysicsLoader().getEventReceiver().removeListener(mPhysicsListener);
        }

        @Override
        public void onLoadError(SXRPhysicsContent world, String filename, String errors)
        {

        }
    };

    @Override
    public void onInit(SXRContext ctx)
    {
        mContext = ctx;
        mScene = ctx.getMainScene();
        mGeometryRoot = new SXRNode(ctx);
        mGeometryRoot.setName("Geometry");
        SXRCameraRig rig = mScene.getMainCameraRig();
        rig.setNearClippingDistance(0.1f);
        rig.setFarClippingDistance(50);
        rig.getTransform().setPosition(0, 0.8f, 1);
        mScene.setFrustumCulling(false);
        mScene.addNode(mGeometryRoot);
        makeEnvironment(ctx, mScene);
        mScene.setFrustumCulling(false);
        ctx.getInputManager().selectController();
    }

    public void onAfterInit()
    {
        loadAvatar(mAvatarDesc);
    }

    private void loadAvatar(String avatarDescPath)
    {
        try
        {
            String desc = readFile(avatarDescPath);
            JSONObject avatarDesc = new JSONObject(desc);
            SXRAvatar avatar = new SXRAvatar(mContext, avatarDesc.getString("name"));
            String bonemap = avatarDesc.getString("bonemap");
            String modelFile = avatarDesc.getString("model");

            mAvatar = avatar;
            avatar.getEventReceiver().addListener(mAvatarListener);
            mBoneMap = readFile(bonemap);
            mAvatarPhysics = new SXRAvatarPhysics(avatar, mWorld, new PhysicsAVTConverter(getSXRContext()));
            mAvatarPhysics.getPhysicsLoader().getEventReceiver().addListener(mPhysicsListener);
            avatar.loadModel(new SXRAndroidResource(mContext, modelFile), desc);
        }
        catch (IOException | JSONException e)
        {
            Log.e(TAG, "Cannot read avatar decription file " + avatarDescPath);
            mActivity.finish();
            mActivity = null;
        }
    }

    private void loadHair(String hairDescPath)
    {
        try
        {
            String hairDesc = readFile(hairDescPath);
            JSONObject jsonDesc = new JSONObject(hairDesc);
            String modelFile = jsonDesc.getString("model");

            mAvatar.loadModel(new SXRAndroidResource(mContext, modelFile), hairDesc, "hair");
        }
        catch (IOException | JSONException e)
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
        SXRNode floor = new SXRCubeNode(ctx, true, new Vector3f(30, 4, 30));
        SXRBoxCollider floorCollider = new SXRBoxCollider(ctx);
        SXRRigidBody floorBody = new SXRRigidBody(ctx, 0, SXRCollisionMatrix.STATIC_GROUP);

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
        floor.getTransform().setPositionY(-3);
        floor.getRenderData().setCastShadows(false);
        floor.setName("floor");
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
        env.setName("Environment");
        mGeometryRoot.addChildObject(env);

        SXRCollisionMatrix cm = new SXRCollisionMatrix();

        cm.enableCollision(SXRCollisionMatrix.DEFAULT_GROUP, SXRCollisionMatrix.STATIC_GROUP);
        cm.disableCollision(HAIR_COLLISION_GROUP, HAIR_COLLISION_GROUP);
        cm.enableCollision(HAIR_COLLISION_GROUP, SXRCollisionMatrix.KINEMATIC_GROUP);

        mWorld = new SXRWorld(mScene, cm, false);
        // Include the following 3 lines for Bullet debug draw
//        SXRNode debugDraw = mWorld.setupDebugDraw(20000);
//        mScene.addNode(debugDraw);
//        mWorld.setDebugMode(-1);

        floor.attachCollider(floorCollider);
        floor.attachComponent(floorBody);
        return env;
    }

    private void loadNextAnimation(SXRAvatar avatar, String bonemap)
    {
        if (mNumAnimsLoaded >= mAnimationPaths.length)
        {
            return;
        }
        try
        {
            SXRAndroidResource res = new SXRAndroidResource(mContext, mAnimationPaths[mNumAnimsLoaded]);
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

    @Override
    public void onSingleTapUp(MotionEvent event)
    {
        if (mAvatarPhysics.isRunning())
        {
            mAvatarPhysics.stop();
        }
        else
        {
            mAvatarPhysics.start();
        }
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
        }
        catch (IOException ex)
        {
            return null;
        }
    }

    }
