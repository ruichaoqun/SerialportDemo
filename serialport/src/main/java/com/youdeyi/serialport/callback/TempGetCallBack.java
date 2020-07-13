package com.youdeyi.serialport.callback;

/**
 * @author Rui Chaoqun
 * @date :2020/3/16 11:36
 * description:
 */
public interface TempGetCallBack {
    /**
     *
     * @param temp 温度
     * @param humidity 湿度
     */
    void onSuccess(int temp, int humidity);

    void onError(int errCode, String errMsg);
}
