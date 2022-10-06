package ru.fabit.remoteservicecoroutines.remoteservice;

import java.util.Map;


public interface RemoteServiceConfig {

    String getBaseUrl();

    String getUploadServerUrl();

    Map<String, String> getDefaultHeaders();

    long getConnectTimeoutMillis();

    long getReadTimeoutMillis();

    boolean isLogEnabled();

    int getMaxRetries();

}
