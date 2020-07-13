package com.youdeyi.serialport.callback;

/**
 * @author Rui Chaoqun
 * @date :2020/4/11 10:50
 * description:制冷控制回调
 */
public interface RefrigerationControlCallback {
    /**
     * 串口连接
     */
    void onConnectError();

    /**
     * 其他错误回调
     * @param code
     * @param msg
     */
    void onOtherError(int code, String msg);

    /**
     * 制冷指令回复成功
     */
    void onRefrigerationControlSuccess();
}
