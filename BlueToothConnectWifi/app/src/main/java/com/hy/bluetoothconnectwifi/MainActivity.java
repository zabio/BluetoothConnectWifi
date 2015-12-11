package com.hy.bluetoothconnectwifi;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hy.bluetoothconnectwifi.wifi.WIfiConnectionListener;
import com.hy.bluetoothconnectwifi.wifi.WifiSelectDialog;
import com.hy.bluetoothconnectwifi.wifi.WifiUtil;

import org.json.JSONException;
import org.json.JSONObject;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends Activity implements View.OnClickListener, Handler.Callback {

    static final int MSG_WIFI_OFF = -1;
    static final int MSG_WIFI_LIST_NONE = 2;
    static final int MSG_WIFI_CONNECTING = 3;
    static final int MSG_WIFI_CONNECTED = 4;
    static final int MSG_WIFI_FIND_LIST = 5;
    static final int MSG_WIFI_CONNECT_FAILED = 6;
    static final int MSG_WIFI_CONNECT_GET_IP = 7;
    static final int MSG_WIFI_ON = 8;
    static final int MSG_WIFI_INFO = 9;
    private TextView mTvBtName;
    private EditText mEtBtName;
    private Button mBtnSetWifi;
    private Button mBtnTest;
    private BluetoothSPP bt;
    private SharedPreferences mShare;
    static final String FILE_NAME = "SPP_BT_MAC";
    static final String SP_ARG_NAME = "SP_ARG_NAME";
    static final String SP_ARG_MAC = "SP_ARG_MAC";
    static final String ORDER_TEST_SEND = "长江长江我是黄河";
    static final String ORDER_TEST_GET = "黄河黄河我是长江";
    static final String ORDER_WIFI_SUCCESS = "SUCCESS";
    private String mBtName;
    private String mBtMac;
    private WifiManager mManager;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler(this);
        getShareFile();
        initSpp();
        initSetupWifi();
        initView();
    }


    private void getShareFile() {
        mShare = getSharedPreferences(FILE_NAME, MODE_PRIVATE);
        if (null != mShare) {
            mBtName = mShare.getString(SP_ARG_NAME, "");
            mBtMac = mShare.getString(SP_ARG_MAC, "");
        }

    }


    private void initSetupWifi() {
        mManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!mManager.isWifiEnabled()) {
            //open wifi
            mManager.setWifiEnabled(true);
        }
    }

    private void initSpp() {
        bt = new BluetoothSPP(this);

        if (!bt.isBluetoothEnabled()) {
            bt.enable();
        }

        if (!bt.isServiceAvailable()) {
            bt.setupService();
            bt.startService(BluetoothState.DEVICE_ANDROID);
        }

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {

                setBluetoothTextName(name);

                saveSpp(name, address);

                Toast.makeText(MainActivity.this, "connect已连接" + name + "----" + address, Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onDeviceDisconnected() {
                Toast.makeText(MainActivity.this, "connect断开已连接", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceConnectionFailed() {
                Toast.makeText(MainActivity.this, "connect连接失败", Toast.LENGTH_SHORT).show();

            }
        });


        bt.setAutoConnectionListener(new BluetoothSPP.AutoConnectionListener() {
            @Override
            public void onAutoConnectionStarted() {
                Toast.makeText(MainActivity.this, "自动连接开始", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNewConnection(String name, String address) {
                setBluetoothTextName(name);
                saveSpp(name, address);
                Toast.makeText(MainActivity.this, "已连接" + name + "----" + address, Toast.LENGTH_SHORT).show();
            }
        });


        bt.setBluetoothStateListener(new BluetoothSPP.BluetoothStateListener() {
            @Override
            public void onServiceStateChanged(int state) {
                switch (state) {
                    case BluetoothState.STATE_CONNECTED:
                        Log.e("Check", "State : Connected");
                        break;
                    case BluetoothState.STATE_CONNECTING:
                        Log.e("Check", "State : Connecting");
                        break;
                    case BluetoothState.STATE_LISTEN:
                        Log.e("Check", "State : Listen");
                        break;
                    case BluetoothState.STATE_NONE:
                        Log.e("Check", "State : None");
                        break;
                }
            }
        });


        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                Log.e("Check", "Length : " + data.length);
                Log.e("Check", "Message : " + message);
                if (message.equals(ORDER_TEST_SEND)) {

                    Toast.makeText(MainActivity.this, ORDER_TEST_GET, Toast.LENGTH_SHORT).show();

                } else if (message.equals(ORDER_WIFI_SUCCESS)) {

                    Toast.makeText(MainActivity.this, ORDER_WIFI_SUCCESS, Toast.LENGTH_SHORT).show();

                } else {
                    try {
                        JSONObject json = new JSONObject(message);
                        String ssd = json.getString("@SSD");
                        String pwd = json.getString("@PWD");

                        new ConnectWifiThread(ssd, pwd).execute();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        });


    }


    private void initView() {

        mTvBtName = (TextView) findViewById(R.id.tv_name);
        mTvBtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String text = s.toString();
                mBtnSetWifi.setEnabled(!TextUtils.isEmpty(text));
                mBtnTest.setEnabled(!TextUtils.isEmpty(text));
            }
        });
        mEtBtName = (EditText) findViewById(R.id.et_bt_name);

        findViewById(R.id.btn_select).setOnClickListener(this);
        findViewById(R.id.btn_key_word).setOnClickListener(this);
        findViewById(R.id.btn_last).setOnClickListener(this);
        mBtnSetWifi = (Button) findViewById(R.id.btn_set_wifi);
        mBtnTest = (Button) findViewById(R.id.btn_test);

        mBtnSetWifi.setEnabled(false);
        mBtnTest.setEnabled(false);

        mBtnSetWifi.setOnClickListener(this);
        mBtnTest.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_select:
                Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                break;
            case R.id.btn_key_word:
                keyWordConnect();
                break;
            case R.id.btn_last:
                bt.autoConnect(mBtName);
                break;
            case R.id.btn_set_wifi:
                setWifi();
                break;
            case R.id.btn_test:
                Toast.makeText(this, ORDER_TEST_SEND, Toast.LENGTH_SHORT).show();
                bt.send(ORDER_TEST_SEND, true);
                break;
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_WIFI_CONNECT_GET_IP:
                Toast.makeText(this, "正在获取ip地址...", Toast.LENGTH_SHORT).show();
                new RefreshSsidThread().start();
                break;
            case MSG_WIFI_CONNECTING:
                Toast.makeText(this, "正在连接...", Toast.LENGTH_SHORT).show();
                break;
            case MSG_WIFI_CONNECTED:
                Toast.makeText(this, "wifi连接成功", Toast.LENGTH_SHORT).show();
                bt.send(ORDER_WIFI_SUCCESS, true);
                break;
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_ANDROID);

            } else {
                // Do something if user doesn't choose any device (Pressed back)
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        bt.stopService();
    }


    private void saveSpp(String name, String mac) {
        mBtName = name;
        mBtMac = mac;
        if (null != mShare) {
            mShare.edit().putString(SP_ARG_NAME, name).putString(SP_ARG_MAC, mac).apply();
        }
    }


    private void setWifi() {
        WifiSelectDialog selectDialog = new WifiSelectDialog();
        selectDialog.setWifiConnectionListener(new WIfiConnectionListener() {
            @Override
            public void onWifiConnected(String ssd, String pwd) {
                JSONObject json = new JSONObject();
                try {
                    json.put("@SSD", ssd);
                    json.put("@PWD", pwd);
                    bt.send(json.toString(), false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onWifiConnectFailed() {

            }
        });
        selectDialog.show(getFragmentManager(), WifiSelectDialog.TAG);
    }


    private void keyWordConnect() {

        final String text = mEtBtName.getText().toString();
        if (TextUtils.isEmpty(text)) {
            autoConnect(text);
        } else {
            Toast.makeText(this, "请输入蓝牙名称", Toast.LENGTH_SHORT).show();
        }

    }


    private void autoConnect(String key) {
        bt.autoConnect(key);
        /*
        if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
            bt.disconnect();
        } else {
            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }*/
    }

    private void setBluetoothTextName(String textName) {
        mTvBtName.setText(textName);
    }


    /**
     * 连接wifi
     */
    class ConnectWifiThread extends AsyncTask<String, Integer, String> {

        String ssid;
        String pwd;

        public ConnectWifiThread(String ssid, String pwd) {
            this.ssid = ssid;
            this.pwd = pwd;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mHandler.sendEmptyMessage(MSG_WIFI_CONNECTING);
        }

        @Override
        protected String doInBackground(String... params) {
            // 连接配置好指定ID的网络
            WifiConfiguration config = WifiUtil.createWifiInfo(ssid, pwd, 3, mManager);

            int networkId = mManager.addNetwork(config);
            if (null != config) {
                mManager.enableNetwork(networkId, true);
                return ssid;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (null != result) {
                mHandler.sendEmptyMessage(MSG_WIFI_CONNECT_GET_IP);
            } else {
                mHandler.sendEmptyMessage(MSG_WIFI_CONNECT_FAILED);
            }
        }

    }


    /**
     * 获取网络ip地址
     */
    class RefreshSsidThread extends Thread {

        @Override
        public void run() {
            boolean flag = true;
            while (flag) {
                WifiInfo info = mManager.getConnectionInfo();
                if (null != info.getSSID()
                        && 0 != info.getIpAddress()) {
                    flag = false;

                    Message message = mHandler.obtainMessage();
                    message.what = MSG_WIFI_INFO;
                    message.obj = info;
                    mHandler.sendMessage(message);
                }
            }
            mHandler.sendEmptyMessage(MSG_WIFI_CONNECTED);
            super.run();
        }
    }

}
