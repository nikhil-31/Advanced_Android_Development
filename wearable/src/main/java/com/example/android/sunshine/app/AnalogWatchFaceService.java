package com.example.android.sunshine.app;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.example.android.sunshine.app.Utilities.getIconResourceForWeatherCondition;
import static com.example.android.sunshine.app.Utilities.getstringforweatherCondition;

public class AnalogWatchFaceService extends CanvasWatchFaceService {

    // Create the new engine that draws the watchFace
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);


    /* Typefaces to be used for the text elements*/
    private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);


    // Implement the service callback methods
    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        static final int MSG_UPDATE_TIME = 0;

        // Stroke widths for the watch hands
        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 4;
        private static final int TEMP_SHADOW_RADIUS = 2;

        private float mCenterX;
        private float mCenterY;

        Calendar mCalendar;

        // device features
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mAmbient;

        private boolean mRegisteredTimeZoneReciever = false;

        // Background for the watch
        private Bitmap mBackgroundBitmap;
        private Bitmap mBackgroundScaledBitmap;

        // Paint objects for the watch face
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mTickAndCirclePaint;
        private Paint mAmPmPaint;
        private Paint mmaxTempPaint;
        private Paint mminTempPaint;
        private Paint mWeatherTextPaint;


        // Watch hands colors
        private int mWatchHandColor;
        private int mWatchHandShadowColor;
        private int mWatchHandHighlightColor;

        // Watch Hand lengths
        private float mHourHandLength;
        private float mMinuteHandLength;
        private float mSecondHandLength;

        String mAmString = "AM";
        String mPmString = "PM";

        private int mAmPmXoffset;
        private int mAmPmYoffset;

        private int mIconOffsetX;
        private int mIconOffsetY;

        private int mMaxTempOffsetX;
        private int mMinTempOffsetX;

        private int mWeatherTextOffsetX;
        private int mWeatherTextOffsetY;

        private GoogleApiClient mgoogleApiClient;

        private String maxTemp = "0";
        private String minTemp = "0";
        private int weatherId = 200;


        // handler to update the time once a second in interactive mode
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        // receiver to update the time zone
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };


        // Create your watchface
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            // Configure the system UI
            // Configures peeking cards to be a single line tall, background of peekCard is shown briefly
            // and only for interruptive notifications, and system time to not be sown
            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());


            // Load the background image
            // The Background bitmap is loaded only once when the system initializes the watchface
            Resources resources = AnalogWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.blue, null);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            // Default Watch hands colors
            mWatchHandColor = Color.WHITE;
            mWatchHandShadowColor = Color.BLACK;
            mWatchHandHighlightColor = Color.RED;

            // Create graphic styles for the Watch hands
            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchHandHighlightColor);
            mSecondPaint.setStrokeWidth(SECOND_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStyle(Paint.Style.STROKE);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchHandColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_STROKE_WIDTH);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mWeatherTextPaint = createTextPaint(mWatchHandColor);
            mWeatherTextPaint.setTextSize(15);

            mAmPmPaint = createTextPaint(mWatchHandColor);
            mAmPmPaint.setTextSize(20);
            mAmPmPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mmaxTempPaint = createTextPaint(mWatchHandColor);
            mmaxTempPaint.setTextSize(40);
            mmaxTempPaint.setShadowLayer(TEMP_SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mminTempPaint = createTextPaint(mWatchHandColor);
            mminTempPaint.setTextSize(25);
            mminTempPaint.setShadowLayer(TEMP_SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            // allocate a calendar to calculate local time using the UTC time and time zone
            mCalendar = Calendar.getInstance();

            mgoogleApiClient = new GoogleApiClient.Builder(AnalogWatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        // Device features like burn-in protection and low-bit ambient mode.
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

        }

        // When the time changes. It is called every minute by the system and it is sufficient to update in ambient mode
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            boolean isRound = insets.isRound();

            mAmPmXoffset = (isRound ? 110 : 90);
            mAmPmYoffset = (isRound ? 5 : 5);

            mIconOffsetX = (isRound ? 60 : 50);
            mIconOffsetY = (isRound ? 160 : 140);

            mMaxTempOffsetX = (isRound ? 35 : 25);
            mMinTempOffsetX = (isRound ? 40 : 30);

            mWeatherTextOffsetX = (isRound ? 50 :40);
            mWeatherTextOffsetY = (isRound ? 200:200);


        }

        // When the mode is changed
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;

            updateWatchHandStyle();
            invalidate();

            // Check and trigger whether or not timer should be running (only runs in ambient mode)
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);
                mTickAndCirclePaint.setColor(Color.WHITE);
                mAmPmPaint.setColor(Color.WHITE);
                mmaxTempPaint.setColor(Color.WHITE);
                mminTempPaint.setColor(Color.WHITE);
                mWeatherTextPaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondPaint.setAntiAlias(false);
                mTickAndCirclePaint.setAntiAlias(false);
                mAmPmPaint.setAntiAlias(false);
                mmaxTempPaint.setAntiAlias(false);
                mminTempPaint.setAntiAlias(false);
                mWeatherTextPaint.setAntiAlias(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondPaint.clearShadowLayer();
                mTickAndCirclePaint.clearShadowLayer();
                mAmPmPaint.clearShadowLayer();
                mmaxTempPaint.clearShadowLayer();
                mminTempPaint.clearShadowLayer();
                mWeatherTextPaint.clearShadowLayer();
            } else {
                mHourPaint.setColor(mWatchHandColor);
                mMinutePaint.setColor(mWatchHandColor);
                mSecondPaint.setColor(mWatchHandHighlightColor);
                mTickAndCirclePaint.setColor(mWatchHandColor);
                mAmPmPaint.setColor(mWatchHandColor);
                mmaxTempPaint.setColor(mWatchHandColor);
                mminTempPaint.setColor(mWatchHandColor);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondPaint.setAntiAlias(true);
                mTickAndCirclePaint.setAntiAlias(true);
                mAmPmPaint.setAntiAlias(true);
                mmaxTempPaint.setAntiAlias(true);
                mminTempPaint.setAntiAlias(true);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mAmPmPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mmaxTempPaint.setShadowLayer(TEMP_SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mminTempPaint.setShadowLayer(TEMP_SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            }

        }

        // Scale the background to fit the device any time the view changes
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            // Find the center. Ignore the window insets so that the round watches
            // with a "chin". the watch face is centered on the entire screen, not just
            // the usable portion

            mCenterX = width / 2f;
            mCenterY = height / 2f;

            // Calculate the lengths of different hands based on watch screen size
            mHourHandLength = (float) (mCenterX * 0.5);
            mMinuteHandLength = (float) (mCenterX * 0.75);
            mSecondHandLength = (float) (mCenterX * 0.875);

            if (mBackgroundScaledBitmap == null || mBackgroundScaledBitmap.getWidth() != width || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap, width, height, true);
            }

        }

        // Draw your watch face
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            // Update the time
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            // Drawing the background
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            }

            /* Code to draw the ticks on the watchface, usually it should be created with the background,
            *  but in cases where we have to allow users to select their own photos, this dynamically
            *  created them to top of the photo*/

            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) (Math.sin(tickRot) * outerTickRadius);
                float outerY = (float) (-Math.cos(tickRot) * outerTickRadius);

                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint);

            }

            // Compute rotations and lengths for the clock hands
            final float seconds = (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;


//            canvas.drawText(getAmPmString(mCalendar.get(Calendar.AM_PM)), mCenterX + mAmPmXoffset, mCenterY + mAmPmYoffset, mAmPmPaint);

            canvas.drawText(maxTemp + "\u00b0", mCenterX + mMaxTempOffsetX, mCenterY + 45, mmaxTempPaint);
            canvas.drawText(minTemp + "\u00b0", mCenterX + mMinTempOffsetX, mCenterY + 80, mminTempPaint);

//            String weatherText =
//            canvas.drawText();

            if (!mAmbient) {
                int icon = getIconResourceForWeatherCondition(weatherId);
                Bitmap weatherIcon = BitmapFactory.decodeResource(getResources(), icon);
                canvas.drawBitmap(weatherIcon, mIconOffsetX, mIconOffsetY, null);
            }

            if(mAmbient){
                String weatherCondtition = getstringforweatherCondition(weatherId);
                canvas.drawText(weatherCondtition ,50,200,mWeatherTextPaint);

            }

            // Save the canvas state before we can begin to rotate it
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mMinuteHandLength,
                    mMinutePaint
            );

            // Seconds should only be shown in the interactive mode
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint
                );

                canvas.drawCircle(
                        mCenterX,
                        mCenterY,
                        CENTER_GAP_AND_CIRCLE_RADIUS,
                        mTickAndCirclePaint
                );


            }

            //Restore the canvas original orientation
            canvas.restore();
        }

        private String getAmPmString(int ampm) {
            return ampm == Calendar.AM ? mAmString : mPmString;
        }


        // The watch face becomes visible or invisible
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Connect to recieve messages from the mobile.
                mgoogleApiClient.connect();

                // Update the time zone when the watchface becomes visible
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReciever();

                if (mgoogleApiClient != null && mgoogleApiClient.isConnected()) {
                    mgoogleApiClient.disconnect();
                }
            }

            // If updating the timer should be running depends on whether we're visible and
            // whether we're in ambient mode, so we may need to start or stop the timer
            updateTimer();

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReciever) {
                return;
            }
            mRegisteredTimeZoneReciever = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            AnalogWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReciever() {
            if (!mRegisteredTimeZoneReciever) {
                return;
            }

            mRegisteredTimeZoneReciever = false;
            AnalogWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }


        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mgoogleApiClient, this);
        }


        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    processConfigurationFor(item);
                }
            }

            dataEvents.release();
            invalidate();
        }

        private void processConfigurationFor(DataItem item) {
            if ("/wear_face".equals(item.getUri().getPath())) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                if (dataMap.containsKey("HIGH_TEMP"))
                    maxTemp = dataMap.getString("HIGH_TEMP");
                if (dataMap.containsKey("LOW_TEMP"))
                    minTemp = dataMap.getString("LOW_TEMP");
                if (dataMap.containsKey("WEATHER_ID"))
                    weatherId = dataMap.getInt("WEATHER_ID");
            }
        }


        @Override
        public void onConnectionSuspended(int i) {
            Log.d("Connection", "Failed");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d("Connection", "Failed" );
        }


    }


}
