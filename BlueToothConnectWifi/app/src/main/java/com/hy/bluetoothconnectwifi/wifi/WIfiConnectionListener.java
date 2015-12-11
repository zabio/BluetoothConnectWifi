package com.hy.bluetoothconnectwifi.wifi;

/**
 * Created by henry  15/12/2.
 */
public interface WIfiConnectionListener {
    void onWifiConnected(String ssd, String pwd);

    void onWifiConnectFailed();
}
