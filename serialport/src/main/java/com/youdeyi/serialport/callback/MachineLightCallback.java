package com.youdeyi.serialport.callback;

/**
 * @author Rui Chaoqun
 * @date :2020/5/6 11:53
 * description:
 */
public interface MachineLightCallback {
    void onsuccess();
    void onError(int errCode, String errMsg);
}
