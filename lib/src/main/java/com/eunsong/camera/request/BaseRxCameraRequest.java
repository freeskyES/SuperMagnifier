package com.eunsong.camera.request;

import com.eunsong.camera.RxCamera;
import com.eunsong.camera.RxCameraData;

import rx.Observable;

/**
 * Created by ragnarok on 15/11/15.
 */
public abstract class BaseRxCameraRequest {

    protected RxCamera rxCamera;

    public BaseRxCameraRequest(RxCamera rxCamera) {
        this.rxCamera = rxCamera;
    }

    public abstract Observable<RxCameraData> get();
}
