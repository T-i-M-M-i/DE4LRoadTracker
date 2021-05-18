package io.timmi.de4lroadtracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTConnection  implements SharedPreferences.OnSharedPreferenceChangeListener {


    private static final String TAG = "MQTTConnection";
    private SharedPreferences settings;
    @Nullable
    MqttAndroidClient mqttAndroidClient;
    @Nullable
    private Context appContext = null;
    private Boolean subscriptionEnabled = true;


    String clientId = "ExampleAndroidClient";
    final String subscriptionTopic = BuildConfig.BUILD_TYPE.toLowerCase().equals("debug")
            ? AppConstants.MQTT_SUBSCRIBE_TOPIC_DEBUG
            : AppConstants.MQTT_SUBSCRIBE_TOPIC_PRODUCTION;
    final String publishTopic = BuildConfig.BUILD_TYPE.toLowerCase().equals("debug")
            ?  AppConstants.MQTT_PUBLISH_TOPIC_DEBUG
            : AppConstants.MQTT_PUBLISH_TOPIC_PRODUCTION;

    public MQTTConnection(Context context, Context appContext) {
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.registerOnSharedPreferenceChangeListener(this);

        this.appContext = appContext;
        connectMqtt();

    }

    public MQTTConnection(Context context, Context appContext, Boolean withSubscriptionEnabled) {
        subscriptionEnabled = withSubscriptionEnabled;
        new MQTTConnection(context, appContext);
    }



    private void connectMqtt() {

        final String serverUri = settings.getString("mqttUrl", "");

        clientId = clientId + System.currentTimeMillis();
        if(mqttAndroidClient != null) {
            mqttAndroidClient.close();
        }

        mqttAndroidClient = new MqttAndroidClient(appContext, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MQTTConnection.this.appContext);
            private long messagesDelivered = (long) settings.getLong("messagesDelivered", 0);

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    addToHistory("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                addToHistory("The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                addToHistory("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                messagesDelivered++;
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putLong("messagesDelivered", messagesDelivered);
                settingsEditor.apply();
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        String password = settings.getString("mqttPW", "");
        String username = settings.getString("mqttUsername", "");
        if(username != null && !username.isEmpty()) {
            mqttConnectOptions.setUserName(username);
        }
        if(password != null && !password.isEmpty()) {
            mqttConnectOptions.setPassword(password.toCharArray());
        }

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Connected to: " + String.valueOf( asyncActionToken.isComplete()));
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(AppConstants.MQTT_BUFFER_SIZE);
                    disconnectedBufferOptions.setPersistBuffer(AppConstants.MQTT_PERSIST_BUFFER);
                    disconnectedBufferOptions.setDeleteOldestMessages(true);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }

    }



    private void addToHistory(String mainText){
        Log.d(TAG, "historyMessage: " + mainText);
        sendHistoryBroadcast(mainText);

    }

    public void subscribeToTopic(){
        if(!subscriptionEnabled)
            return;
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public boolean publishMessage(String publishMessage){

        //Log.d(TAG, publishMessage);
        try {
            if(mqttAndroidClient == null || !mqttAndroidClient.isConnected()) {
                Log.e(TAG, "no mqtt client available!");
                return false;
            }
        } catch(Exception e) {
            //we once got an Illegal argument exception see  #18
            Log.e(TAG,  "mqtt client error,  maybe  because in closing state", e);
            return false;
        }
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);
            addToHistory("Message Published");
            if(!mqttAndroidClient.isConnected()){
                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error Publishing: ", e);
            return false;
        }
        return true;
    }

    /**
     * This method is responsible to send broadcast to specific Action
     * */
    private void sendHistoryBroadcast(String message)
    {
        try
        {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(MainActivity.HISTORY_MESSAGE_BROADCAST);
            broadCastIntent.putExtra("historyMessage", message);

            if (appContext != null) {
                Log.i("MQTT", "[sendBroadcast]");
                appContext.sendBroadcast(broadCastIntent);
            }

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void close() {
        if(mqttAndroidClient != null) {
            try {
                mqttAndroidClient.close();
            } catch (Exception e) {
                Log.e(TAG, "Cannot close MQTT client", e);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case "mqttUrl":
            case "mqttPW":
            case "mqttUsername":
                if(appContext != null) {
                    connectMqtt();
                }
                break;
            default:
        }
    }
}
