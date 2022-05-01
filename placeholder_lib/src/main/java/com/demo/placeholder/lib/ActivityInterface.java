package com.demo.placeholder.lib;

import android.app.Activity;
import android.os.Bundle;

public interface ActivityInterface {

    void insertAppContext(Activity activity);

    void onCreate(Bundle savedInstanceState);

    void onStart();

    void onResume();

    void onPause();

    void onStop();

    void onDestroy();
}
