package com.youdeyi.serialport;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.serialport.SerialPort;
import android.util.Log;

import com.youdeyi.serialport.data.ICommandParse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * @author Rui Chaoqun
 * @description: 串口操作类
 * @date :2019/5/27 9:29
 */
public class RealSerialPort {
    private static final String TAG = "RealSerialPort";
    public static final String TIMEOUT_COMMAND = "000000001";

    public boolean isConnect = false;
    private String devicePath;
    private String baudrateString;
    //串口设备
    private SerialPort mSerialPort;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
    private byte[] received = new byte[1024];
    private Disposable disposable;
    private ICommandParse mICommandParse;

    private ThreadPoolExecutor mSendCommandExecutor;

    private long timeoutMillis = 3000L;
    private long timeMillis;
    //当前执行指令
    private String mCurrentCommand;
    private ConnectCallback mConnectCallback;

    public RealSerialPort(String devicePath, String baudrateString, ICommandParse iCommandParse, ConnectCallback connectCallback) {
        this.devicePath = devicePath;
        this.baudrateString = baudrateString;
        this.mICommandParse = iCommandParse;
        this.mICommandParse.setRealSerialPort(this);
        this.mConnectCallback = connectCallback;
        open();
    }

    /**
     * 开启串口
     */
    @SuppressLint("CheckResult")
    public void open() {
        if (mSerialPort == null) {
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                    File device = new File(devicePath);
                    int baurate = Integer.parseInt(baudrateString);
                    mSerialPort = new SerialPort(device, baurate, 0);
                    inputStream = new BufferedInputStream(mSerialPort.getInputStream());
                    outputStream = new BufferedOutputStream(mSerialPort.getOutputStream());

                    mSendCommandExecutor = new ThreadPoolExecutor(1, 1,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "send command thread");
                        }
                    });
                    startReceiveCommand();
                    isConnect = true;
                    emitter.onNext(true);
                    emitter.onComplete();
                }
            }).retryWhen(new Function<Observable<Throwable>, ObservableSource<?>>() {
                //连接失败，尝试重连3次
                private int maxRetries = 3;
                private long retryDelayMillis = 1000L;
                private int retryCount = 0;

                @Override
                public ObservableSource<?> apply(Observable<Throwable> throwableObservable) throws Exception {
                    return throwableObservable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                        @Override
                        public ObservableSource<?> apply(Throwable throwable) throws Exception {
                            if (++retryCount <= maxRetries) {
                                return Observable.timer(retryDelayMillis,
                                        TimeUnit.MILLISECONDS);
                            }
                            // Max retries hit. Just pass the error along.
                            return Observable.error(throwable);
                        }
                    });
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<Boolean>() {
                @Override
                public void accept(Boolean aBoolean) throws Exception {
                    isConnect = true;
                    if (mConnectCallback != null) {
                        mConnectCallback.onConnectSuccess();
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    isConnect = false;
                    if (mConnectCallback != null) {
                        mConnectCallback.onConnectError();
                    }
                }
            });
        }
    }

    /**
     * 发送指令
     *
     * @param hexStr 指令
     */
    @SuppressLint("CheckResult")
    public void sendCommand(final String hexStr) {
        SerialPortManager.getInstance().getLoggerProvider().w(TAG, devicePath + "   发送指令：" + hexStr);
        if (!isConnect) {
            if (mConnectCallback != null) {
                mConnectCallback.onConnectError();
            }
            return;
        }
        mSendCommandExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mCurrentCommand = hexStr;
                    byte[] bytes = ByteUtil.hexStr2bytes(hexStr);
                    outputStream.write(bytes);
                    outputStream.flush();
                    timeMillis = SystemClock.uptimeMillis();
                } catch (IOException e) {
                    SerialPortManager.getInstance().getLoggerProvider().e(TAG, devicePath + "   发送指令：" + hexStr + "失败");
                    e.printStackTrace();
                }
            }
        });
    }

    private Observable<String> rxStartReceiveCommand(){
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                boolean flag = true;
                long firstTime = SystemClock.uptimeMillis();
                String preCommand;
                while (flag){
                    //每隔30毫秒睡眠一次
                    Thread.sleep(30);
                    //判断是否已读取超时
                    long times = SystemClock.uptimeMillis();
                    if(times - 1000 > timeoutMillis){
                        //超时，返回超时
//                        mICommandParse.parseCommand(TIMEOUT_COMMAND);
                        flag = true;
                        emitter.onNext(TIMEOUT_COMMAND);
                    }else{
                        int available = inputStream.available();
                        if (available > 0) {
                            int size = inputStream.read(received);
                            if (size > 0) {
                                String hexStr = ByteUtil.bytes2HexStr(received, 0, size);
                                Log.w("AAAAAA","接收指令--》"+hexStr);
                                mICommandParse.verifyCommand(hexStr);
                            }
                        }
                    }
                }
            }
        });
    }


    /**
     * 开启定时器定时读取返回数据
     */
    private void startReceiveCommand() {
        disposable = Observable.interval(30, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        long t = SystemClock.uptimeMillis();
                        if(timeMillis != 0 && t - timeMillis > timeoutMillis){
                            mICommandParse.onTimeOut(mCurrentCommand);
                            timeMillis = 0;
                            return;
                        }
                        int available = inputStream.available();
                        if (available > 0) {
                            int size = inputStream.read(received);
                            if (size > 0) {
                                String hexStr = ByteUtil.bytes2HexStr(received, 0, size);
                                Log.w("AAAAAA","接收指令--》"+hexStr);
                                if(mICommandParse.verifyCommand(hexStr)){
                                    timeMillis = 0;
                                }
                            }
                        }
                    }
                });
    }

    //重定向回调接口
    public void setICommandParse(ICommandParse ICommandParse) {
        this.mICommandParse = ICommandParse;
        this.mICommandParse.setRealSerialPort(this);
    }

    /**
     * 关闭串口
     */
    public synchronized void close() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mSerialPort != null) {
            mSerialPort.close();
        }

        if(mICommandParse != null){
            mICommandParse.setRealSerialPort(null);
            mICommandParse = null;
        }
        inputStream = null;
        outputStream = null;
        mSerialPort = null;
    }

    public interface ConnectCallback {
        void onConnectSuccess();

        void onConnectError();
    }
}
