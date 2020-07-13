package com.youdeyi.serialport.data;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Rui Chaoqun
 * @date :2019/11/9 10:30
 * description:制冷控制字段
 */


@StringDef(value = {LightControlType.OPEN,
        LightControlType.CLOSE})
@Retention(RetentionPolicy.SOURCE)
public @interface LightControlType {
    /**
     * 关
     */
    String CLOSE = "00";

    /**
     * 开
     */
    String OPEN = "01";
}
