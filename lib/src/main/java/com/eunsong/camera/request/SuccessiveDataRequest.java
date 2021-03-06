package com.eunsong.camera.request;

import com.eunsong.camera.OnRxCameraPreviewFrameCallback;
import com.eunsong.camera.RxCamera;
import com.eunsong.camera.RxCameraData;
import com.eunsong.camera.error.CameraDataNullException;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;


/**
 * Created by ragnarok on 15/11/13.
 */
public class SuccessiveDataRequest extends BaseRxCameraRequest implements OnRxCameraPreviewFrameCallback {

    private boolean isInstallSuccessivePreviewCallback = false;

    private Subscriber<? super RxCameraData> successiveDataSubscriber = null;

    public SuccessiveDataRequest(RxCamera rxCamera) {
        super(rxCamera);
    }

    public Observable<RxCameraData> get() {

        return Observable.create(new Observable.OnSubscribe<RxCameraData>() {
            @Override
            public void call(final Subscriber<? super RxCameraData> subscriber) {
                successiveDataSubscriber = subscriber;
            }
        }).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                rxCamera.uninstallPreviewCallback(SuccessiveDataRequest.this);
                isInstallSuccessivePreviewCallback = false;
            }
        }).doOnSubscribe(new Action0() {
            @Override
            public void call() {
                if (!isInstallSuccessivePreviewCallback) {
                    rxCamera.installPreviewCallback(SuccessiveDataRequest.this);
                    isInstallSuccessivePreviewCallback = true;
                }
            }
        }).doOnTerminate(new Action0() {
            @Override
            public void call() {
                rxCamera.uninstallPreviewCallback(SuccessiveDataRequest.this);
                isInstallSuccessivePreviewCallback = false;
            }
        });
    }

    @Override
    public void onPreviewFrame(byte[] data) {
        if (successiveDataSubscriber != null && !successiveDataSubscriber.isUnsubscribed() && rxCamera.isOpenCamera()) {
            if (data == null || data.length == 0) {
                successiveDataSubscriber.onError(new CameraDataNullException());
            }
            RxCameraData cameraData = new RxCameraData();
            cameraData.cameraData = data;
            cameraData.rotateMatrix = rxCamera.getRotateMatrix();
            successiveDataSubscriber.onNext(cameraData);
        }
    }
}
