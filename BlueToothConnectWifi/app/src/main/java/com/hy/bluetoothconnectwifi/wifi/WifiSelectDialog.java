package com.hy.bluetoothconnectwifi.wifi;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hy.bluetoothconnectwifi.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by henry  15/12/2.
 */
public class WifiSelectDialog extends DialogFragment implements Handler.Callback {

    public static final String TAG = WifiSelectDialog.class.getName();
    private ProgressDialog mDialog;
    private View mRlForm;
    private EditText mEtPwd;
    private TextView mTvStatus;
    private TextView mTvWifiName;
    private Button mBtnConnect;
    private Button mBtnScan;
    private ListView mListView;
    private Handler mHandler;
    private MyAdapter mAdapter;

    private List<ScanResult> mDataList;// wifi列表
    private WifiInfo mWifiInfo;// 当前所连接的wifi
    private WifiManager mManager;
    private ScanResult mSelectResult;


    static final int MSG_WIFI_OFF = -1;
    static final int MSG_WIFI_LIST_NONE = 2;
    static final int MSG_WIFI_CONNECTING = 3;
    static final int MSG_WIFI_CONNECTED = 4;
    static final int MSG_WIFI_FIND_LIST = 5;
    static final int MSG_WIFI_CONNECT_FAILED = 6;
    static final int MSG_WIFI_CONNECT_GET_IP = 7;
    static final int MSG_WIFI_ON = 8;
    static final int MSG_WIFI_INFO = 9;

    private String mSSID;
    private String mPwd;

    private WIfiConnectionListener mListener;

