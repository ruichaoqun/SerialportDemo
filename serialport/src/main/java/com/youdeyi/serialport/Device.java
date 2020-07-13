package com.youdeyi.serialport;

/**
 * @author Rui Chaoqun
 * @date :2019/11/8 11:47
 * description:
 */
public class Device {

    private Device(){

    }

    private Device(String port, String daud) {
        this.port = port;
        this.daud = daud;
    }

    //串口号
    private String port;

    //波特率
    private String daud;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDaud() {
        return daud;
    }

    public void setDaud(String daud) {
        this.daud = daud;
    }



    public static Device getSearchInfoDevice(){
        return new Device(DeviceKey.SEARCH_INFO_PORD, DeviceKey.SEARCH_INFO_DAUD);
    }
}
