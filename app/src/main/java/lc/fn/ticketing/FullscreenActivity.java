package lc.fn.ticketing;

import android.annotation.SuppressLint;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.content.Intent;
import android.net.Uri;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends ActionBarActivity implements View.OnClickListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = getWindow(); // in Activity's onCreate() for instance
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_fullscreen);

        mContentView = findViewById(R.id.fullscreen_content);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.scan).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
            startActivityForResult(intent, 0);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                String[] bits = contents.split("/ticket/");
                if (bits.length != 2) {
                    error("INVALID\n\nURL PARSE FAILED");
                    return;
                }
                String id = bits[1];
                info("CHECKING\n\n" + id);
                // Instantiate the RequestQueue.
                RequestQueue queue = Volley.newRequestQueue(this);
                String url ="http://tickets.ubccsss.org/api/ticket/"+id;

                final FullscreenActivity self = this;

                // Request a string response from the provided URL.
                JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                // Display the first 500 characters of the response string.
                                try {
                                    self.success(response.getString("FirstName") + " " + response.getString("LastName") + "\n\n" + response.getString("PhoneNumber")+ "\n" + response.getString("Email"));
                                } catch (JSONException e) {
                                    self.error("ERROR\n\nINVALID JSON RETURNED");
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse.statusCode == 400) {
                            self.error("INVALID\n\nNOT FOUND");
                        } else {
                            self.error("ERROR\n\nCODE: " + error.networkResponse.statusCode);
                        }
                    }
                });
// Add the request to the RequestQueue.
                queue.add(stringRequest);
            }
            if(resultCode == RESULT_CANCELED){
                info("CANCELED");
            }
        }
    }
    void success(String text){
        setText(text);
        setColor(0xff43A047, 0xff66BB6A);
    }
    void error(String text) {
        setText(text);
        setColor(0xffc62828, 0xfff44336);
    }
    void info(String text) {
        setText(text);
        setColor(0xff0099cc,0xff33b5e5);
    }
    void setText(String text) {
        TextView disp = (TextView) findViewById(R.id.fullscreen_content);
        disp.setText(text);
    }
    void setColor(int background, int text) {
        TextView disp = (TextView) findViewById(R.id.fullscreen_content);
        disp.setTextColor(text);
        FrameLayout disp2 = (FrameLayout) findViewById(R.id.background);
        disp2.setBackgroundColor(background);
    }
}
