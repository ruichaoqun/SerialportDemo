package com.youdeyi.serialport.callback;

/**
 * @author Rui Chaoqun
 * @date :2020/5/6 11:54
 * description:
 */
public interface LightBoxControlCallback {
    void onsuccess();
    void onError(int errCode, String errMsg);
}
