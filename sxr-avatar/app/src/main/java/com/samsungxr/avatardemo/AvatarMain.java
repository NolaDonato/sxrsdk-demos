package com.samsungxr.avatardemo;

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
import com.samsungxr.SXRScene;
import com.samsungxr.SXRSpotLight;
import com.samsungxr.SXRTexture;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.animation.SXRAvatar;
import com.samsungxr.animation.SXRRepeatMode;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.animation.SXRPoseMapper;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.nodes.SXRCubeNode;
import com.samsungxr.nodes.SXRSphereNode;
import com.samsungxr.physics.PhysicsAVTConverter;
import com.samsungxr.physics.SXRCollisionMatrix;
import com.samsungxr.physics.SXRConstraint;
import com.samsungxr.physics.SXRFixedConstraint;
import com.samsungxr.physics.SXRPhysicsCollidable;
import com.samsungxr.physics.SXRPhysicsContent;
import com.samsungxr.physics.SXRPhysicsJoint;
import com.samsungxr.physics.SXRPhysicsLoader;
import com.samsungxr.physics.SXRRigidBody;
import com.samsungxr.physics.SXRWorld;

import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AvatarMain extends SXRMain
{
//    private final String mModelPath = "YBot/ybot.fbx";
    //private final String[] mAnimationPaths =  { "YBot/Zombie_Stand_Up_mixamo.com.bvh", "YBot/Football_Hike_mixamo.com.bvh" };
    //private final String mBoneMapPath = "animation/mixamo/mixamo_map.txt";
    private final String mModelPath = "female/FemaleBody.gltf";
    private final String[] mAnimationPaths =  { "animation/Motion_Body_HappyDance.bvh" };
    private final String mBoneMapPath = "female/DMbonemap.txt";
    private static final String TAG = "AVATAR";
    private SXRContext mContext;
    private SXRScene mScene;
    private SXRActivity mActivity;
    private int mNumAnimsLoaded = 0;
    private String mBoneMap;
    private SXRWorld mWorld;
    private SXRNode mGeometryRoot;
    private SXRAvatar mAvatar;
    private SXRSkeleton mAvatarSkel = null;
    private SXRPoseMapper mPhysicsToAvatar = null;
    private SXRSkeleton mPhysicsSkel = null;

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
                mGeometryRoot.addChildObject(avatarRoot);
                mAvatarSkel = avatar.getSkeleton();
                mAvatarSkel.poseToBones();
                try
                {
                    String headDesc = readFile("female/FemaleHead.json");
                    avatar.loadModel(new SXRAndroidResource(mContext, "female/Head_Female.glb"), headDesc, "head_JNT");
                }
                catch (IOException e) { }

                loadPhysics("female/FemaleBody.avt", mAvatarSkel);
//                loadPhysics("/sdcard/AvatarFashion/physics/avatar.bullet", mAvatarSkel);
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

        public void onModelLoaded(SXRAvatar avatar, final SXRNode modelRoot, String filePath, String errors)
        {
            if ((modelRoot == null) || "head".equals(avatar.findModelName(modelRoot)))
            {
                loadHair("hair/myemojihair_Long25_Male.gltf");
            }
        }

        public void onAnimationFinished(SXRAvatar avatar, SXRAnimator animator) { }

        public void onAnimationStarted(SXRAvatar avatar, SXRAnimator animator) { }
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
        rig.getTransform().setPosition(0, 0.8f, 1.5f);
        mScene.addNode(mGeometryRoot);
        makeEnvironment(ctx, mScene);
        mScene.setFrustumCulling(false);
        ctx.getInputManager().selectController();
    }

    public void onAfterInit()
    {
        loadAvatar("female/FemaleBody.gltf");
    }

    private void loadAvatar(String avatarFile)
    {
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

    private void loadHair(String hairFile)
    {
        try
        {
            String hairDesc = readFile("hair/hair_long25.json");

            mAvatar.loadModel(new SXRAndroidResource(mContext, hairFile), hairDesc, "hair");
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
        env.setName("Environment");
        mGeometryRoot.addChildObject(env);

        SXRCollisionMatrix cm = new SXRCollisionMatrix();

        cm.enableCollision(SXRCollisionMatrix.DEFAULT_GROUP, SXRCollisionMatrix.STATIC_GROUP);

        mWorld = new SXRWorld(mScene, cm, true);
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

    public void loadPhysics(String physicsFile, final SXRSkeleton avatarSkel)
    {
        final PhysicsAVTConverter loader = new PhysicsAVTConverter(mContext);

        loader.setMultiBody(true);
        loader.getEventReceiver().addListener(new SXRPhysicsLoader.IPhysicsLoaderEvents()
        {
            @Override
            public void onPhysicsLoaded(SXRPhysicsContent world, SXRSkeleton skel, String filename)
            {
                SXRNode physicsRoot = world.getOwnerObject();
                if ((physicsRoot != null) && (avatarSkel != null) && (skel != null))
                {
                    mPhysicsSkel = skel;
                    mPhysicsSkel.disable();
                    mPhysicsToAvatar = new SXRPoseMapper(avatarSkel, mPhysicsSkel, 100000);
                }
                loader.exportPhysics((SXRWorld) world, "/storage/emulated/0/AvatarFashion/avatars/female_avatar.bullet");
                if (world != mWorld)
                {
                    mWorld.getOwnerObject().addChildObject(physicsRoot);
                    mWorld.merge(world);
                }
                loadHairPhysics("hair/myemojihair_Long25_Male.avt");
            }

            @Override
            public void onLoadError(String filename, String errors)
            {
                Log.e(TAG, "Problem loading physics file " + filename + " " + errors);
            }
        });
        try
        {
            SXRAndroidResource res = new SXRAndroidResource(mContext, physicsFile);
            loader.loadPhysics(mWorld, res, false);
//            loader.loadPhysics(res, false);
        }
        catch (IOException e) {  return; }
    }

    public void loadHairPhysics(String physicsFile)
    {
        if (mPhysicsSkel == null)
        {
            return;
        }
        final PhysicsAVTConverter loader = new PhysicsAVTConverter(mContext);
        loader.setMultiBody(true);
        loader.getEventReceiver().addListener(new SXRPhysicsLoader.IPhysicsLoaderEvents()
        {
            @Override
            public void onPhysicsLoaded(final SXRPhysicsContent world, final SXRSkeleton skel, String filename)
            {
                loader.exportPhysics((SXRWorld) world, "/storage/emulated/0/AvatarFashion/geometry/Hair/Hair_Long25/myemojihair_Long25_Male.bullet");
                if (mWorld != world)
                {
                    int attachIndex1 = mPhysicsSkel.getBoneIndex("head_JNT");
                    int attachIndex2 = skel.getBoneIndex("head_JNT");
                    final SXRNode attachNode1 = mPhysicsSkel.getBone(attachIndex1);
                    final SXRNode attachNode2 = skel.getBone(attachIndex2);
                    SXRPhysicsJoint attachJoint1 = (SXRPhysicsJoint) attachNode1.getComponent(SXRPhysicsJoint.getComponentType());
                    SXRPhysicsJoint attachJoint2 = (SXRPhysicsJoint) attachNode2.getComponent(SXRPhysicsJoint.getComponentType());

                    if ((attachJoint1 != null) && (attachJoint2 != null))
                    {
                        attachJoint1.merge(mPhysicsSkel, skel);
                        mWorld.merge(world);
                        return;
                    }
                    SXRRigidBody attachBody1 = (SXRRigidBody) attachNode1.getComponent(SXRRigidBody.getComponentType());
                    SXRRigidBody attachBody2 = (SXRRigidBody) attachNode2.getComponent(SXRRigidBody.getComponentType());
                    final SXRPhysicsCollidable bodyA = (attachBody1 != null) ? attachBody1 : attachJoint1;
                    final SXRPhysicsCollidable bodyB = (attachBody2 != null) ? attachBody2 : attachJoint2;
                    if ((bodyA != null) && (bodyB != null))
                    {
                        mWorld.run(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                mPhysicsSkel.merge(skel, "head_JNT");
                                mWorld.merge(world);
                                SXRFixedConstraint constraint = new SXRFixedConstraint(getSXRContext(), bodyB);
                                attachNode1.attachComponent(constraint);
                            }
                        });
                    }
                 }
            }

            @Override
            public void onLoadError(String filename, String errors)
            {
                Log.e(TAG, "Problem loading physics file " + filename + " " + errors);
            }
        });
        try
        {
            SXRAndroidResource res = new SXRAndroidResource(mContext, physicsFile);
            loader.loadPhysics(mWorld, res, mPhysicsSkel, "head_JNT");
//            loader.loadPhysics(res, false);
        }
        catch (IOException e) {  return; };

    }

    @Override
    public void onSingleTapUp(MotionEvent event)
    {
// Include the following 3 lines for Bullet debug draw
//        SXRNode debugDraw = mWorld.setupDebugDraw();
//        mScene.addNode(debugDraw);
//        mWorld.setDebugMode(-1);
        mPhysicsSkel.enable();
        mWorld.setEnable(true);
    }

    @Override
    public void onStep()
    {
        if (mPhysicsToAvatar != null)
        {
            SXRSkeleton physicsSkel = mPhysicsToAvatar.getSourceSkeleton();

            if (physicsSkel.isEnabled())
            {
                physicsSkel.poseFromBones();
                mPhysicsToAvatar.animate(0);
            }
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
