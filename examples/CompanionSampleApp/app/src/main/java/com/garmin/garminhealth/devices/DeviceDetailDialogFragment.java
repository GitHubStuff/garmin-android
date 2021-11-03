package com.garmin.garminhealth.devices;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.garmin.garminhealth.R;
import com.garmin.garminhealth.ui.settings.widget.CustomAutoValueListView;
import com.garmin.garminhealth.ui.sync.LoggingFragment;
import com.garmin.health.ConnectionState;
import com.garmin.health.Device;
import com.garmin.health.DeviceManager;
import com.garmin.health.NotificationManager;
import com.garmin.health.customlog.DataSource;
import com.garmin.health.customlog.LoggingStatus.State;
import com.garmin.health.customlog.LoggingSyncListener;
import com.garmin.health.settings.ConnectIqItem;
import com.garmin.health.settings.SupportStatus;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
 * Created by jacksoncol on 6/19/18.
 */
public class DeviceDetailDialogFragment extends DialogFragment implements LoggingSyncListener
{
    public final static String DEVICE_ADDRESS_EXTRA = "device.address";

    private TextView mStartText = null;
    private TextView mEndText = null;
    private ImageView mStartButton = null;
    private ImageView mEndButton = null;
    private Button mSettingsButton = null;

    private Button mQueryButton = null;
    private Button mDeleteButton = null;

    private long mEndTime = Calendar.getInstance().getTimeInMillis()/1000;
    private long mStartTime = mEndTime - (3600 * 6);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        setRetainInstance(true);

        DeviceManager deviceManager = DeviceManager.getDeviceManager();
        Device device = deviceManager.getDevice(getArguments().getString(DEVICE_ADDRESS_EXTRA));
        deviceManager.addLoggingSyncListener(this);

        if(device.connectionState() != ConnectionState.CONNECTED)
        {
            return null;
        }

        View view = inflater.inflate(R.layout.fragment_device_details, container, false);

        if(device.notificationSupportStatus() != SupportStatus.ENABLED)
        {
            hideNotifications(view);
        }
        else
        {
            setNotification(view, device);
        }

        if(device.connectIqLaunchSupportStatus() != SupportStatus.ENABLED)
        {
            hideConnectIq(view);
        }
        else
        {
            setConnectIq(view, device);
        }

        if(device.batteryPercentageSupportStatus() == SupportStatus.ENABLED)
        {
            setBatteryLife(view, device);
        }

        mStartText = view.findViewById(R.id.start_text);
        mEndText = view.findViewById(R.id.end_text);
        mStartButton = view.findViewById(R.id.start_button);
        mEndButton = view.findViewById(R.id.end_button);
        mQueryButton = view.findViewById(R.id.query_button);
        mDeleteButton = view.findViewById(R.id.delete_button);
        mSettingsButton = view.findViewById(R.id.settings_button);

