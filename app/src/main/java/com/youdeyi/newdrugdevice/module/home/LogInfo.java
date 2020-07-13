package com.youdeyi.newdrugdevice.module.home;

public class LogInfo {
    private int type;
    private String msg;

    public LogInfo(String msg,int type) {
        this.type = type;
        this.msg = msg;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


}
