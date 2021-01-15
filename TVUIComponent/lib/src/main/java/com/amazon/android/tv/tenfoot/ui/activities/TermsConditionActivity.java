package com.amazon.android.tv.tenfoot.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.ContextCompat;

import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.base.BaseActivity;
import com.amazon.android.utils.Preferences;
import com.zype.fire.api.ZypeSettings;

/**
 * A class to display content in a vertical grid.
 */
public class TermsConditionActivity extends BaseActivity {
    private static final String TAG = TermsConditionActivity.class.getSimpleName();

    private static final String PREFERENCE_TERMS = "ZypeTerms";

    private WebView webView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.terms_n_condition_layout);
        webView = findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setBackgroundColor(ContextCompat.getColor(this, R.color.browse_background_color));
//        webView.loadUrl(ZypeSettings.TERMS_CONDITION_URL);
        String textColor = ZypeSettings.LIGHT_THEME ? "#000" : "#fff";
        String finalHtml = "<html><head>"
                + "<style type=\"text/css\">body {color: " + textColor + "} "
                + "</style></head>"
                + "<body>"
                + Preferences.getString(PREFERENCE_TERMS)
                + "</body></html>";
        webView.loadDataWithBaseURL(null, finalHtml, "text/html", "UTF-8", null);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, final String url) {
                findViewById(R.id.progress).setVisibility(View.GONE);
            }
        });

        if (ZypeSettings.SHOW_TOP_MENU) {
            hideTopMenu();
        }

    }

    @Override
    public void setRestoreActivityValues() {
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "event=" + event.toString());

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MENU:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (!isMenuOpened) {
                        if (ZypeSettings.SHOW_TOP_MENU) {
                            showTopMenu();
                            return true;
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_BACK: {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    Log.d(TAG, "Back button pressed");
                    if (isMenuOpened) {
                        if (ZypeSettings.SHOW_TOP_MENU) {
                            hideTopMenu();
                            return true;
                        }
                    }
                }
                break;
            }
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.d(TAG, "Up button pressed");
                if (!isMenuOpened && ZypeSettings.SHOW_TOP_MENU) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (webView.getScrollY() == 0) {
                            showTopMenu();
                            return true;
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.d(TAG, "Down button pressed");
                if (isMenuOpened) {
                    if (ZypeSettings.SHOW_TOP_MENU) {
                        hideTopMenu();
                        return true;
                    }
                }
                break;
        }
        return super.dispatchKeyEvent(event);
    }


}