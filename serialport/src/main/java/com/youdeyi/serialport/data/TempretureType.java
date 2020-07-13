package com.youdeyi.serialport.data;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Rui Chaoqun
 * @date :2019/11/9 10:21
 * description:温度控制类型
 */
@StringDef(value = {TempretureType.NORMAL,
        TempretureType.COLD,
        TempretureType.HEAT})
@Retention(RetentionPolicy.SOURCE)
public @interface TempretureType {

    //常温
    public static final String  NORMAL = "00";

    //制冷
    public static final String  COLD = "01";

    //加热
    public static final String  HEAT = "02";
}
