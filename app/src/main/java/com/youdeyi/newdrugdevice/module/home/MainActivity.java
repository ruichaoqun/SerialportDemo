package com.youdeyi.newdrugdevice.module.home;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kyleduo.switchbutton.SwitchButton;
import com.youdeyi.newdrugdevice.R;
import com.youdeyi.newdrugdevice.common.utils.ToastUtils;
import com.youdeyi.serialport.ByteUtil;
import com.youdeyi.serialport.Constant;
import com.youdeyi.serialport.Device;
import com.youdeyi.serialport.DeviceKey;
import com.youdeyi.serialport.DrugSellSerialHelper;
import com.youdeyi.serialport.RealSerialPort;
import com.youdeyi.serialport.SerialPortManager;
import com.youdeyi.serialport.callback.DrugSellSerialCallback;
import com.youdeyi.serialport.callback.GateControlCallback;
import com.youdeyi.serialport.callback.LightBoxControlCallback;
import com.youdeyi.serialport.callback.LightControlCallback;
import com.youdeyi.serialport.callback.MachineLightCallback;
import com.youdeyi.serialport.callback.OnGlobalErrorListener;
import com.youdeyi.serialport.callback.SerialInitCallback;
import com.youdeyi.serialport.callback.TempControlCallBack;
import com.youdeyi.serialport.callback.TempGetCallBack;
import com.youdeyi.serialport.data.CheckStatusInfo;
import com.youdeyi.serialport.data.ICommandParse;
import com.youdeyi.serialport.data.MotorRequestInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private TextView tvId;
    private TextView tvLog;           //打印日志
    private Button btnClear;           //清空日志
    private ScrollView scrollView;
    private DrugSellSerialHelper mDrugSellSerialHelper;
    private TextView tvTemp;

    private EditText mEtSingle;
    private Button mBtnRun;

    private EditText mEtMulti;
    private Button mBtnRunMulti;

    private EditText mEtStart;
    private EditText mEtEnd;
    private SwitchButton mSwitchButton;
    private Button mBtnRunBetween;

    private EditText mEtTemp;
    private EditText mEtShidu;
    private Button mBtnRunTemp;

    private EditText mEtLight;
    private Button mLightOpen;
    private Button mLightClose;

    private RecyclerView recyclerView;


    private List<MotorRequestInfo> requestMotors;

    private StringBuilder mLogBuilder;

    private List<LogInfo> logInfos;
    private LogAdapter adapter;

    private Handler mLogHangler;
    private SharedPreferences sharedPreferences;

    private boolean isCyclerRun = false;
    private Disposable mCyclerRunDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 200);
            } else {
                initLocalLog();
            }
        } else {
            initLocalLog();
        }

        initView();
        initData();
    }

    private void initLocalLog() {
        File dir = new File(Environment
                .getExternalStorageDirectory() + File.separator + "Test" + File.separator + "log");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH-mm");
        final File logFile = new File(dir, formater.format(new Date()) + ".txt");
        HandlerThread handlerThread = new HandlerThread("log");
        handlerThread.start();
        mLogHangler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (null != logFile) {
                    String message = msg.obj.toString();
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter(logFile, true), 1024);
                        writer.write(message);
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e) {

                            }
                        }
                    }
                }
            }
        };
    }

    private void initView() {
        tvId = findViewById(R.id.tv_id);
        tvTemp = findViewById(R.id.tv_temp);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mEtSingle = findViewById(R.id.et_single);
        mBtnRun = findViewById(R.id.btn_run_monitor_single);
        requestMotors = new ArrayList<>();
        logInfos = new ArrayList<>();
        mBtnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mEtSingle.getText())) {
                    ToastUtils.show("请输入电机号");
                    return;
                }
                hideInput();
                int id = Integer.valueOf(mEtSingle.getText().toString());
                requestMotors.clear();
                requestMotors.add(new MotorRequestInfo(0, id, 1));
                MainActivity.this.startMotors(false);
            }
        });

        mEtMulti = findViewById(R.id.et_multi);
        mBtnRunMulti = findViewById(R.id.btn_run_monitor_multi);
        mBtnRunMulti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mEtMulti.getText())) {
                    ToastUtils.show("请输入货道号");
                    return;
                }
                try {
                    hideInput();
                    String[] s = mEtMulti.getText().toString().split(":");
                    if (s == null) {
                        ToastUtils.show("输入格式不正确");
                        return;
                    }
                    requestMotors.clear();
                    for (int i = 0; i < s.length; i++) {
                        MotorRequestInfo info;
                        if (s[i].contains(",")) {
                            String[] arr = s[i].split(",");
                            info = new MotorRequestInfo(1, Integer.valueOf(arr[0]), Integer.valueOf(arr[1]));
                        } else {
                            info = new MotorRequestInfo(0, Integer.valueOf(s[i]), 1);
                        }
                        requestMotors.add(info);
                    }
                    mEtMulti.setText("");
                    MainActivity.this.startMotors(false);
                } catch (Exception e) {
                    ToastUtils.show("输入格式不正确");
                }
            }
        });

        mEtStart = findViewById(R.id.et_start);
        mEtEnd = findViewById(R.id.et_end);
        mSwitchButton = findViewById(R.id.switch_button);
        mBtnRunBetween = findViewById(R.id.btn_run_monitor_between);
        mBtnRunBetween.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMotors.clear();
                if (TextUtils.isEmpty(mEtStart.getText()) || TextUtils.isEmpty(mEtEnd.getText())) {
                    ToastUtils.show("请输入起始机号以及结束机号");
                    return;
                }
                try {
                    hideInput();
                    int start = Integer.valueOf(mEtStart.getText().toString());
                    int end = Integer.valueOf(mEtEnd.getText().toString());

                    for (int i = start; i <= end; i++) {
                        MotorRequestInfo info = new MotorRequestInfo(0, i, 1);
                        requestMotors.add(info);
                    }
                    MainActivity.this.startMotors(true);
                } catch (Exception e) {
                    ToastUtils.show("输入格式不正确");
                }
            }
        });
        mSwitchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isCyclerRun = true;
                    addLog("开启连续循环出货", 0);
                    ToastUtils.show("开启连续循环出货");
                } else {
                    isCyclerRun = false;
                    if (mCyclerRunDisposable != null && !mCyclerRunDisposable.isDisposed()) {
                        mCyclerRunDisposable.dispose();
                    }
                    addLog("关闭连续循环出货", 0);
                    ToastUtils.show("关闭连续循环出货");
                }
            }
        });

        mEtTemp = findViewById(R.id.et_temp);
        mEtShidu = findViewById(R.id.et_shidu);
        mBtnRunTemp = findViewById(R.id.btn_run_monitor_temp);
        mBtnRunTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mEtTemp.getText()) || TextUtils.isEmpty(mEtShidu.getText())) {
                    ToastUtils.show("请输入温度以及湿度");
                    return;
                }
                hideInput();
                int temp = Integer.valueOf(mEtTemp.getText().toString());
                int shidu = Integer.valueOf(mEtShidu.getText().toString());
                tempControl(temp, shidu);
            }
        });

        mEtLight = findViewById(R.id.et_light);
        mLightOpen = findViewById(R.id.btn_open_light);
        mLightClose = findViewById(R.id.btn_close_light);
        mLightOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mEtLight.getText())) {
                    ToastUtils.show("请输入开灯机号");
                    return;
                }
                hideInput();
                int id = Integer.valueOf(mEtLight.getText().toString());
                openLight(id);
            }
        });

        mLightClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeAllLight();
            }
        });


        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);


        mLogBuilder = new StringBuilder();

        mDrugSellSerialHelper = DrugSellSerialHelper.getInstance();
        mDrugSellSerialHelper.connectSerialPortAndGetId(new SerialInitCallback() {
            @Override
            public void onConnectSuccess(String id) {
                tvId.setTextColor(Color.parseColor("#ff333333"));
                tvId.setText(id);
                getTemp();
            }

            @Override
            public void onConnectError(int errCode, String errMsg) {
                tvId.setTextColor(Color.parseColor("#ffFF0000"));
                tvId.setText(errMsg);
                SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String s = formater.format(new Date()) + "    " + errMsg;
                mLogHangler.sendMessage(mLogHangler.obtainMessage(0, s));
            }

            @Override
            public void onPollCommandResponse(CheckStatusInfo checkStatusInfo) {
                tvTemp.setText(String.format("温度：%d℃  湿度：%d", checkStatusInfo.getCurrentTemp(), checkStatusInfo.getCurrentHumidity()));
                addLog(tvTemp.getText().toString(), 0);
            }
        });
        mDrugSellSerialHelper.setOnGlobalErrorListener(new OnGlobalErrorListener() {
            @Override
            public void onError(int type, String errorMsg) {
                addLog("错误码：" + type + "   " + errorMsg, 1);
            }
        });

        btnClear = findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logInfos.clear();
                adapter.notifyDataSetChanged();
            }
        });
        requestMotors = new ArrayList<>();
    }

    private void getTemp() {
        mDrugSellSerialHelper.timingGetTemp(new TempGetCallBack() {
            @Override
            public void onSuccess(int temp, int humidity) {
                tvTemp.setText(String.format("温度：%d℃  湿度：%d", temp, humidity));
                addLog(tvTemp.getText().toString(), 0);
            }

            @Override
            public void onError(int errCode, String errMsg) {
                addLog(errMsg, 1);
            }
        });
    }

    private void closeAllLight() {
        this.mDrugSellSerialHelper.closeAllLight(new LightControlCallback() {
            @Override
            public void onsuccess() {
                addLog("关闭补货灯成功", 0);
            }

            @Override
            public void onError(int errCode, String errMsg) {
                addLog("关闭补货灯失败  " + errMsg, 1);
            }
        });
    }

    private void openLight(final int id) {
        this.mDrugSellSerialHelper.openLight(id, new LightControlCallback() {
            @Override
            public void onsuccess() {
                addLog(id + "   打开补货灯成功", 0);
            }

            @Override
            public void onError(int errCode, String errMsg) {
                addLog(id + "  打开补货灯失败  " + errMsg, 1);
            }
        });
    }

    private void startMotors(final boolean isCycyler) {
        this.mDrugSellSerialHelper.satrtMotor(requestMotors, new DrugSellSerialCallback() {
            @Override
            public void onGoodDropSuccess(MotorRequestInfo info, int index) {
                addLog("第" + index + "个货道掉货成功", 0);
            }

            @Override
            public void onDeliverError(List<MotorRequestInfo> list, MotorRequestInfo info, int index, int errCode, String errMsg) {
                addLog("第" + index + "个货道掉货光眼未检查到物体", 1);
            }

            @Override
            public void onProcessEnd() {
                addLog("本次出货流程结束", 0);
                if (isCycyler && isCyclerRun) {
                    startCycler();
                }
            }

            @Override
            public void onOtherError(int errCode, String errMsg) {
                addLog(errMsg, 1);
            }

            @Override
            public void onConnectError() {

            }
        });
    }

    private void startCycler() {
        mCyclerRunDisposable = Observable.timer(2 * 60 * 1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        addLog("自动循环出货开始！", 0);
                        startMotors(true);
                    }
                });
    }

    private void tempControl(int temp, int shidu) {
        this.mDrugSellSerialHelper.tempControl(temp, shidu, new TempControlCallBack() {
            @Override
            public void onSuccess() {
                addLog("温湿度设置成功", 0);
            }

            @Override
            public void onError(int errCode, String errMsg) {
                addLog(errCode + errMsg, 1);
            }
        });
    }


    /**
     * 打印日志
     */
    private void initData() {
        sharedPreferences = getSharedPreferences("demo", Context.MODE_PRIVATE);
        String boardId = sharedPreferences.getString("boardId", DeviceKey.BOARD_ADDRESS);
        DeviceKey.BOARD_ADDRESS = boardId;
        SerialPortManager.getInstance().setLoggerProvider(new SerialPortManager.LoggerProvider() {
            @Override
            public void w(String tag, String msg) {
                Log.e(tag, msg);
                addLog(msg, 0);
            }

            @Override
            public void e(String tag, String msg) {
                Log.e(tag, msg);
                addLog(msg, 1);
            }

            @Override
            public void d(String tag, String msg) {
                Log.e(tag, msg);
                addLog(msg, 0);
            }
        });


    }


    int count = 0;

    private void addLog(String log, int type) {
        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String s = formater.format(new Date()) + "    " + log;
        mLogHangler.sendMessage(mLogHangler.obtainMessage(0, s));
        if (logInfos.size() > 10000) {
            logInfos.clear();
        }
        logInfos.add(new LogInfo(log, type));
        adapter.notifyDataSetChanged();
        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
    }

    protected void hideInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View v = getWindow().peekDecorView();
        if (null != v) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_zhuban:
                new MaterialDialog.Builder(MainActivity.this)
                        .input("请输入扩展板地址，例如“00”“04”", DeviceKey.BOARD_ADDRESS, false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                sharedPreferences.edit().putString("boardId", input.toString());
                                DeviceKey.BOARD_ADDRESS = input.toString();
                            }
                        }).show();
                break;
            case R.id.menu_machine_light:
                machineLightControl(1);
                break;
            case R.id.menu_machine_light_close:
                machineLightControl(0);
                break;
            case R.id.menu_light_box:
                lightBoxControl(1);
                break;
            case R.id.menu_light_box_close:
                lightBoxControl(0);
                break;
            case R.id.menu_open_gate:
                gateControl(1);
                break;
            case R.id.menu_close_gate:
                gateControl(0);
                break;
            case R.id.menu_reset:
                reset();
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    private void gateControl(final int type) {
        this.mDrugSellSerialHelper.gateControl(type, new GateControlCallback() {
            @Override
            public void onsuccess() {
                addLog("电动门" + (type == 0 ? "关闭成功" : "打开成功"), 0);
            }

            @Override
            public void onError(int errCode, String errMsg) {
                addLog(errMsg, 1);
            }
        });
    }


    private void lightBoxControl(final int type) {
        this.mDrugSellSerialHelper.lightBoxControl(type, new LightBoxControlCallback() {
            @Override
            public void onsuccess() {
                addLog("灯箱控制" + (type == 0 ? "关闭成功" : "打开成功"), 0);
            }

            @Override
            public void onError(int errCode, String errMsg) {
                addLog(errMsg, 1);
            }
        });
    }

    private void machineLightControl(final int type) {
        this.mDrugSellSerialHelper.machineLightControl(type, new MachineLightCallback() {
            @Override
            public void onsuccess() {
                addLog("药柜内照明灯" + (type == 0 ? "关闭成功" : "打开成功"), 0);
            }

            @Override
            public void onError(int errCode, String errMsg) {
                addLog(errMsg, 1);
            }
        });
    }

    private void reset() {
        final RealSerialPort realSerialPort = new RealSerialPort(Device.getSearchInfoDevice().getPort(), Device.getSearchInfoDevice().getDaud(), new ICommandParse() {
            @Override
            public void parseCommand(String hexStr) {
                addLog("接收指令："+hexStr,0);
            }

            @Override
            public void onError(int errCode, String errorMsg) {

            }

            @Override
            public void onTimeOut(String hexString) {

            }

            @Override
            public void onConnectFail() {

            }
        }, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                addLog("连接成功",0);
            }

            @Override
            public void onConnectError() {
                addLog("连接失败",0);
            }
        });
        Observable.timer(2000,TimeUnit.MILLISECONDS)
                .map(new Function<Long, Long>() {
                    @Override
                    public Long apply(Long aLong) throws Exception {
                        realSerialPort.sendCommand(Constant.FULL_ACK_CONFIRM);
                        return aLong;
                    }
                }).delay(2000,TimeUnit.MILLISECONDS)
                .map(new Function<Long, Long>() {
                    @Override
                    public Long apply(Long aLong) throws Exception {
                        realSerialPort.sendCommand(Constant.getCommand(Constant.START_MOTOR+ ByteUtil.decimal2fitHex(1, 4)));
                        return aLong;
                    }
                }).delay(2000,TimeUnit.MILLISECONDS)
                .map(new Function<Long, Long>() {
                    @Override
                    public Long apply(Long aLong) throws Exception {
                        realSerialPort.sendCommand(Constant.FULL_ACK_CONFIRM);
                        return aLong;
                    }
                }).delay(2000,TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        addLog("重置结束", 0);
                        realSerialPort.close();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        addLog("重置错误！"+throwable.getMessage(), 1);
                        realSerialPort.close();
                    }
                });
    }


    class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, null);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LogInfo info = logInfos.get(position);
            if (info.getType() == 1) {
                holder.textView.setTextColor(Color.RED);
            } else {
                holder.textView.setTextColor(Color.parseColor("#ff555555"));
            }
            holder.textView.setText(info.getMsg());
        }

        @Override
        public int getItemCount() {
            return logInfos.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.text);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 200) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocalLog();
            }
        }
    }

    @Override
    protected void onDestroy() {
        mLogHangler.getLooper().quit();
        super.onDestroy();
    }
}
