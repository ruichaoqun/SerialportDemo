package com.youdeyi.serialport;

import androidx.annotation.NonNull;

import com.youdeyi.serialport.callback.DrugSellSerialCallback;
import com.youdeyi.serialport.callback.GateControlCallback;
import com.youdeyi.serialport.callback.LightBoxControlCallback;
import com.youdeyi.serialport.callback.LightControlCallback;
import com.youdeyi.serialport.callback.MachineLightCallback;
import com.youdeyi.serialport.callback.OnGlobalErrorListener;
import com.youdeyi.serialport.callback.RefrigerationControlCallback;
import com.youdeyi.serialport.callback.SerialInitCallback;
import com.youdeyi.serialport.callback.TempControlCallBack;
import com.youdeyi.serialport.callback.TempGetCallBack;
import com.youdeyi.serialport.data.CheckStatusInfo;
import com.youdeyi.serialport.data.ICommandParse;
import com.youdeyi.serialport.data.MotorRequestInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.youdeyi.serialport.Constant.ELECTRONIC_GATE;
import static com.youdeyi.serialport.Constant.LIGHT_BOX_CONTROL;
import static com.youdeyi.serialport.Constant.LIGHT_CLOSE;
import static com.youdeyi.serialport.Constant.LIGHT_OPEN;
import static com.youdeyi.serialport.Constant.MACHINE_LIGHT;
import static com.youdeyi.serialport.Constant.REFRIGERATION_CONTROL;
import static com.youdeyi.serialport.Constant.SET_TEMPRETOR;
import static com.youdeyi.serialport.ErrorCode.ERROR_READ_COMMAND_TIMEOUT;

/**
 * @author Rui Chaoqun
 * @date :2020/1/17 9:08
 * description:串口辅助类，单例模式
 */
public class DrugSellSerialHelper {
    private static final int maxPollCommandExceptionTimes = 10;

    public static final String TAG = "DrugSellSerial";
    private static DrugSellSerialHelper instance;
    private SerialPortManager mSerialPortManager;

    private DrugSellSerialCallback mSellCallback;
    private SerialInitCallback mInitCallback;
    private LightControlCallback mLightControlCallback;
    private TempControlCallBack mTempControlCallBack;
    private TempGetCallBack mTempGetCallBack;
    private OnGlobalErrorListener mOnGlobalErrorListener;
    private RefrigerationControlCallback mRefrigerationControlCallback;
    private MachineLightCallback mMachineLightCallback;
    private GateControlCallback mGateControlCallback;
    private LightBoxControlCallback mLightBoxControlCallback;

    private @SerialState
    int mState = SerialState.NONE;
    private CompositeDisposable mCompositeDisposable;

    //需要出货列表
    private List<MotorRequestInfo> mMotorRequestInfoList;
    //当前指定的需要出货的position
    private int currentPosition;

    //轮询指令异常重试次数
    private int pollCommandExceptionTimes = 0;

    private Device mDevice;

    private CommonParseImpl mCommonParse;


    public static DrugSellSerialHelper getInstance() {
        if (instance == null) {
            synchronized (DrugSellSerialHelper.class) {
                if (instance == null) {
                    instance = new DrugSellSerialHelper();
                }
            }
        }
        return instance;
    }

    private DrugSellSerialHelper() {
        mSerialPortManager = SerialPortManager.getInstance();
        mMotorRequestInfoList = new ArrayList<>();
        mCompositeDisposable = new CompositeDisposable();
        mDevice = Device.getSearchInfoDevice();
        mCommonParse = new CommonParseImpl();
    }

    /**
     * 设置串口，不设置默认 Device.getSearchInfoDevice()
     *
     * @param device
     */
    public void setDevice(Device device) {
        mDevice = device;
    }

    /**
     * 连接串口并获取设备信息
     *
     * @param initCallback 回调
     */
    public void connectSerialPortAndGetId(SerialInitCallback initCallback) {
        this.mInitCallback = initCallback;
        this.mState = SerialState.INIT;
        mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                mSerialPortManager.checkConnect();
            }

