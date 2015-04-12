package edu.northwestern.cbits.purple_robot_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DigitalWatchFaceService extends CanvasWatchFaceService
{
    private static final String TAG = "DigitalWatchFaceService";

    private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final long NORMAL_UPDATE_RATE_MS = 500;
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    private static final long mWatchStart = System.currentTimeMillis();

    @Override
    public Engine onCreateEngine()
    {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine
    {
        static final String COLON_STRING = ":";
        static final int MUTE_ALPHA = 100;
        static final int NORMAL_ALPHA = 255;
        static final int MSG_UPDATE_TIME = 0;
        long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

        final Handler mUpdateTimeHandler = new Handler()
        {
            @Override
            public void handleMessage(Message message)
            {
                switch (message.what)
                {
                    case MSG_UPDATE_TIME:
                        Engine.this.invalidate();

                        if (shouldTimerBeRunning())
                        {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }

                        break;
                }

                Intent heartbeat = new Intent(DigitalWatchFaceService.this, HeartbeatService.class);
                DigitalWatchFaceService.this.startService(heartbeat);
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mAmPmPaint;
        Paint mColonPaint;
        Paint mSamplesPaint;

        float mColonWidth;
        boolean mMute;
        Time mTime;
        boolean mShouldDrawColons;
        float mXOffset;
        float mYOffset;
        String mAmString;
        String mPmString;
        int mInteractiveBackgroundColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_BACKGROUND;
        int mAmbientBackgroundColor = DigitalWatchFaceUtil.COLOR_VALUE_AMBIENT_BACKGROUND;
        int mInteractiveHourDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS;
        int mInteractiveMinuteDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS;
        int mInteractiveSecondDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder)
        {
            super.onCreate(holder);

            this.setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = DigitalWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mAmString = resources.getString(R.string.digital_am);
            mPmString = resources.getString(R.string.digital_pm);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mInteractiveBackgroundColor);
            mHourPaint = createTextPaint(mInteractiveHourDigitsColor, BOLD_TYPEFACE);
            mMinutePaint = createTextPaint(mInteractiveMinuteDigitsColor);
            mSecondPaint = createTextPaint(mInteractiveSecondDigitsColor);
            mAmPmPaint = createTextPaint(resources.getColor(R.color.digital_am_pm));
            mColonPaint = createTextPaint(resources.getColor(R.color.digital_colons));

            mSamplesPaint = createTextPaint(mInteractiveHourDigitsColor, NORMAL_TYPEFACE);

            mTime = new Time();
        }

        @Override
        public void onDestroy()
        {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int defaultInteractiveColor)
        {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface)
        {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible)
        {
            super.onVisibilityChanged(visible);

            if (visible)
            {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            }
            else
                unregisterReceiver();

            updateTimer();
        }

        private void registerReceiver()
        {
            if (mRegisteredTimeZoneReceiver)
                return;

            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            DigitalWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver()
        {
            if (!mRegisteredTimeZoneReceiver)
                return;

            mRegisteredTimeZoneReceiver = false;
            DigitalWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets)
        {
            super.onApplyWindowInsets(insets);

            Resources resources = DigitalWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float amPmSize = resources.getDimension(isRound ? R.dimen.digital_am_pm_size_round : R.dimen.digital_am_pm_size);
            float samplesSize = resources.getDimension(isRound ? R.dimen.digital_samples_size_round : R.dimen.digital_samples_size);

            mHourPaint.setTextSize(textSize);
            mMinutePaint.setTextSize(textSize);
            mSecondPaint.setTextSize(textSize);
            mAmPmPaint.setTextSize(amPmSize);
            mColonPaint.setTextSize(textSize);
            mSamplesPaint.setTextSize(samplesSize);

            mColonWidth = mColonPaint.measureText(COLON_STRING);
        }

        @Override
        public void onPropertiesChanged(Bundle properties)
        {
            super.onPropertiesChanged(properties);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick()
        {
            super.onTimeTick();

            this.invalidate();

            Intent heartbeat = new Intent(DigitalWatchFaceService.this, HeartbeatService.class);
            DigitalWatchFaceService.this.startService(heartbeat);
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode)
        {
            super.onAmbientModeChanged(inAmbientMode);

            if (inAmbientMode)
                adjustPaintColorToCurrentMode(mBackgroundPaint, mInteractiveBackgroundColor, DigitalWatchFaceUtil.COLOR_VALUE_AMBIENT_BACKGROUND);
            else
                adjustPaintColorToCurrentMode(mBackgroundPaint, mInteractiveBackgroundColor, DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_BACKGROUND);

            adjustPaintColorToCurrentMode(mHourPaint, mInteractiveHourDigitsColor, DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
            adjustPaintColorToCurrentMode(mMinutePaint, mInteractiveMinuteDigitsColor, DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS);
            adjustPaintColorToCurrentMode(mSamplesPaint, mInteractiveMinuteDigitsColor, DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS);
            // Actually, the seconds are not rendered in the ambient mode, so we could pass just any
            // value as ambientColor here.
            adjustPaintColorToCurrentMode(mSecondPaint, mInteractiveSecondDigitsColor, DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS);

            if (mLowBitAmbient){
                boolean antiAlias = !inAmbientMode;

                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mAmPmPaint.setAntiAlias(antiAlias);
                mColonPaint.setAntiAlias(antiAlias);
                mSamplesPaint.setAntiAlias(antiAlias);
            }

            this.invalidate();

            Intent heartbeat = new Intent(DigitalWatchFaceService.this, HeartbeatService.class);
            DigitalWatchFaceService.this.startService(heartbeat);

            this.updateTimer();
        }

        private void adjustPaintColorToCurrentMode(Paint paint, int interactiveColor, int ambientColor)
        {
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter)
        {
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
            setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : NORMAL_UPDATE_RATE_MS);

            if (mMute != inMuteMode)
            {
                mMute = inMuteMode;
                int alpha = inMuteMode ? MUTE_ALPHA : NORMAL_ALPHA;
                mHourPaint.setAlpha(alpha);
                mMinutePaint.setAlpha(alpha);
                mColonPaint.setAlpha(alpha);
                mAmPmPaint.setAlpha(alpha);
                mSamplesPaint.setAlpha(alpha);
                this.invalidate();

                Intent heartbeat = new Intent(DigitalWatchFaceService.this, HeartbeatService.class);
                DigitalWatchFaceService.this.startService(heartbeat);
            }
        }

        public void setInteractiveUpdateRateMs(long updateRateMs)
        {
            if (updateRateMs == mInteractiveUpdateRateMs)
                return;

            mInteractiveUpdateRateMs = updateRateMs;

            // Stop and restart the timer so the new update rate takes effect immediately.
            if (shouldTimerBeRunning())
                updateTimer();
        }

        private String formatTwoDigitNumber(int hour)
        {
            return String.format("%02d", hour);
        }

        private int convertTo12Hour(int hour)
        {
            int result = hour % 12;
            return (result == 0) ? 12 : result;
        }

        private String getAmPmString(int hour)
        {
            return (hour < 12) ? mAmString : mPmString;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds)
        {
            mTime.setToNow();

            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            // Draw the background.
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            // Draw the hours.
            float x = mXOffset;
            String hourString = String.valueOf(convertTo12Hour(mTime.hour));
            canvas.drawText(hourString, x, mYOffset, mHourPaint);
            x += mHourPaint.measureText(hourString);

            // In ambient and mute modes, always draw the first colon. Otherwise, draw the
            // first colon for the first half of each second.
            if (isInAmbientMode() || mMute || mShouldDrawColons) {
                canvas.drawText(COLON_STRING, x, mYOffset, mColonPaint);
            }

            x += mColonWidth;

            float samplesOffset = x;

            // Draw the minutes.
            String minuteString = formatTwoDigitNumber(mTime.minute);
            canvas.drawText(minuteString, x, mYOffset, mMinutePaint);
            x += mMinutePaint.measureText(minuteString);

            // In ambient and mute modes, draw AM/PM. Otherwise, draw a second blinking
            // colon followed by the seconds.
            if (isInAmbientMode() || mMute)
            {
                x += mColonWidth;
                canvas.drawText(getAmPmString(mTime.hour), x, mYOffset, mAmPmPaint);
            }
            else
            {
                if (mShouldDrawColons)
                    canvas.drawText(COLON_STRING, x, mYOffset, mColonPaint);

                x += mColonWidth;

                canvas.drawText(formatTwoDigitNumber(mTime.second), x, mYOffset, mSecondPaint);
            }

            Rect samplesRect = new Rect();

            mSamplesPaint.getTextBounds("0", 0, 1, samplesRect);

            int payloadsCount = SensorService.pendingPayloadsCount();

            long now = System.currentTimeMillis();

            double runtime = ((double) (now - mWatchStart)) / (60 * 1000);

            String payloadsStatus = DigitalWatchFaceService.this.getString(R.string.label_payloads_single, runtime);

            if (payloadsCount != 1)
                payloadsStatus = DigitalWatchFaceService.this.getString(R.string.label_payloads, payloadsCount, runtime);

            Resources resources = DigitalWatchFaceService.this.getResources();

            float paddingSize = resources.getDimension(R.dimen.payload_padding_size);

            canvas.drawText(payloadsStatus, samplesOffset, mYOffset + paddingSize + samplesRect.height(), mSamplesPaint);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer()
        {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);

            if (shouldTimerBeRunning())
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning()
        {
            return isVisible() && !isInAmbientMode();
        }
    }
}
