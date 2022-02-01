package io.heraldprox.herald.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.heraldprox.herald.sensor.analysis.algorithms.distance.TemporalHistogramModel;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Distribution;
import io.heraldprox.herald.sensor.datatype.Histogram;

public class TemporalHistogramModelView extends View {
    private final SensorLogger logger = new ConcreteSensorLogger("GUI", "HistogramView");
    private final List<ChartElement> chartElements = new ArrayList<>();
    @Nullable
    private TemporalHistogramModel model = null;
    private int bin = 1;

    /**
     * Element of a stacked histogram chart
     */
    public final static class ChartElement {
        @NonNull
        public final Date start;
        @NonNull
        public final Date end;
        @ColorInt
        public final int color;
        @NonNull
        public final Paint paint;

        public ChartElement(@NonNull final Date start, @NonNull final Date end, @ColorInt final int color) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.paint = new Paint();
            paint.setColor(color);
        }
    }

    public TemporalHistogramModelView(Context context) {
        super(context);
    }

    public TemporalHistogramModelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TemporalHistogramModelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TemporalHistogramModelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Set data model underpinning the view.
     * @param model Data model.
     */
    public void model(@Nullable final TemporalHistogramModel model) {
        this.model = model;
    }

    /**
     * Set stacked chart configuration.
     * @param chartElements List of data series and associated colors.
     */
    public void chart(@NonNull final List<ChartElement> chartElements) {
        this.chartElements.clear();
        this.chartElements.addAll(chartElements);
    }

    /**
     * Set bin size.
     * @param size Size of histogram bin.
     */
    public void bin(final int size) {
        this.bin = size;
    }

    @Nullable
    private Rect bitmapFrame = null;
    @NonNull
    protected Rect bitmapFrame(@NonNull final Bitmap bitmap) {
        if (null == bitmapFrame || bitmapFrame.width() != bitmap.getWidth() || bitmapFrame.height() != bitmap.getHeight()) {
            bitmapFrame = new Rect(0,0, bitmap.getWidth(), bitmap.getHeight());
        }
        return bitmapFrame;
    }

    @Nullable
    private Rect canvasFrame = null;
    @NonNull
    protected Rect canvasFrame(@NonNull final Canvas canvas) {
        if (null == canvasFrame || canvasFrame.width() != canvas.getWidth() || canvasFrame.height() != canvas.getHeight()) {
            canvasFrame = new Rect(0,0, canvas.getWidth(), canvas.getHeight());
        }
        return canvasFrame;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final Bitmap bitmap = render(getHeight());
        if (null == bitmap) {
            return;
        }
        canvas.drawBitmap(bitmap, bitmapFrame(bitmap), canvasFrame(canvas), null);
    }

    @NonNull
    protected final static Histogram normalise(@NonNull final Histogram histogram, final long count) {
        final Histogram normalised = new Histogram(histogram.min, histogram.max);
        if (0 == histogram.count()) {
            return normalised;
        }
        final double scale = count / (double) histogram.count();
        for (int value=histogram.min; value<=histogram.max; value++) {
            final long valueCount = histogram.count(value);
            if (valueCount > 0) {
                final long scaledValueCount = Math.round(valueCount * scale);
                if (scaledValueCount > 0) {
                    normalised.add(value, scaledValueCount);
                }
            }
        }
        return normalised;
    }

    @NonNull
    protected final static Histogram quantise(@NonNull final Histogram histogram, final int bin) {
        final Histogram quantised = new Histogram(histogram.min / bin, histogram.max / bin);
        if (0 == histogram.count()) {
            return quantised;
        }
        for (int value=histogram.min; value<=histogram.max; value++) {
            final int quantisedValue = value / bin;
            quantised.add(quantisedValue, histogram.count(value));
        }
        return quantised;
    }

    @Nullable
    protected final static Integer gravity(@NonNull final Histogram histogram) {
        if (0 == histogram.count()) {
            return null;
        }
        final Distribution distribution = new Distribution();
        for (int value=histogram.min; value<=histogram.max; value++) {
            final long count = histogram.count(value);
            if (0 == count) {
                continue;
            }
            distribution.add(value, count);
        }
        return (int) Math.round(distribution.mean());
    }

    @Nullable
    protected Bitmap render(final int height) {
        if (null == model || chartElements.isEmpty()) {
            return null;
        }
        // Compute histogram for each chart element
        final List<Histogram> histograms = new ArrayList<>(chartElements.size());
        final Histogram combinedHistogram = quantise(new Histogram(model.minValue, model.maxValue), bin);
        for (final ChartElement chartElement : chartElements) {
            final Histogram histogram = normalise(quantise(model.histogram(chartElement.start, chartElement.end), bin), 1000);
            histograms.add(histogram);
            combinedHistogram.add(histogram);
        }
        // Calculate width based on combined histogram [min,max] inclusive
        final int width = combinedHistogram.max - combinedHistogram.min + 1;
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        // Establish max count
        if (null == combinedHistogram.mode()) {
            return bitmap;
        }
        final long countMode = combinedHistogram.count(combinedHistogram.mode());
        // Prepare for rounding up errors
        final double countMax = countMode + (countMode / height) * chartElements.size();
        // Paint chart
        for (int value=combinedHistogram.min; value<=combinedHistogram.max; value++) {
            final int x = value - combinedHistogram.min;
            for (int element=0, yStart=0; element<chartElements.size(); element++) {
                final long count = histograms.get(element).count(value);
                final int yPixels = (int) Math.ceil(height * (count / countMax));
                int yEnd = yStart + yPixels;
                if (yEnd > height) {
                    // Clipping due to cumulative rounding errors
                    yEnd = height;
                }
                canvas.drawLine(x, height-yStart, x, height-yEnd, chartElements.get(element).paint);
                yStart += yPixels;
            }
        }
        return bitmap;
    }
}
