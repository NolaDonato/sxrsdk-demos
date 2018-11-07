package com.samsungxr.avatar_fashion;

import com.samsungxr.SXRActivity;
import com.samsungxr.SXRMain;
import com.samsungxr.widgetlib.log.Log;
import com.samsungxr.widgetlib.main.WidgetLib;

import android.os.Bundle;

import static com.samsungxr.utility.Log.tag;

public class AvatarFashionActivity extends SXRActivity {
    private SXRMain mMain;
    private final static String TAG = tag(AvatarFashionActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Log.init(this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onCreate!");
        super.onCreate(savedInstanceState);

        mMain = new AvatarFashionMain(this);
        setMain(mMain, "sxr.xml");
        enableGestureDetector();
    }

    public void setMyMain(SXRMain main) {
        mMain = main;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setMain(mMain, "sxr.xml");
            }
        });
    }

    @Override
    protected void onDestroy() {
        try {
            if (WidgetLib.isInitialized()) {
                WidgetLib.destroy();
            }
        } finally {
            // make sure super is called last always, it clears internal sxrf
            // references and things may crash
            super.onDestroy();
        }
    }

}
