package com.eunsong.camera.error;

/**
 * Created by ragnarok on 15/11/23.
 */
public class CameraDataNullException extends Exception {

    public CameraDataNullException() {
        super("the camera data is null");
    }
}
