package com.amazon.android.tv.tenfoot.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.utils.Preferences;
import com.zype.fire.api.ZypeSettings;

/**
 * A class to display content in a vertical grid.
 */
public class TermsConditionActivity extends Activity {

    private static final String PREFERENCE_TERMS = "ZypeTerms";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.terms_n_condition_layout);
        WebView webView = (WebView) findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
//        webView.loadUrl(ZypeSettings.TERMS_CONDITION_URL);
        webView.loadData(Preferences.getString(PREFERENCE_TERMS), "text/html", "UTF-8");

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

    }
}