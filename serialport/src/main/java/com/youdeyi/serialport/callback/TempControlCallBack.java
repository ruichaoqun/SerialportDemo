package com.youdeyi.serialport.callback;

/**
 * @author Rui Chaoqun
 * @date :2020/3/16 11:36
 * description:
 */
public interface TempControlCallBack {
    void onSuccess();
    void onError(int errCode, String errMsg);
}
