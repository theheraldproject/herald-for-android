//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.app;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.heraldprox.herald.sensor.DefaultSensorDelegate;
import io.heraldprox.herald.sensor.SensorArray;
import io.heraldprox.herald.sensor.data.ConcretePayloadDataFormatter;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.Resettable;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

public class AutomatedTestClient extends DefaultSensorDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("App", "AutomatedTestClient");
    private final static String tag = AutomatedTestClient.class.getName();
    @NonNull
    private final String serverAddress;
    @NonNull
    private final Context context;
    @NonNull
    private final SensorArray sensorArray;
    @NonNull
    private final TimeInterval heartbeatInterval;
    @NonNull
    private final PowerManager.WakeLock wakeLock;
    @NonNull
    private final Thread timerThread;
    private final List<Resettable> resettables = new ArrayList<>();
    private final Queue<String> commandQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean sensorArrayState = new AtomicBoolean(false);
    private long lastActionHeartbeat = 0;
    private boolean processingQueue = false;

    public AutomatedTestClient(@NonNull final String serverAddress, @NonNull final Context context, @NonNull final SensorArray sensorArray, @NonNull final TimeInterval heartbeatInterval) {
        this.serverAddress = (serverAddress.endsWith("/") ? serverAddress : serverAddress + "/");
        this.context = context;
        this.sensorArray = sensorArray;
        this.heartbeatInterval = heartbeatInterval;
        // Thread based reliable timer
        final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Herald:AutomatedTestClient");
        wakeLock.acquire(); // Deliberate use wakelock forever and actively manage sleep time so as not to waste battery
        this.timerThread = new Thread(new Runnable() {
            private long last = 0;
            @Override
            public void run() {
                while (true) {
                    final long now = System.currentTimeMillis();
                    final long elapsed = now - last;
                    if (elapsed >= 1000) {
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                if (now - lastActionHeartbeat < heartbeatInterval.millis()) {
                                    return;
                                }
                                actionHeartbeat();
                                lastActionHeartbeat = now;
                            }
                        });
                        last = now;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (Throwable e) {
                        Log.e(tag, "Timer interrupted", e);
                    }

                }
            }
        });
        this.timerThread.setPriority(Thread.MAX_PRIORITY);
        this.timerThread.setName("Herald.AutomatedTestClient");
        this.timerThread.start();
        // Add resettable
        add(logger);
    }

    // MARK: - SensorDelegate

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull SensorState didUpdateState) {
        if (sensor == SensorType.ARRAY) {
            sensorArrayState.set(didUpdateState == SensorState.on);
            Log.d(tag, "sensor (didUpdateState=" + didUpdateState + ")");
            actionHeartbeat();
        }
    }

    /**
     * Add resettable item for resetting on clear action.
     * @param resettable
     */
    public void add(@NonNull Resettable resettable) {
        resettables.add(resettable);
    }

    private void processQueue() {
        if (processingQueue) {
            return;
        }
        String command = null;
        while ((command = commandQueue.poll()) != null) {
            processingQueue = true;
            if ("start".equals(command)) {
                Log.d(tag, "processQueue, processing (command=start,action=startSensorArray)");
                sensorArray.start();
            } else if ("stop".equals(command)) {
                Log.d(tag, "processQueue, processing (command=stop,action=stopSensorArray)");
                sensorArray.stop();
            } else if (command.startsWith("upload")) {
                final String filename = command.substring("upload(".length()).replace(")","");
                Log.d(tag, "processQueue, processing (command=upload,action=uploadFile,filename=" + filename + ")");
                actionUpload(filename);
            } else if ("clear".equals(command)) {
                Log.d(tag, "processQueue, processing (command=upload,action=clear)");
                actionClear();
            } else {
                Log.w(tag, "processQueue, ignoring unknown command (command=" + command + ")");
            }
        }
        processingQueue = false;
    }

    // MARK: - Actions

    private void actionHeartbeat() {
        try {
            final String model = android.os.Build.MODEL;
            final String os = "Android";
            final String version = Integer.toString(android.os.Build.VERSION.SDK_INT);
            final String payload = new ConcretePayloadDataFormatter().shortFormat(sensorArray.payloadData());
            final String status = (sensorArrayState.get() ? "on" : "off");
            serverHeartbeat(model, os, version, payload, status, new Runnable() {
                @Override
                public void run() {
                    processQueue();
                }
            });
        } catch (Throwable e) {
            Log.e(tag, "actionHeartbeat failed", e);
        }
    }

    private void actionUpload(@NonNull final String filename) {
        final InputStream inputStream = TextFile.inputStream(context, filename);
        if (null == inputStream) {
            Log.w(tag, "actionUpload failed, unable to open file (filename=" + filename + ")");
            return;
        }
        try {
            final String model = android.os.Build.MODEL;
            final String os = "Android";
            final String version = Integer.toString(android.os.Build.VERSION.SDK_INT);
            final String payload = new ConcretePayloadDataFormatter().shortFormat(sensorArray.payloadData());
            final String status = (sensorArrayState.get() ? "on" : "off");
            serverUpload(model, os, version, payload, status, filename, inputStream);
        } catch (Throwable e) {
            Log.e(tag, "actionHeartbeat failed", e);
        }
    }

    private void actionClear() {
        // Reset all resettables
        for (final Resettable resettable : resettables) {
            try {
                resettable.reset();
            } catch (Throwable e) {
                Log.e(tag, "actionClear, failed to reset (resettable=" + resettable + ")", e);
            }
        }
        // Reset all text files
        for (final TextFile textFile : TextFile.listAll(context)) {
            try {
                textFile.reset();
            } catch (Throwable e) {
                Log.e(tag, "actionClear, failed to reset (textFile=" + textFile + ")", e);
            }
        }
     }

    // MARK: - Server API

    private void serverHeartbeat(@NonNull final String model, @NonNull final String os, @NonNull final String version,
                                 @NonNull final String payload, @NonNull final String status, @Nullable final Runnable postProcess) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final URL url = new URL(serverAddress + "heartbeat?model=" + model + "&os=" + os + "&version=" + version + "&payload=" + payload + "&status=" + status);
                    final String response = getUrl(url);
                    lastActionHeartbeat = System.currentTimeMillis();
                    if (null == response) {
                        Log.w(tag, "serverHeartbeat, no response from server");
                        return;
                    }
                    if (!response.startsWith("ok")) {
                        Log.w(tag, "serverHeartbeat, server responded with error (response=" + response + ")");
                        return;
                    }
                    final String[] commands = response.split(",");
                    for (int i=1; i<commands.length; i++) {
                        commandQueue.add(commands[i]);
                    }
                    Log.d(tag, "serverHeartbeat, complete (commandQueue=" + commandQueue + ")");
                    if (null == postProcess) {
                        return;
                    }
                    postProcess.run();
                    Log.d(tag, "serverHeartbeat, post process complete");
                } catch (Throwable e) {
                    Log.e(tag, "serverHeartbeat, failed on exception", e);
                }
            }
        });
    }

    private void serverUpload(@NonNull final String model, @NonNull final String os, @NonNull final String version,
                              @NonNull final String payload, @NonNull final String status,
                              @NonNull final String filename, @NonNull final InputStream inputStream) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final URL url = new URL(serverAddress + "upload?model=" + model + "&os=" + os + "&version=" + version + "&payload=" + payload + "&status=" + status + "&filename=" + filename);
                    final String response = postUrl(url, inputStream);
                    lastActionHeartbeat = System.currentTimeMillis();
                    if (null == response) {
                        Log.w(tag, "serverUpload, no response from server");
                        return;
                    }
                    if (!response.startsWith("ok")) {
                        Log.w(tag, "serverUpload, server responded with error (response=" + response + ")");
                        return;
                    }
                    Log.d(tag, "serverUpload, complete (response=" + response + ")");
                } catch (Throwable e) {
                    Log.e(tag, "serverUpload, failed on exception", e);
                }
            }
        });
    }

    // MARK: - Utility functions

    @Nullable
    private String getUrl(@NonNull final URL url) {
        String response = null;
        try {
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                final byte[] byteArray = read(urlConnection.getInputStream());
                response = new String(byteArray, StandardCharsets.UTF_8);
            } catch (Throwable e) {
                Log.e(tag, "getUrl, failed to read response (url=" + url + ")", e);
            }
            urlConnection.disconnect();
        } catch (Throwable e) {
            Log.e(tag, "getUrl, failed to connect/disconnect", e);
        }
        return response;
    }

    @Nullable
    private String postUrl(@NonNull final URL url, @NonNull final InputStream inputStream) {
        String response = null;
        try {
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setUseCaches(false);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Connection", "Keep-Alive");
                urlConnection.setRequestProperty("Cache-Control", "no-cache");
                pipe(inputStream, urlConnection.getOutputStream());
                final byte[] byteArray = read(urlConnection.getInputStream());
                response = new String(byteArray, StandardCharsets.UTF_8);
            } catch (Throwable e) {
                Log.e(tag, "postUrl, failed to read response (url=" + url + ")", e);
            }
            urlConnection.disconnect();
        } catch (Throwable e) {
            Log.e(tag, "postUrl, failed to connect/disconnect", e);
        }
        return response;
    }

    /**
     * Pipe all data from input stream to output stream.
     * @param inputStream
     * @param outputStream
     * @throws IOException
     */
    private void pipe(@NonNull final InputStream inputStream, @NonNull final OutputStream outputStream) throws IOException {
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        final byte[] bytesBuffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = bufferedInputStream.read(bytesBuffer, 0, bytesBuffer.length)) != -1) {
            bufferedOutputStream.write(bytesBuffer, 0, bytesRead);
        }
        bufferedOutputStream.flush();
        bufferedOutputStream.close();
        bufferedInputStream.close();
        inputStream.close();
    }

    /**
     * Read all bytes from input stream and close stream on completion.
     * @param inputStream Input stream to be read.
     * @return All bytes from input stream.
     * @throws IOException
     */
    @NonNull
    private byte[] read(@NonNull final InputStream inputStream) throws IOException {
        final OutputStream outputStream = new ByteArrayOutputStream();
        pipe(inputStream, outputStream);
        final byte[] byteArray = ((ByteArrayOutputStream) outputStream).toByteArray();
        return byteArray;
    }

}
