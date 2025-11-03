package com.koigzzzz.cex.utils;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.koigzzzz.cex.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class PriceMarker extends MarkerView {
    private TextView tvPrice;
    private NumberFormat priceFormat;

    public PriceMarker(Context context, int layoutResource) {
        super(context, layoutResource);
        tvPrice = findViewById(R.id.tvPrice);
        priceFormat = new DecimalFormat("#,##0.00");
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        float price = e.getY();
        tvPrice.setText("$" + priceFormat.format(price));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Center the marker horizontally and position it above the point
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}

