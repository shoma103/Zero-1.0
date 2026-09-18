package com.zero.browser;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF08080C, 0xFF0B0B11, 0xFF13131C}));

        View glow = new View(this);
        GradientDrawable glowBg = new GradientDrawable();
        glowBg.setShape(GradientDrawable.OVAL);
        glowBg.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glowBg.setGradientRadius(500f);
        glowBg.setColors(new int[]{0x557C3AED, 0x33EC4899, 0x00000000});
        glow.setBackground(glowBg);
        int glowSize = (int)(getResources().getDisplayMetrics().density * 340);
        FrameLayout.LayoutParams glowLp = new FrameLayout.LayoutParams(glowSize, glowSize);
        glowLp.gravity = Gravity.CENTER;
        glow.setLayoutParams(glowLp);
        glow.setAlpha(0f);
        glow.setScaleX(0.5f);
        glow.setScaleY(0.5f);
        root.addView(glow);

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        TextView logo = new TextView(this);
        logo.setText("Zero");
        logo.setTextSize(78);
        logo.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        logo.setTextColor(0xFFFFFFFF);
        logo.setLetterSpacing(-0.04f);
        logo.setGravity(Gravity.CENTER);

        TextView tagline = new TextView(this);
        tagline.setText("Минимализм. Скорость. Приватность.");
        tagline.setTextSize(13);
        tagline.setTextColor(0xFF8B8B96);
        tagline.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        tagLp.topMargin = (int)(getResources().getDisplayMetrics().density * 14);
        tagline.setLayoutParams(tagLp);

        center.addView(logo);
        center.addView(tagline);
        root.addView(center);
        setContentView(root);

        logo.post(() -> {
            float w = Math.max(logo.getWidth(), 600);
            LinearGradient shader = new LinearGradient(
                -w, 0, 0, 0,
                new int[]{
                    0xFFA78BFA, 0xFFEC4899, 0xFFF59E0B, 0xFF06B6D4, 0xFFA78BFA
                },
                null, Shader.TileMode.CLAMP);
            logo.getPaint().setShader(shader);

            ValueAnimator sweep = ValueAnimator.ofFloat(0f, 2f * w);
            sweep.setDuration(2200);
            sweep.setRepeatCount(ValueAnimator.INFINITE);
            sweep.addUpdateListener(a -> {
                float v = (float) a.getAnimatedValue();
                Matrix m = new Matrix();
                m.setTranslate(v, 0);
                logo.getPaint().getShader().setLocalMatrix(m);
                logo.invalidate();
            });
            sweep.start();
        });

        logo.setAlpha(0f);
        logo.setScaleX(0.35f);
        logo.setScaleY(0.35f);
        logo.setRotation(-10f);
        tagline.setAlpha(0f);
        tagline.setTranslationY(28f);

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .rotation(0f)
            .setDuration(750)
            .setInterpolator(new OvershootInterpolator(1.6f))
            .start();

        glow.animate()
            .alpha(1f)
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setStartDelay(150)
            .setDuration(900)
            .withEndAction(() -> glow.animate()
                .alpha(0f)
                .scaleX(2.0f)
                .scaleY(2.0f)
                .setDuration(900)
                .start())
            .start();

        tagline.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(380)
            .setDuration(550)
            .start();

        root.postDelayed(() -> root.animate()
            .alpha(0f)
            .setDuration(380)
            .withEndAction(() -> {
                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                if (getIntent() != null && getIntent().getData() != null) {
                    i.setData(getIntent().getData());
                }
                startActivity(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            })
            .start(), 1900);
    }
}
