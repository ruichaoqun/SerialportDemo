package com.youdeyi.serialport.callback;

/**
 * @author Rui Chaoqun
 * @date :2020/3/16 11:17
 * description:
 */
public interface LightControlCallback {
    void onsuccess();
    void onError(int errCode, String errMsg);
}
