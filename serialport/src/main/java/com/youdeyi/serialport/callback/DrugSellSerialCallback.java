package com.youdeyi.serialport.callback;

import com.youdeyi.serialport.data.MotorRequestInfo;

import java.util.List;

/**
 * @author Rui Chaoqun
 * @date :2020/3/12 11:01
 * description:
 */
public interface DrugSellSerialCallback {

    /**
     * 货物掉货成功
     * @param info 商品货道信息
     * @param index 在出货列中的位置
     */
    void onGoodDropSuccess(MotorRequestInfo info, int index);

    /**
     * 出货失败回调
     * @param list 指定出货商品列表
     * @param info 发生出货错误的商品
     * @param index 发生出货错误商品在列表中的位置
     * @param errCode 错误码
     * @param errMsg  错误详情
     */
    void onDeliverError(List<MotorRequestInfo> list, MotorRequestInfo info, int index, int errCode, String errMsg);

    /**
     * 所有货物全部出货完成，整个出货流程结束
     */
    void onProcessEnd();


    /**
     * 其他错误回调
     * @param errCode 错误码
     * @param errMsg    错误详情
     */
    void onOtherError(int errCode, String errMsg);

    /**
     * 串口连接失败回调
     */
    void onConnectError();
}
