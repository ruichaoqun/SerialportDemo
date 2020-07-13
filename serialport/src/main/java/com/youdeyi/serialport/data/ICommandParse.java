package com.youdeyi.serialport.data;

import android.text.TextUtils;

import com.youdeyi.serialport.CRCUtils;
import com.youdeyi.serialport.RealSerialPort;

/**
 * @author Rui Chaoqun
 * @date :2019/10/23 10:28
 * description:返回指令解析基类
 */
public abstract class ICommandParse {
    protected RealSerialPort mRealSerialPort;
    private String preCommand = "";

    public ICommandParse() {
    }

    public void setRealSerialPort(RealSerialPort realSerialPort) {
        this.mRealSerialPort = realSerialPort;
    }

    /**
     * 此处验证所有返回指令，使用CRC校验，如果出现分包，自动合并之后再进行校验
     * @param hexStr 串口返回指令
     */
    public boolean verifyCommand(String hexStr){
        preCommand = preCommand + hexStr;
        String command = preCommand.substring(0,preCommand.length()-4);
        String crc = preCommand.substring(preCommand.length()-4);
        //验证成功
        if(TextUtils.equals(CRCUtils.getCRC(command),crc)){
            //过滤器
            if(!isFilterCommand(hexStr)){
                parseCommand(preCommand);
            }
            preCommand = "";
            return true;
        }
        return false;
    }


    /**
     * 指令过滤器  某些指令即使CRC校验成功，也并不是正确的返回指令，在此过滤
     * @param hexStr 串口返回指令
     * @return 是否需要过滤
     */
    public boolean isFilterCommand(String hexStr){
        return false;
    }

    /**
     *
     * @param hexStr 16进制字符串
     */
    public abstract void parseCommand(String hexStr);

    /**
     * 所有的报错都会回调该方法
     * @param errCode  错误码
     * @param errorMsg 错误详情
     */
    public abstract void onError(int errCode,String errorMsg);

    /**
     * 指令读取超时
     * @param hexString 指令
     */
    public abstract void onTimeOut(String hexString);

    public abstract void onConnectFail();

}
