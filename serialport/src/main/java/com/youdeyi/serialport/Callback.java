package com.youdeyi.serialport;

/**
 * @author Rui Chaoqun
 * @date :2019/11/8 10:32
 * description:最终回调数据
 */
public interface Callback {
    /**
     * 成功回调
     * @param result 返回数据
     */
    void onSucccess(Object result);

    /**
     * 错误回调
     * @param code  错误码
     * @param errMsg  错误信息
     */
    void onError(int code, String errMsg);
}
