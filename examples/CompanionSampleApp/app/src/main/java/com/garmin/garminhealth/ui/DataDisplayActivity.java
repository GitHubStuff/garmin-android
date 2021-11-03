package com.garmin.garminhealth.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.garmin.device.realtime.RealTimeDataType;
import com.garmin.device.realtime.RealTimeResult;
import com.garmin.device.realtime.listeners.RealTimeDataListener;
import com.garmin.garminhealth.R;
import com.garmin.garminhealth.devices.PairedDevicesDialogFragment;
import com.garmin.garminhealth.ui.charts.AccelerometerChart;
import com.garmin.garminhealth.ui.charts.BodyBatteryChart;
import com.garmin.garminhealth.ui.charts.CaloriesChart;
import com.garmin.garminhealth.ui.charts.FloorsChart;
import com.garmin.garminhealth.ui.charts.GHLineChart;
import com.garmin.garminhealth.ui.charts.HeartRateChart;
import com.garmin.garminhealth.ui.charts.HeartRateVariabilityChart;
import com.garmin.garminhealth.ui.charts.IntensityMinutesChart;
import com.garmin.garminhealth.ui.charts.RealTimeChartData;
import com.garmin.garminhealth.ui.charts.RespirationChart;
import com.garmin.garminhealth.ui.charts.Spo2Chart;
import com.garmin.garminhealth.ui.charts.StepsChart;
import com.garmin.garminhealth.ui.charts.StressLevelChart;
import com.garmin.garminhealth.ui.realtime.RealTimeDataHandler;
import com.garmin.health.Device;
import com.garmin.health.DeviceManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DataDisplayActivity extends BaseGarminHealthActivity implements RealTimeDataListener {
    public static final String APP_START_TIME = "appStartTime";
    private static final int CHART_HEIGHT = 375;

    private static final String TAG = DataDisplayActivity.class.getSimpleName();

    private String mAddress;
    private int originalChartHeight = CHART_HEIGHT;

    private HeartRateChart hrChart;
    private HeartRateVariabilityChart hrvChart;
    private StepsChart stepsChart;
    private CaloriesChart caloriesChart;
    private FloorsChart floorsChart;
    private IntensityMinutesChart intensityMinutesChart;
    private StressLevelChart stressChart;
    private AccelerometerChart accelerometerChart;
    private Spo2Chart spo2Chart;
    private BodyBatteryChart bodyBatteryChart;
    private RespirationChart respirationChart;

    private long startTime;
    private List<GHLineChart> charts;
    private Set<RealTimeDataType> mSupportedTypes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time);
        setTitle(R.string.title_real_time);
        setChartToolbar();

        Intent intent = getIntent();
        mAddress = intent.getStringExtra(PairedDevicesDialogFragment.DEVICE_ADDRESS_EXTRA);
        DeviceManager deviceManager = DeviceManager.getDeviceManager();
        Device device = deviceManager.getDevice(mAddress);
        mSupportedTypes = device.supportedRealTimeTypes();

        // Initialize charts
        Point size = findDisplaySize();

        hrChart = findViewById(R.id.hr_chart);
        hrvChart = findViewById(R.id.hrv_chart);
        stepsChart = findViewById(R.id.steps_chart);
        caloriesChart = findViewById(R.id.calories_chart);
        floorsChart = findViewById(R.id.floor_chart);
        intensityMinutesChart = findViewById(R.id.intensity_minutes_chart);
        stressChart = findViewById(R.id.stress_chart);
        accelerometerChart = findViewById(R.id.accelerometer_chart);
        spo2Chart = findViewById(R.id.spo2_chart);
        bodyBatteryChart = findViewById(R.id.body_battery_chart);
        respirationChart = findViewById(R.id.respiration_chart);

        initRealTimeData();

        if(savedInstanceState != null)
        {
            startTime = savedInstanceState.getLong(APP_START_TIME);
        }

        startTime = (startTime == 0) ? System.currentTimeMillis() : startTime;
        charts = Arrays.asList(hrChart, hrvChart, stressChart, stepsChart, caloriesChart, intensityMinutesChart, floorsChart, accelerometerChart, spo2Chart, bodyBatteryChart, respirationChart);

        // create GH charts
        for(GHLineChart chart : charts)
        {
            if(chart != null )
            {
                chart.createChart(savedInstanceState, startTime);
                // resizeChart(chart, size.x, CHART_HEIGHT);
                // apply the gesture listener
                chart.setOnChartGestureListener(new HealthChartGestureListener(chart));
            }
        }

        // show the message
        showSnackbarMessage(R.string.all_charts_refreshed);
    }

    /**
     * Sets char toolbar
     */
    private void setChartToolbar() {
        // Toolbar
        Toolbar chartToolbar = findViewById(R.id.chart_toolbar);
        chartToolbar.setTitle(getString(R.string.garmin_health_charts));
        setSupportActionBar(chartToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflates the chart menu
        getMenuInflater().inflate(R.menu.menu_charts, menu);
        for(RealTimeDataType queryType : RealTimeDataType.values())
        {
            if(mSupportedTypes == null || !mSupportedTypes.contains(queryType))
            {
                switch(queryType)
                {
                    case STEPS:
                        menu.findItem(R.id.show_steps_chart).setEnabled(false);
                        break;
                    case HEART_RATE_VARIABILITY:
                        menu.findItem(R.id.show_hrv_chart).setEnabled(false);
                        break;
                    case CALORIES:
                        menu.findItem(R.id.show_calories_chart).setEnabled(false);
                        break;
                    case ASCENT:
                        menu.findItem(R.id.show_floors_chart).setEnabled(false);
                        break;
                    case INTENSITY_MINUTES:
                        menu.findItem(R.id.show_intensity_minutes_chart).setEnabled(false);
                        break;
                    case HEART_RATE:
                        menu.findItem(R.id.show_hr_chart).setEnabled(false);
                        break;
                    case STRESS:
                        menu.findItem(R.id.show_stress_chart).setEnabled(false);
                        break;
                    case ACCELEROMETER:
                        menu.findItem(R.id.show_accelerometer_chart).setEnabled(false);
                        break;
                    case SPO2:
                        menu.findItem(R.id.show_spo2_chart).setEnabled(false);
                        break;
                    case RESPIRATION:
                        menu.findItem(R.id.show_respiration_chart).setEnabled(false);
                        break;
                    case BODY_BATTERY:
                        menu.findItem(R.id.show_body_battery_chart).setEnabled(false);
                        break;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.chart_refresh:
                refreshCharts();
                return true;

            case R.id.show_hr_chart:
                toggleChartCard(item, R.id.hr_chart_card);
                return true;

            case R.id.show_hrv_chart:
                toggleChartCard(item, R.id.hrv_chart_card);
                return true;

            case R.id.show_stress_chart:
                toggleChartCard(item, R.id.stress_chart_card);
                return true;

            case R.id.show_steps_chart:
                toggleChartCard(item, R.id.steps_chart_card);
                return true;

            case R.id.show_calories_chart:
                toggleChartCard(item, R.id.calories_chart_card);
                return true;

            case R.id.show_intensity_minutes_chart:
                toggleChartCard(item, R.id.intensity_minutes_chart_card);
                return true;

            case R.id.show_floors_chart:
                toggleChartCard(item, R.id.floor_chart_card);
                return true;

            case R.id.accelerometer_chart:
                toggleChartCard(item, R.id.accelerometer_chart_card);
                return true;

            case R.id.spo2_chart:
                toggleChartCard(item, R.id.spo2_chart_card);
                return true;

            case R.id.body_battery_chart:
                toggleChartCard(item, R.id.body_battery_chart_card);
                return true;

            case R.id.respiration_chart:
                toggleChartCard(item, R.id.respiration_chart_card);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Toggles chart card based on the menu selection
     * @param item
     * @param chartCardId
     */
    private void toggleChartCard(MenuItem item, int chartCardId ){
        boolean checked = item.isChecked();
        item.setChecked(checked ? false : true);
        findViewById(chartCardId).setVisibility(checked ? View.GONE : View.VISIBLE);
    }

    private void disableChartCard(int chartCardId){
        findViewById(chartCardId).setVisibility(View.GONE);
    }

    private void initRealTimeData()
    {
        //Data may have been received when page wasn't in the foreground
        HashMap<RealTimeDataType, RealTimeResult> latestData = RealTimeDataHandler.getInstance().getLatestData(mAddress);
        if(latestData != null)
        {
            for (RealTimeDataType type : latestData.keySet())
            {
                updateData(type, latestData.get(type));
            }
        }

        for(RealTimeDataType queryType : RealTimeDataType.values())
        {
            if(mSupportedTypes == null || !mSupportedTypes.contains(queryType))
            {
                switch(queryType)
                {
                    case STEPS:
                        disableChartCard(R.id.steps_chart_card);
                        break;
                    case HEART_RATE_VARIABILITY:
                        disableChartCard(R.id.hrv_chart_card);
                        break;
                    case CALORIES:
                        disableChartCard(R.id.calories_chart_card);
                        break;
                    case ASCENT:
                        disableChartCard(R.id.floor_chart_card);
                        break;
                    case INTENSITY_MINUTES:
                        disableChartCard(R.id.intensity_minutes_chart_card);
                        break;
                    case HEART_RATE:
                        disableChartCard(R.id.hr_chart_card);
                        break;
                    case STRESS:
                        disableChartCard(R.id.stress_chart_card);
                        break;
                    case ACCELEROMETER:
                        disableChartCard(R.id.accelerometer_chart_card);
                        break;
                    case SPO2:
                        disableChartCard(R.id.spo2_chart_card);
                        break;
                    case RESPIRATION:
                        disableChartCard(R.id.respiration_chart_card);
                        break;
                    case BODY_BATTERY:
                        disableChartCard(R.id.body_battery_chart_card);
                        break;
                }
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        DeviceManager.getDeviceManager().enableRealTimeData(mAddress, mSupportedTypes);
        DeviceManager.getDeviceManager().addRealTimeDataListener(this, mSupportedTypes);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        //Activity isn't running, don't need to listen for data
        DeviceManager.getDeviceManager().removeRealTimeDataListener(this, mSupportedTypes);
        DeviceManager.getDeviceManager().disableRealTimeData(mAddress, mSupportedTypes);
    }

    @Override
    @MainThread
    public void onDataUpdate(@NonNull String macAddress, @NonNull final RealTimeDataType dataType, @NonNull final RealTimeResult result)
    {
        if(!macAddress.equals(mAddress))
        {
            //Real time data came from different device
            return;
        }

        //Use same logging as single instance real time listener for sample
        RealTimeDataHandler.logRealTimeData(TAG, macAddress, dataType, result);

        updateData(dataType, result);
    }

    private void updateData(final RealTimeDataType dataType, final RealTimeResult result)
    {
        if (dataType == null || result == null)
        {
            return;
        }

        //Update views with new data
        switch (dataType)
        {
            case HEART_RATE:
                hrChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case HEART_RATE_VARIABILITY:
                hrvChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case STRESS:
                stressChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case STEPS:
                stepsChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case CALORIES:
                caloriesChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case ASCENT:
                floorsChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case INTENSITY_MINUTES:
                intensityMinutesChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case ACCELEROMETER:
                accelerometerChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case SPO2:
                spo2Chart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case BODY_BATTERY:
                bodyBatteryChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            case RESPIRATION:
                respirationChart.updateChart(new RealTimeChartData(result), System.currentTimeMillis());
                break;
            default:
                break;
        }
    }

    //Permission needed to save real time data to external file
    private boolean requestStoragePermission()
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
        {
            return true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // saving chart state to bundle. This will help us to recreate the chart if app is killed or need to be recreated
        super.onSaveInstanceState(outState);
        for (GHLineChart chart : charts) {
            if (chart != null) {
                chart.saveChartState(outState);
            }
        }
        outState.putLong(APP_START_TIME, startTime);
    }

    /**
     * On clicking refresh, all charts will restart.
     */
    public void refreshCharts(){
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    /**
     * Shows the provided message as a snackbar text
     * @param resId
     */
    private  void showSnackbarMessage(@StringRes int resId){
        Snackbar refreshMessage = Snackbar.make(findViewById(R.id.hr_chart), resId, Snackbar.LENGTH_LONG);
        refreshMessage.show();
    }

    /**
     * Resizes the chart
     * @param chart
     * @param width
     * @param height
     */
    private void resizeChart(LineChart chart, int width, int height)
    {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) chart.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        chart.setLayoutParams(layoutParams);
    }

    /**
     * finds the display size of the device
     * @return
     */
    private Point findDisplaySize(){

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return size;
    }
    /**
     * Listens to the chart gestures
     */
    public class HealthChartGestureListener implements OnChartGestureListener
    {
        private LineChart chart;

        public HealthChartGestureListener(LineChart chart1)
        {
            chart = chart1;
        }

        @Override
        public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}

        @Override
        public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}

        @Override
        public void onChartLongPressed(MotionEvent me) {}

        @Override
        public void onChartDoubleTapped(MotionEvent me)
        {
            // finds the display size
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            //Check the chart's size and then resize
            int maxHeight = 3*size.y/4;

            if(chart.getHeight() == maxHeight)
            {
                //already full screen, so reduce the size
                DataDisplayActivity.this.resizeChart(chart, chart.getWidth(), originalChartHeight);
            }
            else
            {
                // make it full screen and allow pinch zoom
                originalChartHeight = chart.getHeight();
                DataDisplayActivity.this.resizeChart(chart, chart.getWidth(), maxHeight);
            }
        }

        @Override
        public void onChartSingleTapped(MotionEvent me) {}

        @Override
        public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}

        @Override
        public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}

        @Override
        public void onChartTranslate(MotionEvent me, float dX, float dY) {}
    }
}