    public void setWifiConnectionListener(WIfiConnectionListener mListener) {
        this.mListener = mListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(this);
        mManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wifi_select, container);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        setWindow();
        initView(view);
        return view;
    }

    private void initView(View view) {
        mDialog = new ProgressDialog(getActivity());
        mDialog.setTitle("wait");
        mDialog.setMessage("正在执行...");
        mDialog.setCanceledOnTouchOutside(false);

        mListView = (ListView) view.findViewById(R.id.listView);
        mRlForm = view.findViewById(R.id.rl_form);
        mEtPwd = (EditText) view.findViewById(R.id.et_pwd);
        mEtPwd.clearFocus();

        mTvStatus = (TextView) view.findViewById(R.id.tv_status);
        mTvWifiName = (TextView) view.findViewById(R.id.tv_wifi);
        mBtnConnect = (Button) view.findViewById(R.id.btn_connect);
        mBtnScan = (Button) view.findViewById(R.id.btn_scan);

        mBtnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanWifi();
            }
        });
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        mDataList = new ArrayList<>();
        mAdapter = new MyAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                inputWifiForm(position);
            }
        });
    }


    void setWindow() {
        int screenWidth = (int) (UIUtils.getWindowWidth(getActivity()) * 0.9);
        int screenHeight = (int) (UIUtils.getWindowHeight(getActivity()) * 0.6);

        if (screenWidth > screenHeight) {
            getDialog().getWindow().setLayout(screenHeight, screenHeight);
        } else {
            getDialog().getWindow().setLayout(screenWidth, screenWidth);
        }


    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_WIFI_OFF:
                setStatusMsg("Wifi未启用");
                break;
            case MSG_WIFI_ON:
                setStatusMsg("Wifi正在开启...");
                break;
            case MSG_WIFI_LIST_NONE:
                setStatusMsg("没有发现可用wifi...请稍后正在扫描");
                break;
            case MSG_WIFI_FIND_LIST:
                setStatusMsg("可用wifi列表");
                mAdapter.notifyDataSetChanged();
                break;
            case MSG_WIFI_INFO:
                mWifiInfo = (WifiInfo) msg.obj;
                mAdapter.notifyDataSetChanged();
                break;
            case MSG_WIFI_CONNECT_GET_IP:
                setStatusMsg("正在获取ip地址...");
                new RefreshSsidThread().start();
                break;
            case MSG_WIFI_CONNECTING:
                mDialog.show();
                setStatusMsg("正在连接wifi ...");
                break;
            case MSG_WIFI_CONNECTED:
                mAdapter.notifyDataSetChanged();
                setStatusMsg("wifi连接成功");
                mDialog.dismiss();
                if (null != mListener) {
                    mListener.onWifiConnected(mSSID, mPwd);
                    dismiss();
                }
                break;
        }
        return false;
    }

    private void setStatusMsg(String statusMsg) {
        mTvStatus.setText(statusMsg);
    }

    private void inputWifiForm(int position) {

        mSelectResult = mDataList.get(position);
        if (mWifiInfo != null && mWifiInfo.getSSID().equals("\"" + mSelectResult.SSID + "\"")) {
            Toast.makeText(getActivity(), "已经连接好了", Toast.LENGTH_SHORT).show();
        } else {
            mRlForm.setVisibility(View.VISIBLE);
            mEtPwd.requestFocus();
            mTvWifiName.setText(mSelectResult.SSID);
        }
    }


    private void startScanWifi() {
        if (!mManager.isWifiEnabled()) {

            mHandler.sendEmptyMessage(MSG_WIFI_OFF);
            //open wifi
            mManager.setWifiEnabled(true);

            mHandler.sendEmptyMessage(MSG_WIFI_ON);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    new ScanWifiThread().start();
                }
            }, 3000);
        } else {
            new ScanWifiThread().start();

        }
    }


    private void connect() {
        mPwd = mEtPwd.getText().toString();
        if (!TextUtils.isEmpty(mPwd) && mPwd.length() >= 8) {
            mSSID = mSelectResult.SSID;
            new ConnectWifiThread(mSelectResult.SSID, mPwd).execute();
            mRlForm.setVisibility(View.GONE);
            mEtPwd.setHint("请输入8位密码");
            mEtPwd.clearFocus();
        } else {
            mEtPwd.setHint("密码不能为空,长度至少为8位");
        }
    }

    class ScanWifiThread extends Thread {

        @Override
        public void run() {
            while (true) {
                startScan();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }


    /**
     * 扫描wifi
     */
    public void startScan() {
        mManager.startScan();
        // 获取扫描结果
        List<ScanResult> results = mManager.getScanResults();

        if (null != results && results.size() > 0) {

            WifiInfo wifiInfo = mManager.getConnectionInfo();

            mDataList.clear();
            mDataList.addAll(results);
            mHandler.sendEmptyMessage(MSG_WIFI_FIND_LIST);

            Message message = mHandler.obtainMessage();
            message.what = MSG_WIFI_INFO;
            message.obj = wifiInfo;
            mHandler.sendMessage(message);

        } else {
            mHandler.sendEmptyMessage(MSG_WIFI_LIST_NONE);
        }

    }


    public class MyAdapter extends BaseAdapter {

        LayoutInflater inflater;


        public MyAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return null != mDataList && mDataList.size() > 0 ? mDataList.size() : 0;
        }

        @Override
        public ScanResult getItem(int position) {
            return null != mDataList && mDataList.size() > 0 ? mDataList.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            view = inflater.inflate(R.layout.wifi_item, null);
            TextView textView = (TextView) view.findViewById(R.id.textView);
            TextView connect = (TextView) view.findViewById(R.id.connect);

            ScanResult scanResult = getItem(position);

            if (null != scanResult) {

                if (mWifiInfo != null && mWifiInfo.getSSID().equals("\"" + scanResult.SSID + "\"")) {
                    connect.setText("已经连接");
                } else {
                    connect.setText("");
                }

                textView.setText(scanResult.SSID);
                TextView signalStrenth = (TextView) view.findViewById(R.id.signal_strenth);
                final int level = WifiManager.calculateSignalLevel(scanResult.level, 100);
                signalStrenth.setText(String.valueOf(level));
                ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
                //判断信号强度，显示对应的指示图标
                if (level > 100) {
                    imageView.setImageResource(R.drawable.stat_sys_wifi_signal_4);
                } else if (level > 80) {
                    imageView.setImageResource(R.drawable.stat_sys_wifi_signal_3);
                } else if (level > 70) {
                    imageView.setImageResource(R.drawable.stat_sys_wifi_signal_2);
                } else if (level > 60) {
                    imageView.setImageResource(R.drawable.stat_sys_wifi_signal_2);
                } else if (level > 50) {
                    imageView.setImageResource(R.drawable.stat_sys_wifi_signal_1);
                } else {
                    imageView.setImageResource(R.drawable.stat_sys_wifi_signal_1);
                }
            }
            return view;
        }

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
