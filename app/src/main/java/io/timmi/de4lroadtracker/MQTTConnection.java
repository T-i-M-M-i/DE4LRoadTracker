package io.timmi.de4lroadtracker;

import android.content.Context;
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

import io.timmi.de4lroadtracker.helper.DebugChannel;

public class MQTTConnection  implements SharedPreferences.OnSharedPreferenceChangeListener {


    private static final String TAG = "MQTTConnection";
    private SharedPreferences settings;
    @Nullable
    MqttAndroidClient mqttAndroidClient;
    @Nullable
    private Context appContext = null;
    private boolean subscriptionEnabled = true;
    private boolean serviceShouldStop = false;


    String clientId = "ExampleAndroidClient";
    private  String publishTopic = AppConstants.MQTT_PUBLISH_TOPIC_DEBUG;
    private String  subscriptionTopic =  AppConstants.MQTT_SUBSCRIBE_TOPIC_DEBUG;

    public MQTTConnection(Context context, Context appContext) {
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.registerOnSharedPreferenceChangeListener(this);

        boolean useDebugTopic = settings.getBoolean("useMqttDebugTopic",  BuildConfig.BUILD_TYPE.toLowerCase().equals("debug"));
        publishTopic = useDebugTopic
                ?  AppConstants.MQTT_PUBLISH_TOPIC_DEBUG
                : AppConstants.MQTT_PUBLISH_TOPIC_PRODUCTION;
        subscriptionTopic = useDebugTopic
                ?  AppConstants.MQTT_SUBSCRIBE_TOPIC_DEBUG
                : AppConstants.MQTT_SUBSCRIBE_TOPIC_PRODUCTION;

        Log.i(TAG, "will publish to topic " + publishTopic);
        this.appContext = appContext;
        connectMqtt();

    }

    public MQTTConnection(Context context, Context appContext, Boolean withSubscriptionEnabled) {
        subscriptionEnabled = withSubscriptionEnabled;
        new MQTTConnection(context, appContext);
    }



    private void connectMqtt() {

        //avoid connecting to mqtt if we are in stopping state... (could rarely happen if settings changed while stopping)
        if(serviceShouldStop)
            return;
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
                    debugHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    debugHistory("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                debugHistory("The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                debugHistory("Incoming message: " + new String(message.getPayload()));
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
                    debugHistory("Connected to: " + String.valueOf( asyncActionToken.isComplete()));
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
                    debugHistory("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }

    }



    private void debugHistory(String mainText){
        DebugChannel.sendHistoryBroadcast(appContext, TAG, mainText);
    }

    public void subscribeToTopic(){
        if(!subscriptionEnabled)
            return;
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    debugHistory("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    debugHistory("Failed to subscribe");
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
            debugHistory("Message Published");
            if(!mqttAndroidClient.isConnected()){
                debugHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error Publishing: ", e);
            return false;
        }
        return true;
    }


    public void disconnect(IMqttActionListener actionListener) throws MqttException {
        if(mqttAndroidClient != null) {
            mqttAndroidClient.disconnect(appContext, actionListener);
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
