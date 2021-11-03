package com.garmin.garminhealth.ui.charts;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;

import com.garmin.garminhealth.R;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Represents body battery charts
 * Created by morajkar on 8/2/2017.
 */

public class BodyBatteryChart extends GHLineChart
{
    public static final String BODY_BATTERY_CHART_ENTRIES = "bodyBatteryChartEntries";
    public static final String BODY_BATTERY_CHART_LABELS = "bodyBatteryChartLabels";

    private int maxXVisibleCount = X_MAX_LANDSCAPE;
    private static final int X_MAX_LANDSCAPE = 400;
    private static final int X_MAX_PORTRAIT = 200;

    public BodyBatteryChart(Context context)
    {
        super(context);
    }

    public BodyBatteryChart(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public BodyBatteryChart(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public void createChart(Bundle savedInstanceState, long appStartTime)
    {
        List<Entry> entries = null;
        List<String> labels = null;

        if (savedInstanceState != null)
        {
            entries = savedInstanceState.getParcelableArrayList(BODY_BATTERY_CHART_ENTRIES);
            labels = savedInstanceState.getStringArrayList(BODY_BATTERY_CHART_LABELS);
        }

        LineDataSet dataSet = createDataSet(entries, getContext().getString(R.string.bodyBattery_dataSet), ColorTemplate.getHoloBlue());
        createGHChart(labels, Collections.singletonList(dataSet), appStartTime);
    }

    @Override
    public void updateChart(ChartData data, long updateTime)
    {
        if(data != null && data instanceof RealTimeChartData && ((RealTimeChartData)data).getBodyBattery() != null)
        {
            ChartData result = new ChartData(((RealTimeChartData)data).getBodyBattery().getBodyBatteryLevel(), updateTime - PROTECTED_StartTime);
            updateDataSet(result, getContext().getString(R.string.bodyBattery_dataSet));
            updateGHChart(result);
        }
    }

    @Override
    public void saveChartState(Bundle outState)
    {
        outState.putParcelableArrayList(BODY_BATTERY_CHART_ENTRIES, getChartEntries(getContext().getString(R.string.bodyBattery_dataSet)));
        outState.putStringArrayList(BODY_BATTERY_CHART_LABELS, getLabels());
    }

    protected int getXAxisLandscapeMax()
    {
        return X_MAX_LANDSCAPE;
    }

    protected  int getXAxisPortraitMax()
    {
        return X_MAX_PORTRAIT;
    }

    protected int getChartXVisibleRange()
    {
        return maxXVisibleCount;
    }
}