        if(device.dataLoggingSupportStatus() != SupportStatus.ENABLED)
        {
            hideDataLogging(view);
        }
        else
        {
            setDataLogging(view, device);
            setLoggingSyncing(device.isDownloadingLoggedData(), view.findViewById(R.id.sync_button), -1);
        }

        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        DeviceManager deviceManager = DeviceManager.getDeviceManager();
        deviceManager.addLoggingSyncListener(this);
    }

    @Override
    public void onStop()
    {
        super.onStop();

        DeviceManager deviceManager = DeviceManager.getDeviceManager();
        deviceManager.removeLoggingSyncListener(this);
    }

    private void hideDataLogging(View view)
    {
        mStartText.setVisibility(View.GONE);
        mEndText.setVisibility(View.GONE);
        mStartButton.setVisibility(View.GONE);
        mEndButton.setVisibility(View.GONE);

        TextView loggingTitle = view.findViewById(R.id.interval_picker_top_anchor);
        TextView startTitle = view.findViewById(R.id.start_title);
        TextView endTitle = view.findViewById(R.id.end_title);
        Button queryButton = view.findViewById(R.id.query_button);
        Button syncButton = view.findViewById(R.id.sync_button);
        Button deleteButton = view.findViewById(R.id.delete_button);

        loggingTitle.setVisibility(View.GONE);
        startTitle.setVisibility(View.GONE);
        endTitle.setVisibility(View.GONE);
        mSettingsButton.setVisibility(View.GONE);
        queryButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        syncButton.setVisibility(View.GONE);
    }

    private void setDataLogging(View view, Device device)
    {
        mStartText = view.findViewById(R.id.start_text);
        mEndText = view.findViewById(R.id.end_text);
        mStartButton = view.findViewById(R.id.start_button);
        mEndButton = view.findViewById(R.id.end_button);

        checkAvailableData(device.address());

        mStartText.setEnabled(false);
        mStartText.setText(formatDateTime(mStartTime));

        mEndText.setEnabled(false);
        mEndText.setText(formatDateTime(mEndTime));

        mStartButton.setOnClickListener(v -> new DateTimePicker(timestamp ->
        {
            if(timestamp < mEndTime)
            {
                mStartText.setText(formatDateTime(timestamp));
                mStartTime = timestamp;
            }
            else
            {
                Toast.makeText(getContext(), "Invalid time.", Toast.LENGTH_SHORT).show();
            }

        }, getActivity()));

        mEndButton.setOnClickListener(v -> new DateTimePicker(timestamp ->
        {
            if(timestamp > mStartTime)
            {
                mEndText.setText(formatDateTime(timestamp));
                mEndTime = timestamp;
            }
            else
            {
                Toast.makeText(getContext(), "Invalid time.", Toast.LENGTH_SHORT).show();
            }
        }, getActivity()));

        Button settingsButton = view.findViewById(R.id.settings_button);
        Button queryButton = view.findViewById(R.id.query_button);
        Button syncButton = view.findViewById(R.id.sync_button);
        Button deleteButton = view.findViewById(R.id.delete_button);

        final AtomicBoolean shouldContinue = new AtomicBoolean(false);

        syncButton.setOnClickListener((v) ->
        {
            boolean isLogging = device.isDownloadingLoggedData();

            if(!isLogging)
            {
                setLoggingSyncing(true, syncButton, 0);
                shouldContinue.set(true);

                device.downloadLoggedData(new LoggingSyncListener()
                {
                    @Override
                    public void onSyncProgress(Device device, int progress)
                    {
                        if(getActivity() != null)
                        {
                            getActivity().runOnUiThread(() ->
                            {
                                setLoggingSyncing(true, syncButton, progress);
                            });
                        }
                    }

                    @Override
                    public void onSyncStarted(Device device)
                    {
                        if (getActivity() != null)
                        {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Logging Sync Started", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onSyncComplete(Device device)
                    {
                        if(getActivity() != null)
                        {
                            getActivity().runOnUiThread(() ->
                            {
                                Toast.makeText(getContext(), "Logging Sync Complete", Toast.LENGTH_SHORT).show();
                                checkAvailableData(device.address());
                                setLoggingSyncing(false, syncButton, -1);
                            });
                        }
                    }

                    @Override
                    public void onSyncFailed(Device device, Exception e)
                    {
                        if (getActivity() != null)
                        {
                            getActivity().runOnUiThread(() ->
                            {
                                Toast.makeText(getContext(), String.format("Logging Sync Failed... [%s]",
                                        e == null ? null : e.getMessage()), Toast.LENGTH_SHORT).show();
                                checkAvailableData(device.address());
                                setLoggingSyncing(false, syncButton, -1);
                            });
                        }
                    }
                });
            }
            else
            {
                shouldContinue.set(!shouldContinue.get());
            }

        });

        settingsButton.setOnClickListener((v) ->
        {
            settingsButton.setEnabled(false);

            new LoggingGetSettingsTask().execute(device);
        });

        queryButton.setOnClickListener((v) ->
        {
            if(mEndTime - mStartTime <= 0 || mEndTime - mStartTime > 86400)
            {
                Toast.makeText(getContext(), "Maximum Interval is 24 Hours", Toast.LENGTH_SHORT).show();
            }

            DeviceManager.getDeviceManager().hasLoggedData(device.address(), mStartTime, mEndTime, (hasData) ->
            {
                if(hasData == null)
                {
                    if(Looper.myLooper() == null)
                    {
                        Looper.prepare();
                    }

                    Toast.makeText(getContext(), "An error occured during communication.", Toast.LENGTH_SHORT).show();
                }
                else if(hasData)
                {
                    DeviceManager.getDeviceManager().getLoggedDataForDevice(device.address(), mStartTime, mEndTime, (loggingResult) ->
                            getFragmentManager().beginTransaction()
                                    .replace(R.id.main_root, LoggingFragment.
                                            getInstance(device.address(), loggingResult))
                                    .addToBackStack(null)
                                    .commit());
                }
                else
                {
                    if(Looper.myLooper() == null)
                    {
                        Looper.prepare();
                    }

                    Toast.makeText(getContext(), "No data available on this interval.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        deleteButton.setOnClickListener((v) ->
        {
            boolean success = DeviceManager.getDeviceManager().deleteData(device.address(), mStartTime, mEndTime);
            Toast.makeText(getContext(), String
                    .format("Data deletion for interval was %ssuccessful.", success ? "" : "un"), Toast.LENGTH_SHORT).show();

        });
    }

    private void hideConnectIq(View view)
    {
        TextView label = view.findViewById(R.id.connect_iq_app_title);
        RecyclerView list = view.findViewById(R.id.recyclerView);

        label.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
    }

    private void hideNotifications(View view)
    {
        TextView title = view.findViewById(R.id.notification_title);
        TextView titleLabel = view.findViewById(R.id.title);
        TextView messageLabel = view.findViewById(R.id.message);
        EditText titleContent = view.findViewById(R.id.title_content);
        EditText contentContent = view.findViewById(R.id.message_content);
        Button clearButton = view.findViewById(R.id.clear_button);
        Button sendButton = view.findViewById(R.id.send_button);

        title.setVisibility(View.GONE);
        titleLabel.setVisibility(View.GONE);
        messageLabel.setVisibility(View.GONE);
        titleContent.setVisibility(View.GONE);
        contentContent.setVisibility(View.GONE);
        clearButton.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);
    }

    void setLoggingSyncing(boolean syncing, Button syncingButton, int progress)
    {
        if(getActivity() != null)
        {
            getActivity().runOnUiThread(() ->
            {
                syncingButton.setText(syncing ? (progress < 0 ? "..." : String.format("%d%%", progress)) : "SYNC");
                //syncingButton.setClickable(!syncing);
            });
        }
    }

    private void setBatteryLife(View view, Device device)
    {
        Executors.newSingleThreadExecutor().submit(() ->
        {
            int percentage;

            try
            {
                percentage = Futures.getChecked(JdkFutureAdapters.listenInPoolThread(device.batteryPercentage()), Exception.class);
            }
            catch(Exception e)
            {
                return;
            }

            if(getActivity() == null)
            {
                return;
            }

            getActivity().runOnUiThread(() ->
            {
                int id;
                String label = String.format("%d%%", percentage);

                if(percentage > 90)
                {
                    id = R.drawable.ic_battery_full_black_24dp;
                }
                else if(percentage > 50)
                {
                    id = R.drawable.ic_battery_60_black_24dp;
                }
                else
                {
                    id = R.drawable.ic_battery_20_black_24dp;
                }

                TextView labelView = view.findViewById(R.id.battery_percentage);
                ImageView imageView = view.findViewById(R.id.battery_image);

                labelView.setText(label);
                imageView.setImageResource(id);
            });
        });
    }

    public void setNotification(View view, Device device)
    {
        Button clearButton = view.findViewById(R.id.clear_button);
        Button sendButton = view.findViewById(R.id.send_button);

        EditText titleContent = view.findViewById(R.id.title_content);
        EditText contentContent = view.findViewById(R.id.message_content);

        NotificationManager manager = NotificationManager.getNotificationManager();

        clearButton.setOnClickListener((v1) ->
        {
            titleContent.setText("");
            contentContent.setText("");

            manager.clearNotifications(device.address());
        });

        sendButton.setOnClickListener((v2) ->
        {
            manager.createNotification(device, titleContent.getText().toString(), contentContent.getText().toString(), "Clear", R.drawable.health_logo);

            titleContent.setText("");
            contentContent.setText("");
        });
    }

    public void setConnectIq(View view, Device device)
    {
        Executors.newSingleThreadExecutor().submit(() ->
        {
            List<ConnectIqItem> items;

            try
            {
                items = Futures.getChecked(JdkFutureAdapters.listenInPoolThread(device.getLaunchableConnectIqApps()), Exception.class);
            }
            catch(Exception e)
            {
                return;
            }

            if(getActivity() == null)
            {
                return;
            }

            getActivity().runOnUiThread(() ->
            {
                RecyclerView list = view.findViewById(R.id.recyclerView);
                list.setAdapter(new ConnectIqAdapter(items, device));
                list.setLayoutManager(new LinearLayoutManager(getContext()));
            });
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if(actionBar != null)
        {
            actionBar.setTitle(R.string.device_detail_title);
        }
    }

    private String formatDateTime(long startTime)
    {
        return DateFormat.format("MM/dd/yyyy h:mm:ss a", startTime * 1000).toString();
    }

    private void checkAvailableData(String address)
    {
        if(getActivity() == null)
        {
            return;
        }

        getActivity().runOnUiThread(() ->
                DeviceManager.getDeviceManager().hasLoggedData(address, b ->
                        {
                            Activity activity = getActivity();

                            if(activity != null && b != null)
                            {
                                mQueryButton.setEnabled(b);
                            }
                        }));
    }

    private static class DateTimePicker implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener
    {
        private TimestampSetListener mTimestampSetListener;
        private Context mContext;

        private int year;
        private int month;
        private int day;
        private int hour;
        private int minute;
        private int second;

        public DateTimePicker(TimestampSetListener listener, Context context)
        {
            mTimestampSetListener = listener;
            mContext = context;

            Calendar now = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(
                    context,
                    this,
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );

            dpd.show();
        }

        @Override
        public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth)
        {
            this.year = year;
            this.month = monthOfYear + 1;
            this.day = dayOfMonth;

            Calendar now = Calendar.getInstance();
            TimePickerDialog tpd = new TimePickerDialog(
                    mContext,
                    this,
                    now.get(Calendar.HOUR),
                    now.get(Calendar.MINUTE),
                    true);

            tpd.show();
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute)
        {
            this.hour = hourOfDay;
            this.minute = minute;

            DateTime dateTime = new DateTime(this.year, this.month, this.day, this.hour, this.minute, 0, DateTimeZone.forTimeZone(TimeZone.getDefault()));

            mTimestampSetListener.onTimestampSet(dateTime.getMillis() / 1000);
        }
    }

    private interface TimestampSetListener
    {
        void onTimestampSet(long timestamp);
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
    }

    private static class ConnectIqViewHolder extends RecyclerView.ViewHolder
    {
        TextView connectIqName;
        ConnectIqItem connectIqItem;

        public ConnectIqViewHolder(View itemView)
        {
            super(itemView);

            connectIqName = itemView.findViewById(R.id.app_name);
        }
    }

    private static class ConnectIqAdapter extends RecyclerView.Adapter<ConnectIqViewHolder>
    {
        List<ConnectIqItem> mItems;
        Device mDevice;

        public ConnectIqAdapter(List<ConnectIqItem> items, Device device)
        {
            mItems = items;
            mDevice = device;
        }

        @Override
        public ConnectIqViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.connect_iq_item, parent, false);

            ConnectIqViewHolder holder = new ConnectIqViewHolder(itemView);

            itemView.setOnClickListener((v1) ->
                    mDevice.launchConnectIqApp(holder.connectIqItem));

            return holder;
        }

        @Override
        public void onBindViewHolder(ConnectIqViewHolder holder, int position)
        {
            if(holder == null)
            {
                return;
            }

            holder.connectIqName.setText(mItems.get(position).getName());
            holder.connectIqItem = mItems.get(position);
        }

        @Override
        public int getItemCount()
        {
            return mItems.size();
        }
    }

    private class LoggingGetSettingsTask extends AsyncTask<Device, Integer, Map<DataSource, Float>>
    {
        Device mDevice;

        protected Map<DataSource, Float> doInBackground(Device... devices)
        {
            Device device = devices[0];
            mDevice = device;

            Map<DataSource, Float> ret = new HashMap<>();

            Set<DataSource> sources = device.supportedLoggingTypes();

            if(sources == null)
            {
                return ret;
            }

            for(DataSource source : sources)
            {
                try
                {
                    Futures.getChecked(DeviceManager.getDeviceManager().getLoggingState(device.address(), source, loggingStatus ->
                    {
                        if(loggingStatus != null && loggingStatus.getState() == State.LOGGING_ON)
                        {
                            ret.put(source, Float.valueOf(loggingStatus.getInterval()));
                        }
                        else
                        {
                            ret.put(source, 0.0f);
                        }
                    }), Exception.class);
                }
                catch(Exception ignored) {}
            }

            return ret;
        }

        protected void onProgressUpdate(Integer... progress) {}

        protected void onPostExecute(Map<DataSource, Float> options)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
            View root = getActivity().getLayoutInflater().inflate(R.layout.logging_options_dialog, null);
            CustomAutoValueListView<DataSource> dataSources = root.findViewById(R.id.logging_view_list);

            dataSources.initialize(DataSource.class, DataSource.INTERVAL_SOURCES, R.string.logging_title);

            if(options.isEmpty())
            {
                Toast.makeText(getContext(), "No Custom Types Returned Settings", Toast.LENGTH_SHORT).show();
                mSettingsButton.setEnabled(true);
                return;
            }

            dataSources.setOptions(options);

            dialog.setView(root);
            dialog.setNegativeButton("Cancel", (dialog1, which) -> { dialog1.dismiss(); mSettingsButton.setEnabled(true); });
            dialog.setPositiveButton("Send", (dialog12, which) ->
            {
                new LoggingSetSettingsTask().execute(dataSources.getCheckedOptions(), mDevice, dialog12);
            });

            dialog.create().show();
        }
    }

    private class LoggingSetSettingsTask extends AsyncTask<Object, Integer, Boolean>
    {
        Dialog mDialog;

        protected Boolean doInBackground(Object... settings)
        {
            Map<DataSource, Float> options = (Map<DataSource, Float>) settings[0];
            Device device = (Device) settings[1];
            mDialog = (Dialog) settings[2];

            DeviceManager manager = DeviceManager.getDeviceManager();

            AtomicBoolean error = new AtomicBoolean(false);

            Set<DataSource> sources = device.supportedLoggingTypes();

            if(sources == null)
            {
                return false;
            }

            for(DataSource source : sources)
            {
                int interval = options.containsKey(source) ? options.get(source).intValue() : 0;

                if(DataSource.NO_INTERVAL_SOURCES.contains(source))
                {
                    interval = source.defaultInterval();
                }

                try
                {
                    Futures.getChecked(manager.setLoggingStateWithInterval(device.address(), source, options.containsKey(source), interval, loggingStatus ->
                    {
                        if(loggingStatus != null && loggingStatus.getState() != State.LOGGING_ON && loggingStatus.getState() != State.LOGGING_OFF)
                        {
                            error.set(true);
                        }
                    }), Exception.class);
                }
                catch(Exception e)
                {
                    error.set(true);
                }
            }

            return error.get();
        }

        protected void onProgressUpdate(Integer... progress) {}

        protected void onPostExecute(Boolean result)
        {
            if(result)
            {
                Toast.makeText(getContext(), "Some Error Occurred Sending Settings", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(getContext(), "Settings Sent Successfully", Toast.LENGTH_SHORT).show();
            }

            mSettingsButton.setEnabled(true);

            mDialog.dismiss();
        }
    }

    AtomicLong totalBytes = new AtomicLong(0);
    AtomicInteger numberOfFailures = new AtomicInteger(0);
    AtomicLong timeSinceLastFailure = new AtomicLong(0);

    @Override
    public void onSyncStarted(Device device)
    {
        timeSinceLastFailure.set(System.currentTimeMillis());
    }

    @Override
    public void onSyncComplete(Device device) {}

    @Override
    public void onSyncFailed(Device device, Exception e)
    {
        int failures = numberOfFailures.incrementAndGet();
        long total = totalBytes.get();

        long averageBytes = failures == 0 ? -1 : total / failures;

        long currentTime = System.currentTimeMillis();
        long lastFailure = timeSinceLastFailure.get();

        long timeSince = currentTime - lastFailure;

        @SuppressLint("DefaultLocale") String time = timeSince > 60000 ? String.format("%d minutes", TimeUnit.MILLISECONDS.toMinutes(timeSince)) :
                String.format("%d seconds", TimeUnit.MILLISECONDS.toSeconds(timeSince));

        Log.e("Logging Metrics", String.format("Total Failures=[%d]\nTotal Bytes Downloaded=[%d]\nTime Since Last Failure=[%s]\nAverage Bytes Per Failure=[%d]",
                failures, total, time, averageBytes));
    }

    @Override
    public void onSyncProgress(Device device, int progress)
    {
        long total = totalBytes.addAndGet(471);

        if(progress % 100 == 0)
        {
            int failures = numberOfFailures.get();

            long averageBytes = failures == 0 ? -1 : total / failures;

            long currentTime = System.currentTimeMillis();
            long lastFailure = timeSinceLastFailure.get();

            long timeSince = currentTime - lastFailure;

            @SuppressLint("DefaultLocale") String time = timeSince > 60000 ? String.format("%d minutes", TimeUnit.MILLISECONDS.toMinutes(timeSince)) :
                    String.format("%d seconds", TimeUnit.MILLISECONDS.toSeconds(timeSince));

            Log.e("Logging Metrics", String.format("Total Failures=[%d]\nTotal Bytes Downloaded=[%d]\nTime Since Last Failure=[%s]\nAverage Bytes Per Failure=[%d]",
                    failures, total, time, averageBytes));
        }
    }
}
