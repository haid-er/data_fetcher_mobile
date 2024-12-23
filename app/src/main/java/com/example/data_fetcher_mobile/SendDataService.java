package com.example.data_fetcher_mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class SendDataService extends Service implements SensorEventListener {

    private static final String TAG = "SendData_Activity";
    private static final String RABBITMQ_QUEUE_NAME = "sensor_data_queue";
    private static final String RABBITMQ_USERNAME = "test";
    private static final String RABBITMQ_PASSWORD = "test";

    private SensorManager sensorManager;
    private List<ParcelableSensor> selectedSensors;
    private ExecutorService sensorExecutorService;
    private ExecutorService rabbitMQExecutorService;

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    private String serverAddress;
    private int serverPort;
    private String userId;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorExecutorService = Executors.newFixedThreadPool(3); // For sensor-related tasks
        rabbitMQExecutorService = Executors.newSingleThreadExecutor(); // For RabbitMQ-related tasks
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service received command");
        startForeground(1, createNotification());
        if (intent != null) {
            String action = intent.getAction();
            if ("START_SENDING".equals(action)) {
                sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                selectedSensors = intent.getParcelableArrayListExtra("selected_sensors");

                serverAddress = intent.getStringExtra("server_address");
                serverPort = intent.getIntExtra("server_port", 5672);
                userId = intent.getStringExtra("userId");

                // Initialize RabbitMQ
                new InitializeRabbitMQTask().execute();

                // Show a toast message to indicate that the service is started
                Toast.makeText(this, "Sending data to " + serverAddress + ":" + serverPort + " from " + userId, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Data Sending Started");

            } else if ("STOP_SENDING".equals(action)) {
                // Stop sensor reading
                stopSensorReading();

                // Close RabbitMQ connection
                closeRabbitMQ();

                // Shutdown ExecutorServices
                shutdownExecutors();

                // Stop the service
                stopSelf();
            }
        }

        return START_STICKY; // Ensure the service restarts if the system kills it
    }

    private Notification createNotification() {
        // Create a persistent notification for the foreground service
        Notification.Builder builder = new Notification.Builder(this, "channel_id")
                .setContentTitle("Sensor Data Collection")
                .setContentText("Sending sensor data in progress...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(Notification.PRIORITY_LOW); // Adjust priority based on your needs

        // Create a notification channel (required for Android O and above)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "channel_id",
                    "Sensor Data Service",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return builder.build();
    }

    private void initializeRabbitMQ() {
        if (connection != null && connection.isOpen()) {
            Log.d(TAG, "RabbitMQ connection already initialized. Skipping reinitialization.");
            // Connection is already initialized; return early
            return;
        }
        factory = new ConnectionFactory();
        factory.setHost(serverAddress);
        factory.setVirtualHost("/");
        factory.setPort(serverPort);
        factory.setUsername(RABBITMQ_USERNAME);
        factory.setPassword(RABBITMQ_PASSWORD);
        factory.setNetworkRecoveryInterval(5000); // in milliseconds
        factory.setConnectionTimeout(30000);

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
//            connection = factory.newConnection();
//            channel = connection.createChannel();
            channel.queueDeclare(RABBITMQ_QUEUE_NAME, true, false, false, null);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to initialize RabbitMQ", e);
        }
    }

    private void closeRabbitMQ() {
        if (rabbitMQExecutorService != null && !rabbitMQExecutorService.isShutdown()) {
            rabbitMQExecutorService.submit(() -> {
                try {
                    if (channel != null && channel.isOpen()) {
                        channel.close();
                    }
                    if (connection != null && connection.isOpen()) {
                        connection.close();
                    }
                    Log.d(TAG, "RabbitMQ connection closed successfully");
                } catch (IOException | TimeoutException e) {
                    Log.e(TAG, "Failed to close RabbitMQ connection", e);
                }
            });
        } else {
            Log.w(TAG, "RabbitMQ ExecutorService is shut down; cannot close RabbitMQ");
        }
    }

    private void shutdownExecutors() {
        if (sensorExecutorService != null && !sensorExecutorService.isShutdown()) {
            sensorExecutorService.shutdownNow();
            Log.d(TAG, "Sensor ExecutorService shutdown");
        }

        if (rabbitMQExecutorService != null && !rabbitMQExecutorService.isShutdown()) {
            rabbitMQExecutorService.shutdownNow();
            Log.d(TAG, "RabbitMQ ExecutorService shutdown");
        }
    }

    private void startSensorReading() {
        sensorExecutorService.submit(() -> {
            for (ParcelableSensor parcelableSensor : selectedSensors) {
                Sensor sensor = sensorManager.getDefaultSensor(parcelableSensor.getType());
                if (sensor != null) {
                    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
                }
            }
            Log.d(TAG, "Sensor reading started");
        });
    }

    private void stopSensorReading() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
            Log.d(TAG, "Sensor reading stopped");
        } else {
            Log.w(TAG, "SensorManager is null, cannot stop sensor reading");
        }
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] sensorValues = event.values;

            for (ParcelableSensor sensor : selectedSensors) {
                if (event.sensor.getType() == sensor.getType()) {
                    sendDataToRabbitMQ(sensor.getName(), sensorValues);
                    break;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if needed
        }
    };

    private void sendDataToRabbitMQ(String sensorType, float[] sensorValues) {
        String sensorData = buildSensorDataString(sensorType, sensorValues);

        if (rabbitMQExecutorService != null && !rabbitMQExecutorService.isShutdown()) {
            rabbitMQExecutorService.submit(() -> {
                try {
                    channel.basicPublish("", RABBITMQ_QUEUE_NAME, null, sensorData.getBytes());
                    Log.d(TAG, "Sensor Data Sent: " + sensorData);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to send sensor data to RabbitMQ", e);
                }
            });
        } else {
            Log.w(TAG, "RabbitMQ ExecutorService is shut down; cannot send data");
        }
    }

    private String buildSensorDataString(String sensorType, float[] values) {
        StringBuilder builder = new StringBuilder("Source: Smart_Phone")
                .append(", User ID: ").append(userId)
                .append(", Timestamp: ").append(System.currentTimeMillis())
                .append(", Sensor Type: ").append(sensorType).append(": ");

        for (float value : values) {
            builder.append(value).append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
        builder.append("\n");
        return builder.toString();
    }

    private class InitializeRabbitMQTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            initializeRabbitMQ();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            startSensorReading();
            Log.d(TAG, "RabbitMQ initialized and sensor reading started");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Not used
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}
