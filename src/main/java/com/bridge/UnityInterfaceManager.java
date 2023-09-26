package com.bridge;

import android.util.Log;

import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class UnityInterfaceManager {

    private static volatile UnityInterfaceManager INSTANCE = null;
    private UnityInterfaceDelegate delegate;

    private UnityInterfaceManager() {}

    public static UnityInterfaceManager getInstance() {
        if (INSTANCE == null) {
            synchronized (UnityInterfaceManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UnityInterfaceManager();
                }
            }
        }
        return INSTANCE;
    }

    public void setDelegate(UnityInterfaceDelegate delegate) {

        this.delegate = delegate;
    }

    public void startTutorialGame() {
        startScene("Tutorial");
    }

    public void startCamera() {
        startScene("Camera");
    }

    private void startScene(String scene) {
        String uuid = UUID.randomUUID().toString().toLowerCase();
        HashMap<String, Object> requestDic = new HashMap<>();
        requestDic.put("requestPath", "StartApplication");
        requestDic.put("requestId", uuid);

        HashMap<String, Object> body = new HashMap<>();
        body.put("scene", scene);
        requestDic.put("body", body);

        sendData(requestDic);
        Log.d("bridging.unity", "startScene: " + scene);
    }

    public void sendData(HashMap<String, Object> data) {
        String jsonString = convertMapToJSONString(data);
        UnityPlayer.UnitySendMessage("MultiPlatformManager", "OnNativeRequest", jsonString);
        Log.d("bridging.unity", "sendData: " + jsonString);
    }

    public static void receivedMessage(String serializedData) {
        UnityInterfaceManager.getInstance().performReceivedMessage(serializedData);
    }

    public void performReceivedMessage(String message) {
        Log.d("bridging.unity", "receivedMessage: " + message);
        if (delegate != null) {
            HashMap<String, Object> params = convertJSONStringToMap(message);
            delegate.unityInterfaceReceivedParams(params, new UnityInterfaceDelegate.ResponseCallback() {
                @Override
                public void onResponse(HashMap<String, Object> response) {
                    HashMap<String, Object> result = new HashMap<>();
                    result.put("requestId", params.get("requestId"));
                    result.put("body", response);
                    String messageForTransmit = convertMapToJSONString(result);
                    UnityPlayer.UnitySendMessage("MultiPlatformManager", "OnResponseReceived", messageForTransmit);
                }
            });
        }
    }

    public interface UnityInterfaceDelegate {
        void unityInterfaceReceivedParams(HashMap<String, Object> params, ResponseCallback callback);

        interface ResponseCallback {
            void onResponse(HashMap<String, Object> response);
        }
    }

    private HashMap<String, Object> convertJSONStringToMap(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return convertJSONObjectToMap(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private HashMap<String, Object> convertJSONObjectToMap(JSONObject jsonObject) {
        try {
            HashMap<String, Object> resultMap = new HashMap<>();
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);

                if (value instanceof JSONObject) {
                    value = convertJSONObjectToMap((JSONObject) value);
                } else if (value instanceof JSONArray) {
                    value = convertJSONArrayToList((JSONArray) value);
                }

                resultMap.put(key, value);
            }

            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Object> convertJSONArrayToList(JSONArray jsonArray) {
        List<Object> resultList = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Object value = jsonArray.get(i);

                if (value instanceof JSONObject) {
                    value = convertJSONObjectToMap((JSONObject) value);
                } else if (value instanceof JSONArray) {
                    value = convertJSONArrayToList((JSONArray) value);
                }

                resultList.add(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return resultList;
    }


    private String convertMapToJSONString(HashMap<String, Object> map) {
        return new JSONObject(map).toString();
    }
}