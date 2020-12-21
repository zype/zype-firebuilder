package com.amazon.android.tv.tenfoot.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.ContextCompat;

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

    }
}