package com.youdeyi.serialport;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Rui Chaoqun
 * @date :2020/3/13 14:55
 * description:
 */
@Retention(value = RetentionPolicy.SOURCE)
@IntDef(value = {
        SerialState.NONE,
        SerialState.INIT,
        SerialState.SHIPEMENT_INIT,
        SerialState.SHIPEMENT_CARGO,
        SerialState.SHIPEMENT_UNDER_WAY,
        SerialState.TEMP,
        SerialState.CLOSE_LIGHT,
        SerialState.OPEN_LIGHT,
        SerialState.GET_TEMP,
        SerialState.REFRIGERATION_CONTROL,
})
public @interface SerialState {
    /**
     * 初始状态
     */
    int NONE = -1;

    /**
     * 初始化过程
     */
    int INIT = 0;

    /**
     * 出货-起始状态检测
     */
    int SHIPEMENT_INIT = 1;

    /**
     * 出货-掉货过程中
     */
    int SHIPEMENT_UNDER_WAY= 2;

    /**
     * 出货-取走货物中
     */
    int SHIPEMENT_CARGO = 3;

    /**
     * 设置温度
     */
    int TEMP = 4;

    /**
     *关闭补货灯
     */
    int CLOSE_LIGHT = 5;

    /**
     *打开补货灯
     */
    int OPEN_LIGHT = 6;

    /**
     * 获取温度
     */
    int GET_TEMP = 7;

    /**
     * 制冷控制
     */
    int REFRIGERATION_CONTROL = 8;

    /**
     * 药柜内照明灯
     */
    int MACHINE_LIGHT = 9;

    /**
     * 灯箱控制
     */
    int LIGHT_BOX_CONTROL = 10;

    /**
     * 电动门控制
     */
    int GATE_CONTROL = 11;

}
