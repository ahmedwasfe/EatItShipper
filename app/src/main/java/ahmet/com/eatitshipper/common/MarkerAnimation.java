package ahmet.com.eatitshipper.common;


import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import ahmet.com.eatitshipper.callback.LatLngInterpolator;

public class MarkerAnimation {

    public static void animateMarkerToGB(final Marker marker, LatLng finalPosition,
                                         LatLngInterpolator latLngInterpolator){

        LatLng startPosition = marker.getPosition();
        Handler handler = new Handler();
        long start = SystemClock.uptimeMillis();
        Interpolator interpolator = new AccelerateDecelerateInterpolator();
        // 3 secound
        float durationInMs = 3000;

        handler.post(new Runnable() {

            long elapsed;
            float t, v;

            @Override
            public void run() {
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                marker.setPosition(latLngInterpolator.interpolate(v, startPosition, finalPosition));

                // Repeat till progress is complete
                if (t < 1){
                    // Post again 16ms lateer
                    handler.postDelayed(this::run, 16);
                }
            }
        });
    }

    // HC mean HONEYCOMB
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void animateMarkerToHC(final Marker marker, LatLng finalPosition,
                                         LatLngInterpolator latLngInterpolator){

        LatLng startLocation = marker.getPosition();

        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.addUpdateListener(valueAnimator1 -> {

            float v = valueAnimator1.getAnimatedFraction();
            LatLng newPosition = latLngInterpolator.interpolate(v, startLocation, finalPosition);
            marker.setPosition(newPosition);
        });

        valueAnimator.setFloatValues(0,1);
        valueAnimator.setDuration(3000);
        valueAnimator.start();
    }

    // ICS mean ICE_CREAM_SANDWICH
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void animateMarkerToICS(final Marker marker, LatLng finalPosition,
                                         LatLngInterpolator latLngInterpolator){

        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };

        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class,"position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);
        animator.setDuration(3000);
        animator.start();
    }
}
