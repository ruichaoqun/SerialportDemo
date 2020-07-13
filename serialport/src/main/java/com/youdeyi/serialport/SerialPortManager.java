package com.youdeyi.serialport;

import android.util.Log;

import com.youdeyi.serialport.data.ICommandParse;
import com.youdeyi.serialport.data.MotorRequestInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Administrator on 2017/3/28 0028.
 */
public class SerialPortManager {
    private static final String TAG = SerialPortManager.class.getSimpleName();

    private volatile static SerialPortManager instance;

    //串口
    private RealSerialPort mRealSerialPort;


    private SerialPortManager() {
        mRequestInfos = new ArrayList<>();
        mLoggerProvider = new LoggerProvider() {
            @Override
            public void w(String tag, String msg) {
                Log.w(tag, msg);
            }

            @Override
            public void e(String tag, String msg) {
                Log.w(tag, msg);
            }

            @Override
            public void d(String tag, String msg) {
                Log.w(tag, msg);
            }
        };
    }

    //取货指令集合
    private List<MotorRequestInfo> mRequestInfos;
    private int requestCount = 0;
    //外部获取详细日志的接口
    private LoggerProvider mLoggerProvider;

    public static SerialPortManager getInstance() {
        if (instance == null) {
            synchronized (SerialPortManager.class) {
                if (instance == null) {
                    instance = new SerialPortManager();
                }
            }
        }
        return instance;
    }

    /**
     * 获取指定的串口，如果不存在则新建，如果已存在则重新设置回调方法
     *
     * @param path           串口id
     * @param baudrateString 串口波特率
     * @param iCommandParse  回调接口
     * @return 串口
     */
    public void connectSerialPort(String path, String baudrateString, ICommandParse iCommandParse, RealSerialPort.ConnectCallback connectCallback) {
        if(mRealSerialPort != null){
            mRealSerialPort.close();
        }
        mRealSerialPort = new RealSerialPort(path, baudrateString, iCommandParse, connectCallback);
    }

    //根据CRC校验工具获取校验码再与指令拼接成完整指令
    private String getCommand(String str) {
        return str + CRCUtils.getCRC(str);
    }

    /**
     * 发送指令
     */
    private void sendCommand(String command) {
        mRealSerialPort.sendCommand(command);
    }

    public void closeSerialPort(){
        this.mRealSerialPort.close();
    }


    /**
     * 开启轮询指令，该指令轮询查询出货指令的执行情况
     * 当确认出货已经成功且检测到货物掉落后将执行ACK指令来清空数据
     * ACK指令执行成功后才可以执行下一个出货指令
     *
     */
    public void checkStatus() {
        sendCommand(Constant.FULL_POLL_STSTUS);
    }

    /**
     * 发送ack确认指令
     */
    public void sendAckCommand() {
        sendCommand(Constant.FULL_ACK_CONFIRM);
    }

    /**
     * 开出货门
     */
    public void openDoor(){
        sendCommand(Constant.COMMAND_OPEN_DOOR);
    }

    /**
     * 关出货门
     */
    public void closeDoor(){
        sendCommand(Constant.COMMAND_CLOSE_DOOR);
    }

    //开启传送带
    public void startConveyor(){
        sendCommand(Constant.COMMAND_START_CONVEYOR);
    }

    //关闭传送带
    public void closeConveyor(){
        sendCommand(Constant.COMMAND_CLOSE_CONVEYOR);
    }

    public void startMotor(final MotorRequestInfo requestInfo, long time) {
        Observable.timer(time, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        String command;
                        if(requestInfo.getType() == 0){
                            command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.START_MOTOR+ ByteUtil.decimal2fitHex(requestInfo.getId(), 4));
                        }else{
                            command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.START_MULTIPLE_MOTOR + ByteUtil.decimal2fitHex(requestInfo.getId(), 4) + ByteUtil.decimal2fitHex(requestInfo.getCount(), 2));
                        }
                        sendCommand(command);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }

    /**
     * 启动单个电机
     */
    private void startMotor(final Callback callback) {
        if (mRequestInfos.size() == 0) {
            mLoggerProvider.e(TAG, "全部出货完毕");
            callback.onSucccess("全部出货完毕");
            return;
        }
        MotorRequestInfo requestInfo = mRequestInfos.get(0);
        String command;
        if(requestInfo.getCount() == 1){
            command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.START_MOTOR+ ByteUtil.decimal2fitHex(requestInfo.getId(), 4));
        }else{
            command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.START_MULTIPLE_MOTOR + ByteUtil.decimal2fitHex(requestInfo.getId(), 4) + ByteUtil.decimal2fitHex(requestInfo.getCount(), 2));
        }
        sendCommand(command);
    }

    private void reset() {
        mRequestInfos.clear();
        requestCount = 0;
    }


    //********************************暴露给外部的API****************************//

    /**
     * 连接所有串口
     * 该方法在application中调用进行初始化
     */
    public void openAllSerial() {

    }

    public void setLoggerProvider(LoggerProvider loggerProvider) {
        mLoggerProvider = loggerProvider;
    }

    public LoggerProvider getLoggerProvider() {
        return mLoggerProvider;
    }

    /**
     * 检查主板状态
     */
    public void checkConnect() {
        String command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.SEARCH_STATE);
        sendCommand(command);
    }

    /**
     * 启动电机，不论单个、多个还是单路、多路都是走这个方法去启动电机
     *
     * @param list     电机列表
     * @param callback 出货回调
     */
    public void startMultiMotor(List<MotorRequestInfo> list, final Callback callback) {
        mRequestInfos.addAll(list);
        requestCount = mRequestInfos.size();
        startMotor(callback);
    }

    /**
     * 温度湿度控制指令
     *
     * @param tempreture 设置温度值
     * @param humidity   设置湿度值
     */
    public void tempControl( int tempreture, int humidity) {
        String command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.SET_TEMPRETOR  + ByteUtil.decimal2fitHex(tempreture, 2) + ByteUtil.decimal2fitHex(humidity, 2));
        sendCommand(command);
    }

    public void openLight(int id) {
        String command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.LIGHT_OPEN  + ByteUtil.decimal2fitHex(id, 4));
        sendCommand(command);
    }

    public void closeAllLight(){
        String command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.LIGHT_CLOSE);
        sendCommand(command);
    }

    /**
     * 制冷控制
     * @param type 01:制冷开  00：制冷关
     */
    public void refrigerationControl( int type) {
        String command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.REFRIGERATION_CONTROL  + ByteUtil.decimal2fitHex(type, 2));
        sendCommand(command);
    }

    /**
     * 药柜内照明灯控制
     * @param type 01:开  00：关
     */
    public void machineLightControl( int type) {
        String command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.MACHINE_LIGHT  + ByteUtil.decimal2fitHex(type, 2));
        sendCommand(command);
    }


    /**
     * 灯箱控制
     * @param type 01:开  00：关
     */
    public void lightBoxControl( int type) {
        String command = getCommand(DeviceKey.BOARD_ADDRESS + Constant.LIGHT_BOX_CONTROL  + ByteUtil.decimal2fitHex(type, 2));
        sendCommand(command);
    }





    /**
     * 日志打印接口
     */
    public interface LoggerProvider {
        void w(String tag, String msg);

        void e(String tag, String msg);

        void d(String tag, String msg);
    }
}