            @Override
            public void onConnectError() {
                mInitCallback.onConnectError(ErrorCode.ERROR_SERIALPORT_OPEN.getErrCode(), ErrorCode.ERROR_SERIALPORT_OPEN.getMsg());
                mState = SerialState.NONE;
            }
        });
    }

    //开始轮询
    private void startPoll(long milliseconds) {
        mCompositeDisposable.add(Observable.timer(milliseconds, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        mSerialPortManager.checkStatus();
                    }
                }));
    }

    //发送ack指令
    private void sendAck() {
        mSerialPortManager.sendAckCommand();
    }


    /**
     * 出货
     * 出货正常流程--发送轮训指令-发送ack指令-启动电机-轮询-发送ack-全部掉货成功-轮询-货物取走
     *
     * @param info           出货列表
     * @param serialCallback 回调
     */
    public void satrtMotor(@NonNull List<MotorRequestInfo> info, DrugSellSerialCallback serialCallback) {
        if(mState != SerialState.NONE){
            serialCallback.onOtherError(0,"当前正在执行其他任务，请稍候...");
            return;
        }
        this.mSellCallback = serialCallback;
        this.mState = SerialState.SHIPEMENT_INIT;
        mMotorRequestInfoList.clear();
        currentPosition = 0;
        mMotorRequestInfoList.addAll(info);
        mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                startPoll(0);
            }

            @Override
            public void onConnectError() {
                mSellCallback.onConnectError();
                mState = SerialState.NONE;
            }
        });
    }

    /**
     * 出货
     *
     * @param info           单个出货详情
     * @param serialCallback 回调
     */
    public void startMotor(@NonNull MotorRequestInfo info, DrugSellSerialCallback serialCallback) {
        if(mState != SerialState.NONE){
            serialCallback.onOtherError(0,"当前正在执行其他任务，请稍候...");
            return;
        }
        List<MotorRequestInfo> infos = new ArrayList<>();
        infos.add(info);
        satrtMotor(infos, serialCallback);
    }

    /**
     * 开启补货灯
     *
     * @param id       补货灯电机号
     * @param callback 回调
     */
    public void openLight(final int id, LightControlCallback callback) {
        if(mState != SerialState.NONE){
            callback.onError(0,"当前正在执行其他任务，请稍候...");
            return;
        }
        this.mState = SerialState.OPEN_LIGHT;
        this.mLightControlCallback = callback;
        mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                mSerialPortManager.openLight(id);
            }

            @Override
            public void onConnectError() {
                mLightControlCallback.onError(ErrorCode.ERROR_SERIALPORT_OPEN.getErrCode(), ErrorCode.ERROR_SERIALPORT_OPEN.getMsg());
                mState = SerialState.NONE;
            }
        });
    }

    /**
     * 关闭所有补货灯
     */
    public void closeAllLight(LightControlCallback callback) {
        if(mState != SerialState.NONE){
            callback.onError(0,"当前正在执行其他任务，请稍候...");
            return;
        }
        this.mState = SerialState.CLOSE_LIGHT;
        this.mLightControlCallback = callback;
        mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                mSerialPortManager.closeAllLight();
            }

            @Override
            public void onConnectError() {
                mLightControlCallback.onError(ErrorCode.ERROR_SERIALPORT_OPEN.getErrCode(), ErrorCode.ERROR_SERIALPORT_OPEN.getMsg());
                mState = SerialState.NONE;
            }
        });
    }

    /**
     * 温湿度控制
     *
     * @param tempreture 温度 单位℃
     * @param humidity   湿度 0-99
     */
    public void tempControl(final int tempreture, final int humidity, TempControlCallBack callBack) {
        if(mState != SerialState.NONE){
            callBack.onError(0,"当前正在执行其他任务，请稍候...");
            return;
        }
        this.mState = SerialState.TEMP;
        this.mTempControlCallBack = callBack;
        mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                mSerialPortManager.tempControl(tempreture, humidity);
            }

            @Override
            public void onConnectError() {
                mTempControlCallBack.onError(ErrorCode.ERROR_SERIALPORT_OPEN.getErrCode(), ErrorCode.ERROR_SERIALPORT_OPEN.getMsg());
                mState = SerialState.NONE;
            }
        });
    }

    /**
     * 定时获取温湿度
     */
    public void timingGetTemp(final TempGetCallBack mTempGetCallBack) {
        this.mTempGetCallBack = mTempGetCallBack;
        mCompositeDisposable.add(Observable.interval(10 * 60 * 1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (mState == SerialState.NONE) {
                            mState = SerialState.GET_TEMP;
                            mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
                                @Override
                                public void onConnectSuccess() {
                                    startPoll(0);
                                }

                                @Override
                                public void onConnectError() {
                                    mTempGetCallBack.onError(ErrorCode.ERROR_SERIALPORT_OPEN.getErrCode(), ErrorCode.ERROR_SERIALPORT_OPEN.getMsg());
                                    mState = SerialState.NONE;
                                }
                            });

                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                }));
    }

    /**
     * 制冷控制
     * @param type 1：制冷开  0：制冷关
     */
    public void refrigerationControl(final int type,RefrigerationControlCallback callBack){
        if(mState != SerialState.NONE){
            callBack.onOtherError(0,"当前正在执行其他任务，请稍候...");
            return;
        }
        this.mState = SerialState.REFRIGERATION_CONTROL;
        this.mRefrigerationControlCallback = callBack;
        mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                mSerialPortManager.refrigerationControl(type);
            }

            @Override
            public void onConnectError() {
                mRefrigerationControlCallback.onConnectError();
                mState = SerialState.NONE;
            }
        });
    }


    /**
     * 药柜内照明灯控制
     * @param type 1：开  0：关
     * @param callBack 回调
     */
    public void machineLightControl(final int type, MachineLightCallback callBack){
        if(mState != SerialState.NONE){
            callBack.onError(0,"当前正在执行其他任务，请稍候...");
            return;
        }
        this.mState = SerialState.MACHINE_LIGHT;
        this.mMachineLightCallback = callBack;
        mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                mSerialPortManager.machineLightControl(type);
            }

            @Override
            public void onConnectError() {
                mMachineLightCallback.onError(ErrorCode.ERROR_SERIALPORT_OPEN.getErrCode(), ErrorCode.ERROR_SERIALPORT_OPEN.getMsg());
                mState = SerialState.NONE;
            }
        });
    }


    /**
     * 灯箱控制
     * @param type 1：开  0：关
     * @param callBack 回调
     */
    public void lightBoxControl(final int type,LightBoxControlCallback callBack){
        if(mState != SerialState.NONE){
            callBack.onError(0,"当前正在执行其他任务，请稍候...");
            return;
        }
        this.mState = SerialState.LIGHT_BOX_CONTROL;
        this.mLightBoxControlCallback = callBack;
        mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                mSerialPortManager.lightBoxControl(type);
            }

            @Override
            public void onConnectError() {
                mLightBoxControlCallback.onError(ErrorCode.ERROR_SERIALPORT_OPEN.getErrCode(), ErrorCode.ERROR_SERIALPORT_OPEN.getMsg());
                mState = SerialState.NONE;
            }
        });
    }

    /**
     * 电动门控制
     * @param type 1：开  0：关
     * @param callBack 回调
     */
    public void gateControl(final int type, GateControlCallback callBack){
        if(mState != SerialState.NONE){
            callBack.onError(0,"当前正在执行其他任务，请稍候...");
            return;
        }
        this.mState = SerialState.GATE_CONTROL;
        this.mGateControlCallback = callBack;
        mSerialPortManager.connectSerialPort(mDevice.getPort(), mDevice.getDaud(), mCommonParse, new RealSerialPort.ConnectCallback() {
            @Override
            public void onConnectSuccess() {
                if(type == 1){
                    mSerialPortManager.openDoor();
                }else{
                    mSerialPortManager.closeDoor();
                }
            }

            @Override
            public void onConnectError() {
                mGateControlCallback.onError(ErrorCode.ERROR_SERIALPORT_OPEN.getErrCode(), ErrorCode.ERROR_SERIALPORT_OPEN.getMsg());
                mState = SerialState.NONE;
            }
        });
    }

    //开启电动门
    private void openGate() {
        mSerialPortManager.openDoor();
    }

    //关闭电动门
    private void closeGate(int times) {
        mCompositeDisposable.add(Observable.timer(times, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        mSerialPortManager.closeDoor();
                    }
                }));
    }



    public void setOnGlobalErrorListener(OnGlobalErrorListener onGlobalErrorListener) {
        mOnGlobalErrorListener = onGlobalErrorListener;
    }

    private class CommonParseImpl extends ICommandParse {
        @Override
        public void parseCommand(String hexStr) {
            SerialPortManager.getInstance().getLoggerProvider().w(TAG, "接收指令：" + hexStr);
            String type = hexStr.substring(2, 4);
            switch (type) {
                case Constant.SEARCH_STATE:
                    //设备详情返回
                    byte[] bytes = ByteUtil.hexStr2bytes(hexStr.substring(4, hexStr.length() - 4));
                    String id = new String(bytes);
                    mInitCallback.onConnectSuccess(id);
                    //获取设备详情后，需要调用一次轮询以获取温湿度
                    startPoll(0);
                    break;
                case Constant.POLL_STSTUS:
                    //轮询返回
                    checkPollCommandResult(hexStr);
                    break;
                case Constant.START_MOTOR://单路电机
                case Constant.START_MULTIPLE_MOTOR://多路电机
                    int state = Integer.parseInt(hexStr.substring(4, 6), 16);
                    switch (state) {
                        case 0:
                            startPoll(500);
                            break;
                        case 1:
                            mSellCallback.onOtherError(ErrorCode.ERROR_INVALID_INDEX.getErrCode(), ErrorCode.ERROR_INVALID_INDEX.getMsg());
                            break;
                        case 2:
                            mSellCallback.onOtherError(ErrorCode.ERROR_ORTHER_RUNNING.getErrCode(), ErrorCode.ERROR_ORTHER_RUNNING.getMsg());
                            break;
                        case 3:
                            mSellCallback.onOtherError(ErrorCode.ERROR_OTHER_RESULT_NOT_TAKE_OFF.getErrCode(), ErrorCode.ERROR_OTHER_RESULT_NOT_TAKE_OFF.getMsg());
                            break;
                        default:
                    }
                    break;
                case Constant.ACK_CONFIRM://ack确认
                    if (mState == SerialState.SHIPEMENT_INIT) {
                        startPoll(100);
                    }
                    if(mState == SerialState.SHIPEMENT_UNDER_WAY){
                        currentPosition++;
                        if (currentPosition < mMotorRequestInfoList.size()) {
                            //当前可以出货，开始出货
                            mSerialPortManager.startMotor(mMotorRequestInfoList.get(currentPosition),0);
                        } else {
                            currentPosition = 0;
                            mState = SerialState.SHIPEMENT_CARGO;
                            openGate();
//                            mState = SerialState.NONE;
//                            if(mSellCallback != null){
//                                endProcess();
//                                mSellCallback.onProcessEnd();
//                            }
                        }
                    }
                    break;
                case LIGHT_OPEN:
                    //打开补货灯
                    try {
                        int openLightId = Integer.parseInt(hexStr.substring(4, 8), 16);
                        int openLightState = Integer.parseInt(hexStr.substring(8, 10), 16);
                        if (openLightState == 0) {
                            mLightControlCallback.onsuccess();
                        } else {
                            mLightControlCallback.onError(ErrorCode.ERROR_OPEN_LIGHT.getErrCode(), ErrorCode.ERROR_OPEN_LIGHT.getMsg() + openLightState);
                        }
                    } catch (Exception e) {
                        mLightControlCallback.onError(ErrorCode.ERROR_PARSE.getErrCode(), ErrorCode.ERROR_PARSE.getMsg() + hexStr);
                    }
                    mLightControlCallback = null;
                    mSerialPortManager.closeSerialPort();
                    mState = SerialState.NONE;
                    break;
                case LIGHT_CLOSE:
                    //关闭所有补货灯
                    if (mLightControlCallback != null) {
                        mLightControlCallback.onsuccess();
                        mLightControlCallback = null;
                    }
                    mSerialPortManager.closeSerialPort();
                    mState = SerialState.NONE;
                    break;
                case SET_TEMPRETOR:
                    //温度湿度
                    if (mTempControlCallBack != null) {
                        mTempControlCallBack.onSuccess();
                        mTempControlCallBack = null;
                    }
                    mSerialPortManager.closeSerialPort();
                    mState = SerialState.NONE;
                    break;
                case REFRIGERATION_CONTROL:
                    //制冷
                    if (mRefrigerationControlCallback != null) {
                        mRefrigerationControlCallback.onRefrigerationControlSuccess();
                        mRefrigerationControlCallback = null;
                    }
                    mSerialPortManager.closeSerialPort();
                    mState = SerialState.NONE;
                    break;
                case MACHINE_LIGHT:
                    //药柜内照明灯
                    if (mMachineLightCallback != null) {
                        mMachineLightCallback.onsuccess();
                        mMachineLightCallback = null;
                    }
                    mSerialPortManager.closeSerialPort();
                    mState = SerialState.NONE;
                    break;
                case LIGHT_BOX_CONTROL:
                    //灯箱控制
                    if (mLightBoxControlCallback != null) {
                        mLightBoxControlCallback.onsuccess();
                        mLightBoxControlCallback = null;
                    }
                    mSerialPortManager.closeSerialPort();
                    mState = SerialState.NONE;
                    break;
                case ELECTRONIC_GATE:
                    if(mState == SerialState.GATE_CONTROL){
                        if (mGateControlCallback != null) {
                            mGateControlCallback.onsuccess();
                            mGateControlCallback = null;
                        }
                        mSerialPortManager.closeSerialPort();
                        mState = SerialState.NONE;
                    }else{
                        int gateState = Integer.parseInt(hexStr.substring(4, 6), 16);
                        if (gateState == 0) {
                            mSerialPortManager.closeSerialPort();
                            mState = SerialState.NONE;
                            mSellCallback.onProcessEnd();
//                            //关门指令回复
//                            if(mState == SerialState.SHIPEMENT_CARGO){
//                                startPoll(500);
//                            }
                        } else {
                            //开门指令回复,关门
                            closeGate(60_000);
                        }
                    }
                default:
            }
        }

        private void checkPollCommandResult(String hexStr) {
            CheckStatusInfo info = CheckStatusInfo.parseCheckStatusInfo(hexStr);
            if (info == null) {
                onError(ErrorCode.ERROR_PARSE.getErrCode(), ErrorCode.ERROR_PARSE.getMsg() + hexStr);
                endProcess();
                return;
            }
            if (info.getOtherState() == 1) {
                onError(ErrorCode.ERROR_SHIPMENT_MOTOR_FAULT.getErrCode(), ErrorCode.ERROR_SHIPMENT_MOTOR_FAULT.getMsg());
                endProcess();
                return;
            }
            if (info.getOtherState() == 2) {
                onError(ErrorCode.ERROR_CONNECT_FAULT.getErrCode(), ErrorCode.ERROR_CONNECT_FAULT.getMsg());
                endProcess();
                return;
            }
            switch (mState) {
                case SerialState.INIT:
                    mInitCallback.onPollCommandResponse(info);
                    mState = SerialState.NONE;
                    break;
                case SerialState.SHIPEMENT_INIT:
                    switch (info.getState()) {
                        case 0:
                            mState = SerialState.SHIPEMENT_UNDER_WAY;
                            //当前空闲状态，出货
                            if (currentPosition < mMotorRequestInfoList.size()) {
                                //当前可以出货，开始出货
                                mSerialPortManager.startMotor(mMotorRequestInfoList.get(currentPosition),0);
                            }
                            break;
                        case 1:
                            //当前状态出货中，500ms继续轮询查询
                            mSerialPortManager.getLoggerProvider().e(TAG,"出货前检测到轮询指令状态为出货中");
                            pollCommandExceptionTimes++;
                            if (pollCommandExceptionTimes > maxPollCommandExceptionTimes) {
                                pollCommandExceptionTimes = 0;
                                mSerialPortManager.getLoggerProvider().e(TAG,"出货电机一直出货中，退出整个流程");
                                //TODO出货电机一直出货中，退出整个流程，上报异常
                                endProcess();
                            } else {
                                startPoll(500);
                            }
                            break;
                        case 2:
                            //当前状态出货结束
                            mSerialPortManager.getLoggerProvider().e(TAG,"出货前检测到轮询指令状态为出货结束");
                            sendAck();
                            break;
                        default:
                    }
                    break;
                case SerialState.SHIPEMENT_UNDER_WAY:
                    if(info.getState() == 2){
                        if (info.getDropLightEyeState() == 0) {
                            //检测到掉货，发送ack
                            sendAck();
                            mSellCallback.onGoodDropSuccess( mMotorRequestInfoList.get(currentPosition), currentPosition);
                        } else {
                            //虽然掉货结束，但是光眼未检测到货物，此时继续出货并上报
                            int id = mMotorRequestInfoList.get(currentPosition).getId();
                            mSellCallback.onDeliverError(mMotorRequestInfoList, mMotorRequestInfoList.get(currentPosition), currentPosition, ErrorCode.ERROR_DROP_LIGHT_NO_DETECT.getErrCode(), ErrorCode.ERROR_DROP_LIGHT_NO_DETECT.getMsg() + id);
                            sendAck();
                        }
                    }else{
                        startPoll(500);
                    }
                    break;
                case SerialState.SHIPEMENT_CARGO:
                    if(info.isHaveCargo()){
                        //取货口有货
                        startPoll(500);
                    }else{
                        //取货口无货
                        if(info.getDoorState() == 1){
                            //门状态为开，15秒后关闭电动门
                            closeGate(10*1000);
                        }else if(info.getDoorState() == 2){
                            startPoll(500);
                        }else{
                            if(mSellCallback != null){
                                mSellCallback.onProcessEnd();
                            }
                            mSerialPortManager.closeSerialPort();
                        }
                    }
                    break;
                case SerialState.GET_TEMP:
                    if (mTempGetCallBack != null) {
                        mTempGetCallBack.onSuccess(info.getCurrentTemp(), info.getCurrentHumidity());
                    }
                    break;
                default:
            }
        }

        @Override
        public boolean isFilterCommand(String hexStr) {
            return hexStr.startsWith("01");
        }

        @Override
        public void onError(int type, String errorMsg) {
            mState = SerialState.NONE;
            if (mOnGlobalErrorListener != null) {
                mOnGlobalErrorListener.onError(type, errorMsg);
            }
            mSerialPortManager.closeSerialPort();
        }

        @Override
        public void onTimeOut(String hexString) {
            mState = SerialState.NONE;
            if (mOnGlobalErrorListener != null) {
                mOnGlobalErrorListener.onError(ERROR_READ_COMMAND_TIMEOUT.getErrCode(), ERROR_READ_COMMAND_TIMEOUT.getMsg()+hexString);
            }
            mSerialPortManager.closeSerialPort();
        }

        @Override
        public void onConnectFail() {

        }
    }

    /**
     * 结束整个流程
     */
    private void endProcess() {
        mState = SerialState.NONE;
        mMotorRequestInfoList.clear();
        currentPosition = 0;
        mSerialPortManager.closeSerialPort();
    }
}
