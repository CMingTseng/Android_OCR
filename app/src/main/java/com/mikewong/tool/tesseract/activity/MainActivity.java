package com.mikewong.tool.tesseract.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

import com.mikewong.tool.tesseract.R;
import com.mikewong.tool.tesseract.fragment.MainFragment;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setFragment(new MainFragment(), false, false);
    }

    public void setFragment(Fragment fragment, boolean stack, boolean anim) {
        try {
            final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (anim) {
                transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
            }
            transaction.replace(R.id.main_content, fragment);
            if (stack) {
                Log.d(TAG, "setFragment() addToBackStack : " + fragment.getClass().getSimpleName());
                transaction.addToBackStack(fragment.getClass().getSimpleName());
            }
            transaction.commit();
        } catch (IllegalStateException ex) {
            Log.d(TAG, "setFragment(): illegal state: " + ex.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
