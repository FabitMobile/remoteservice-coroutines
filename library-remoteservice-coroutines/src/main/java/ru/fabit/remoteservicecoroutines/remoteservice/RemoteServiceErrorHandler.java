package ru.fabit.remoteservicecoroutines.remoteservice;

import org.json.JSONObject;


public interface RemoteServiceErrorHandler {
    String getUserMessage(JSONObject jsonObject);

    String getCode(JSONObject jsonObject);

    String getErrorName(JSONObject jsonObject);

    void handleError(Throwable throwable, String requestPath);
}
