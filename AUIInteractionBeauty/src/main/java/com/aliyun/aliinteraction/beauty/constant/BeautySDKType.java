package com.aliyun.aliinteraction.beauty.constant;

public enum BeautySDKType {
    // should be kept!
    QUEEN(BeautyConstant.BEAUTY_QUEEN_MANAGER_CLASS_NAME, BeautyConstant.BEAUTY_QUEEN_DATA_INJECTOR_CLASS_NAME),
    INTERACT_QUEEN(BeautyConstant.BEAUTY_INTERACT_QUEEN_MANAGER_CLASS_NAME, BeautyConstant.BEAUTY_QUEEN_DATA_INJECTOR_CLASS_NAME);

    private final String managerClassName;
    private final String dataInjectorClassName;

    BeautySDKType(String managerClassName, String dataInjectorClassName) {
        this.managerClassName = managerClassName;
        this.dataInjectorClassName = dataInjectorClassName;
    }

    public String getManagerClassName() {
        return managerClassName;
    }

    public String getDataInjectorClassName() {
        return dataInjectorClassName;
    }
}
