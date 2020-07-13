package com.youdeyi.serialport.callback;

/**
 * @author Rui Chaoqun
 * @date :2020/5/13 15:51
 * description:
 */
public interface GateControlCallback {
    void onsuccess();
    void onError(int errCode, String errMsg);
}
