package io.heraldprox.herald.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Histogram;

public class HistogramView extends View {
    private final SensorLogger logger = new ConcreteSensorLogger("GUI", "HistogramView");
    private Histogram histogram = null;

    public HistogramView(Context context) {
        super(context);
    }

    public HistogramView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HistogramView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HistogramView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setHistogram(final Histogram histogram) {
        this.histogram = histogram;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (histogram == null) {
            return;
        }
        final Bitmap bitmap = render(histogram, 100);
        final Rect bitmapFrame = new Rect(0,0, bitmap.getWidth(), bitmap.getHeight());
        final Rect canvasFrame = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmap, bitmapFrame, canvasFrame, null);
    }

    /**
     * Render histogram as bitmap with width equal to number of bins and specified height.
     * The bitmap is then stretched within onDraw().
     * @param histogram
     * @param height
     * @return
     */
    protected final static Bitmap render(final Histogram histogram, final int height) {
        final int width = histogram.max - histogram.min + 1;
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (null == histogram.mode()) {
            return bitmap;
        }
        final int mode = histogram.mode();
        final long countMax = histogram.count(mode);
        if (0 == countMax) {
            return bitmap;
        }
        final Canvas canvas = new Canvas(bitmap);
        final Paint paintGray = new Paint();
        paintGray.setColor(Color.GRAY);
        final Paint paintRed = new Paint();
        paintRed.setColor(Color.RED);
        final double countMaxLog = Math.log10(countMax);
        for (int value=histogram.min; value<=histogram.max; value++) {
            final long count = histogram.count(value);
            if (0 == count) {
                continue;
            }
            final int magnitude = (int) Math.ceil(height * (Math.log10(count) / countMaxLog));
            final int x = value - histogram.min;
            canvas.drawLine(x, height, x, height-magnitude, (value == mode ? paintRed : paintGray));
        }
        return bitmap;
    }
}
