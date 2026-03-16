package com.example.cmput301_app;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.cmput301_app.database.TestDataManager;
import com.google.android.gms.tasks.Tasks;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.TimeUnit;
import android.util.Log;

/**
 * A bridge to allow running TestDataManager commands via the terminal.
 */
@RunWith(AndroidJUnit4.class)
public class TestDataRunner {

    private static final String TAG = "TestDataRunner";

    @Test
    public void addData() throws Exception {
        Log.d(TAG, "Starting addData test...");
        try {
            // Increased timeout to 60 seconds to allow for slow emulator connections
            Tasks.await(new TestDataManager().addTestData(), 60, TimeUnit.SECONDS);
            Log.d(TAG, "addData test completed successfully.");
        } catch (Exception e) {
            Log.e(TAG, "addData test failed: " + e.getMessage());
            throw e;
        }
    }

    @Test
    public void deleteData() throws Exception {
        Log.d(TAG, "Starting deleteData test...");
        try {
            Tasks.await(new TestDataManager().deleteAllTestData(), 60, TimeUnit.SECONDS);
            Log.d(TAG, "deleteData test completed successfully.");
        } catch (Exception e) {
            Log.e(TAG, "deleteData test failed: " + e.getMessage());
            throw e;
        }
    }
}
