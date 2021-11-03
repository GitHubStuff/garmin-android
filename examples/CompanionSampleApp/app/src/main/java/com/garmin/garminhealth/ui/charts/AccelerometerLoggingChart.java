package com.garmin.garminhealth.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;

import com.garmin.garminhealth.R;
import com.garmin.garminhealth.ui.sync.LoggingFragment;
import com.garmin.health.customlog.LoggingResult;
import com.garmin.health.database.dtos.RawAccelerometerLog;
import com.garmin.health.database.dtos.RawAccelerometerSample;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2017 Garmin International. All Rights Reserved.
 * <p></p>
 * This software is the confidential and proprietary information of
 * Garmin International.
 * You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement
 * you entered into with Garmin International.
 * <p></p>
 * Garmin International MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. Garmin International SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * <p></p>
 * Created by jacksoncol on 10/25/18.
 */
public class AccelerometerLoggingChart extends GHLineChart
{
    private static final String X_CHART_ENTRIES = "X_CHART_ENTRIES";
    private static final String Y_CHART_ENTRIES = "Y_CHART_ENTRIES";
    private static final String Z_CHART_ENTRIES = "Z_CHART_ENTRIES";
    private static final String ACCELEROMETER_CHART_LABELS = "ACCELEROMETER_CHART_LABELS";

    public AccelerometerLoggingChart(Context context)
    {
        super(context);
    }

    public AccelerometerLoggingChart(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public AccelerometerLoggingChart(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public void createChart(Bundle savedInstanceState, long appStartTime)
    {
        long startTime = appStartTime;
        
        if(savedInstanceState != null)
        {
            List<Entry> xEntries;
            List<Entry> yEntries;
            List<Entry> zEntries;

            List<String> labels;

            if(startTime < 0)
            {
                LoggingResult result = savedInstanceState.getParcelable(LoggingFragment.LOGGING_RESULT);

                if(result != null && !result.getRawAccelerometerList().isEmpty())
                {
                    startTime = result.getRawAccelerometerList().get(0).getTimestampMs();

                    LineDataSet xDataSet = createDataSet(null, getContext().getString(R.string.x_accel_dataset), Color.GREEN);
                    LineDataSet yDataSet = createDataSet(null, getContext().getString(R.string.y_accel_dataset), ColorTemplate.getHoloBlue());
                    LineDataSet zDataSet = createDataSet(null, getContext().getString(R.string.z_accel_dataset), Color.CYAN);

                    List<LineDataSet> lineDataSets = new ArrayList<>();

                    lineDataSets.add(xDataSet);
                    lineDataSets.add(yDataSet);
                    lineDataSets.add(zDataSet);

                    createGHChart(null, lineDataSets, startTime);

                    for(RawAccelerometerLog log : result.getRawAccelerometerList())
                    {
                        updateChart(new ChartData(log), log.getTimestampMs());
                    }
                }
                else
                {
                    setVisibility(GONE);
                }
            }
            else
            {
                xEntries = savedInstanceState.getParcelableArrayList(X_CHART_ENTRIES);
                yEntries = savedInstanceState.getParcelableArrayList(Y_CHART_ENTRIES);
                zEntries = savedInstanceState.getParcelableArrayList(Z_CHART_ENTRIES);
                labels = savedInstanceState.getStringArrayList(ACCELEROMETER_CHART_LABELS);

                LineDataSet xDataSet = createDataSet(xEntries, getContext().getString(R.string.x_accel_dataset), Color.GREEN);
                LineDataSet yDataSet = createDataSet(yEntries, getContext().getString(R.string.y_accel_dataset), ColorTemplate.getHoloBlue());
                LineDataSet zDataSet = createDataSet(zEntries, getContext().getString(R.string.z_accel_dataset), Color.CYAN);

                List<LineDataSet> lineDataSets = new ArrayList<>();

                lineDataSets.add(xDataSet);
                lineDataSets.add(yDataSet);
                lineDataSets.add(zDataSet);

                createGHChart(labels, lineDataSets, startTime);

                setMaxVisibleValueCount(100);
            }
        }
    }

    @Override
    public void updateChart(ChartData result, long updateTime)
    {
        if(result != null && result.getLog() != null && result.getLog() instanceof RawAccelerometerLog)
        {
            RawAccelerometerLog log = (RawAccelerometerLog) result.getLog();

            for(RawAccelerometerSample sample : log.getRawAccelerometerSampleList())
            {
                updateDataSet(new ChartData(sample.getX() == null ? -1 : sample.getX().intValue(), sample.getTimestampMs()), getContext().getString(R.string.x_accel_dataset));
                updateDataSet(new ChartData(sample.getY() == null ? -1 : sample.getY().intValue(), sample.getTimestampMs()), getContext().getString(R.string.y_accel_dataset));
                updateDataSet(new ChartData(sample.getZ() == null ? -1 : sample.getZ().intValue(), sample.getTimestampMs()), getContext().getString(R.string.z_accel_dataset));
                updateGHChart(new ChartData(sample.getX() == null ? -1 : sample.getX().intValue(), sample.getTimestampMs()));
            }
        }
    }

    @Override
    public void saveChartState(Bundle outState)
    {
        outState.putParcelableArrayList(X_CHART_ENTRIES, getChartEntries(getContext().getString(R.string.x_accel_dataset)));
        outState.putParcelableArrayList(Y_CHART_ENTRIES, getChartEntries(getContext().getString(R.string.y_accel_dataset)));
        outState.putParcelableArrayList(Z_CHART_ENTRIES, getChartEntries(getContext().getString(R.string.z_accel_dataset)));
        outState.putStringArrayList(ACCELEROMETER_CHART_LABELS, getLabels());
    }
}
