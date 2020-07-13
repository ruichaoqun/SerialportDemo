package com.youdeyi.serialport.callback;

import com.youdeyi.serialport.data.CheckStatusInfo;

/**
 * @author Rui Chaoqun
 * @date :2020/3/13 14:21
 * description:串口初始化回调
 */
public interface SerialInitCallback {
    /**
     * 串口连接成功
     * @param id 返回设备id
     */
    void onConnectSuccess(String id);

    /**
     * 串口连接失败
     * @param errCode 错误码
     * @param errMsg 错误信息
     */
    void onConnectError(int errCode, String errMsg);

    /**
     * 串口连接成功返回数据
     * @param checkStatusInfo 包含温湿度
     */
    void onPollCommandResponse(CheckStatusInfo checkStatusInfo);
}
