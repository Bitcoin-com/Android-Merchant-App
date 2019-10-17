package com.bitcoin.merchant.app.screens;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.bitcoin.merchant.app.BuildConfig;
import com.bitcoin.merchant.app.R;
//import android.util.Log;

public class AboutActivity extends Activity {
    private TextView tvAbout = null;
    private ImageView aboutLogo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_about);
        tvAbout = findViewById(R.id.about);
        aboutLogo = findViewById(R.id.about_logo);
        tvAbout.setText(getString(R.string.about, BuildConfig.VERSION_NAME, "2019"));
        LinearLayout root = (LinearLayout) tvAbout.getParent().getParent();
        Toolbar toolbar = (Toolbar) LayoutInflater.from(AboutActivity.this).inflate(R.layout.settings_toolbar, root, false);
        toolbar.setTitle(R.string.about_title);
        toolbar.setTitleTextColor(Color.BLACK);
        toolbar.setNavigationIcon(R.drawable.ic_back_black_24dp);
        root.addView(toolbar, 0);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        aboutLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bitcoin.com/"));
                startActivity(browserIntent);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.onBackPressed();
    }
}
