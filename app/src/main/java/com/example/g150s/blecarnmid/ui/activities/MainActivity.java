package com.example.g150s.blecarnmid.ui.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.g150s.blecarnmid.R;
import com.example.g150s.blecarnmid.others.Car;
import com.example.g150s.blecarnmid.ui.adapter.ConnectedRLAdapter;
import com.example.g150s.blecarnmid.ui.adapter.SearchingRlAdapter;
import com.example.g150s.blecarnmid.ui.base.BaseActivity;
import com.example.g150s.blecarnmid.utils.TimeUtil;
import com.nightonke.jellytogglebutton.JellyToggleButton;
import com.nightonke.jellytogglebutton.JellyTypes.Jelly;
import com.nightonke.jellytogglebutton.State;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseActivity {
    private List<BluetoothDevice> mSearchBluetoothList = new ArrayList<>();
    private List<Car> mConnectBluetoothList = new ArrayList<>();

    private static final int TIMEENABLED = 120;
    private static final int SEARCHTIME = 15;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 530;
    private static final int SCAN_PERIOD = 15000;

    final int msg1 = 101;
    final int msg2 = 102;
    final int msg3 = 103;
    final int msg4 = 104;
    int bleId = 0;

    private Timer timer;
    private TimerTask timerTask;
    private Timer searchTimer;
    private TimerTask searchTask;

    private int timeenabled = TIMEENABLED;
    /**
     * 允许检测到的时间
     */
    private int searchTime = 15;
    /**
     * 搜索蓝牙设备的时间
     */

    //搜索状态的标示
    private boolean mScanning;

    private Toolbar toolbar;
    /*蓝牙开关*/
    private JellyToggleButton bleButton;
    /*开放检测的开关*/
    private JellyToggleButton scanEnabled;
    private TextView remarkText;

    private ContentLoadingProgressBar progressBar;

    private RecyclerView connectedRL;
    private RecyclerView.LayoutManager connectedLayoutManager;
    private ConnectedRLAdapter connectedRLAdapter;

    private RecyclerView searchingRL;
    private RecyclerView.LayoutManager searchingLayoutManager;
    private SearchingRlAdapter searchingRlAdapter;

    private ImageView searchIMG;
    private ImageView moreIMG;

    private TextView searchText, noConnect, noSearch;

    SharedPreferences spre;

    private BluetoothAdapter mBluetoothAdapter;
    //请求启用蓝牙请求码
    private static final int REQUEST_ENABLE_BT = 1;

    private final static String TAG = MainActivity.class.getSimpleName();

    /**
     * 1.在开机引导页判断用户手机是否支持蓝牙4.0
     * 2.加入蓝牙模块后 点击搜索设备时 应先检测蓝牙是否开启 如果没有开启蓝牙功能 提示用户开启蓝牙功能
     * 3.已配对设备列表有 添加 移除 已配对设备的方法
     * 4.可用设备搜索列表 有Item点击接口
     */

    @Override
    protected void onResume() {
        super.onResume();
        // 确保蓝牙在设备上可以开启
        if (!mBluetoothAdapter.isEnabled() || mBluetoothAdapter == null) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        mConnectBluetoothList.clear();
        spre= getSharedPreferences("myPref", MODE_PRIVATE);
        if (spre.getInt("Id", 0) != 0) {
            for(int i = 1;i <= spre.getInt("Id",0);i++) {
                Log.d(TAG,"bleId" + spre.getInt("Id", 0));
                Car car = new Car(spre.getString("Name"+i,"ErrorName"),spre.getString("Address"+i,"ErrorAddress"));
                mConnectBluetoothList.add(car);
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAdapter();

        initViews();

        initPermission();
    }

    private void initAdapter() {
        //初始化蓝牙适配器
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "您的设备不支持蓝牙BLE，将关闭", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_CONTACTS)) {
                showMessageOKCancel("你必须允许这个权限", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
                });
                return;
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }



    private void initViews() {

        initToolbar();

        findId();

        initProgressBar();

        initConnectRL();

        initSearchRL();

        if (connectedRLAdapter.getItemCount() != 0) {
            noConnect.setVisibility(View.GONE);
        }
        if (searchingRlAdapter.getItemCount() != 0) {
            noSearch.setVisibility(View.GONE);
        }

        initBleButton();

        initEnable();

        initSearch();
    }




    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.d(TAG, "显示" + mScanning);
            handle.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanning) {
                        Log.d(TAG, "结束搜索");
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        searchText.setText("开始搜索");
                        invalidateOptionsMenu();
                    }
                }
            }, SCAN_PERIOD);
            Log.d(TAG, "开始");
            mScanning = true;
            handle.sendEmptyMessage(msg3);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Log.d(TAG, "mLeScanCallback 搜索结果");
                            //获取到蓝牙设备
                            //可以判断是否添加
                            if (!mSearchBluetoothList.contains(device)) {
                                mSearchBluetoothList.add(device);
                                Log.d(TAG, "mLeScanCallback 搜索结果   " + device.getAddress());
                            }
                            //List加载适配器
                            if (searchingRlAdapter.isEmpty()) {
                                Log.d(TAG, "mLeDeviceListAdapter为空");
                            } else {
                                Log.d(TAG, "mLeDeviceListAdapter设置");
                                searchingRL.setAdapter(searchingRlAdapter);
                            }
                            handle.sendEmptyMessage(msg3);
                        }
                    });
                }
            };

    final Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case msg1:
                    remarkText.setText("允许周围设备检测到(" + TimeUtil.MilltoMinute(timeenabled--) + ")");
                    if (timeenabled == 0) {
                        timer.cancel();
                        timerTask.cancel();
                        timeenabled = 120;
                    }
                    break;
                case msg2:
                    searchTime--;
                    if (searchTime == 0) {
                        searchTimer.cancel();
                        searchTask.cancel();
                        searchTime = 15;
                        progressBar.hide();
                    }
                    break;
                case msg3:
                    searchingRlAdapter.notifyDataSetChanged();
                    break;
                case msg4:
                    connectedRLAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };
    private void findId() {
        noConnect = (TextView) findViewById(R.id.noConnect_text);
        noSearch = (TextView) findViewById(R.id.noSearch_text);
        searchText = (TextView) findViewById(R.id.search_text);
        searchIMG = (ImageView) findViewById(R.id.searchIMG);
        moreIMG = (ImageView) findViewById(R.id.moreIMG);
        remarkText = (TextView) findViewById(R.id.remark);
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
    }

    private void initProgressBar() {
        progressBar = (ContentLoadingProgressBar) findViewById(R.id.circle_loading);
        progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.MULTIPLY);
        progressBar.hide();
    }

    private void initConnectRL() {
        connectedRL = (RecyclerView) findViewById(R.id.connectedRL);
        connectedLayoutManager = new LinearLayoutManager(this);
        connectedRL.setLayoutManager(connectedLayoutManager);
        connectedRL.setHasFixedSize(true);
        connectedRLAdapter = new ConnectedRLAdapter(getApplicationContext(),mConnectBluetoothList);
        connectedRL.setAdapter(connectedRLAdapter);
        connectedRLAdapter.setOnItemClickListener(new ConnectedRLAdapter.OnConnectItemClickListener() {
            @Override
            public void onItemClick(View view, Car device) {
                showConnectDialog(device.getCarName(),device.getCarAddress());
            }
        });
        /*
        connectedRL.setItemAnimator(new DefaultItemAnimator());
        */
    }

    private void initSearchRL() {
        searchingRL = (RecyclerView) findViewById(R.id.searchingRL);
        searchingLayoutManager = new LinearLayoutManager(this);
        searchingRL.setLayoutManager(searchingLayoutManager);
        searchingRL.setHasFixedSize(true);
        searchingRlAdapter = new SearchingRlAdapter(getApplicationContext(), mSearchBluetoothList);
        searchingRlAdapter.setOnItemClickListener(new SearchingRlAdapter.OnSearchingItemClickListener() {
            @Override
            public void onItemClick(View view, BluetoothDevice device, int position) {
                Toast.makeText(MainActivity.this, "名字：" + device.getName() + "地址：" + device.getAddress(), Toast.LENGTH_SHORT).show();
                showNormalDialog(device.getName(),device.getAddress(),position);

            }
        });
        searchingRL.setAdapter(searchingRlAdapter);
    }

    private void initBleButton() {
        //可以不用此操作
        bleButton = (JellyToggleButton) findViewById(R.id.ble_turn);
        bleButton.setJelly(Jelly.ITSELF);
        bleButton.setLeftBackgroundColor(Color.parseColor("gray"));
        bleButton.setOnStateChangeListener(new JellyToggleButton.OnStateChangeListener() {
            @Override
            public void onStateChange(float process, State state, JellyToggleButton jtb) {
                if (state.equals(State.LEFT)) {
                    /* 执行关闭蓝牙操作*/
                    Toast.makeText(MainActivity.this, "关闭蓝牙", Toast.LENGTH_SHORT).show();
                    /*恢复默认时间*/
                    if (timer != null || timerTask != null) {
                        timer.cancel();
                        timerTask.cancel();
                        timeenabled = 120;
                    }
                    if (searchTimer != null || searchTask != null) {
                        searchTimer.cancel();
                        searchTask.cancel();
                        searchTime = 15;
                        progressBar.hide();
                    }
                }
                if (state.equals(State.RIGHT)) {
                    /* 执行打开蓝牙操作*/
                    Toast.makeText(MainActivity.this, "打开蓝牙", Toast.LENGTH_SHORT).show();
                    /*1,检查蓝牙是否打开
                      2，打开即不操作，没打开就打开
                      3，6.0以上需要加入适当权限，可在进入页面展开
                     */
                }
            }
        });
    }

    private void initEnable() {
        scanEnabled = (JellyToggleButton) findViewById(R.id.enable_btn);
        scanEnabled.setJelly(Jelly.ITSELF);
        scanEnabled.setLeftBackgroundColor(Color.parseColor("gray"));
        scanEnabled.setOnStateChangeListener(new JellyToggleButton.OnStateChangeListener() {
            @Override
            public void onStateChange(float process, State state, JellyToggleButton jtb) {
                if (state.equals(State.LEFT)) {
                    /** 执行不允许开放检测操作*/
                    Toast.makeText(MainActivity.this, "不允许开放检测", Toast.LENGTH_SHORT).show();
                    if (timer != null || timerTask != null) {
                        timer.cancel();
                        timerTask.cancel();
                        timeenabled = 120;
                    }
                    remarkText.setText(R.string.open_remark);
                }
                if (state.equals(State.RIGHT)) {
                    /** 执行允许开放检测*/
                    Toast.makeText(MainActivity.this, "允许开放检测", Toast.LENGTH_SHORT).show();
                    timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            Message message = new Message();
                            message.what = msg1;
                            message.obj = timeenabled;
                            handle.sendMessage(message);
                        }
                    };
                    timer = new Timer();
                    timer.schedule(timerTask, 0, 1000);
                }
            }
        });
    }

    private void initSearch() {
        searchIMG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mBluetoothAdapter.isEnabled() || mBluetoothAdapter == null) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                if (!mScanning) {
                    mSearchBluetoothList.clear();
                    searchingRlAdapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, "Search!", Toast.LENGTH_SHORT).show();
                    searchText.setText("停止搜索");
                    progressBar.show();
                    scanLeDevice(true);
                    if (searchTask != null || searchTimer != null) {
                        searchTimer.cancel();
                        searchTask.cancel();
                        searchTime = 15;
                    }
                    searchTask = new TimerTask() {
                        @Override
                        public void run() {
                            Message message = new Message();
                            message.what = msg2;
                            message.obj = searchTime;
                            handle.sendMessage(message);
                        }
                    };
                    searchTimer = new Timer();
                    searchTimer.schedule(searchTask, 0, 1000);
                } else {
                    searchText.setText("开始搜索");
                    scanLeDevice(false);
                    progressBar.hide();
                }
            }
        });
    }

    private void showNormalDialog(final String name, final String address,final int position){
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setIcon(R.drawable.ble_weclome_img);
        normalDialog.setTitle("添加设备");
        normalDialog.setMessage("是否添加"+name);
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do传入数据库，建立连接，退出
                        if (!checkAddress(address)){
                            bleId++;
                            Log.d("123", bleId+"scan");
                            SharedPreferences.Editor editor = spre.edit();
                            editor.putString("Name"+bleId, name);
                            editor.putString("Address"+bleId, address);
                            editor.putInt("Id", bleId);
                            editor.apply();
                            connectedRLAdapter.addItem(name,address);
                            mSearchBluetoothList.remove(position);
                            searchingRlAdapter.notifyDataSetChanged();
                        }
                    }
                });
        normalDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        // 显示
        normalDialog.show();
    }

    private void showConnectDialog(final String name, final String address){
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setIcon(R.drawable.ble_weclome_img);
        normalDialog.setTitle("连接小车");
        normalDialog.setMessage("是否连接小车"+name);
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...连接小车

                    }
                });
        normalDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        // 显示
        normalDialog.show();
    }
    private boolean checkAddress(String address){
        if (bleId != 0){
            for (int i = 1;i<=bleId;i++){
                Log.d(TAG, address + spre.getString("Address" + i, "none"));
                if (address.equals(spre.getString("Address"+i,"none")) ){
                    Toast.makeText(this, "已经添加该设备", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }
        return false;
    }
}
