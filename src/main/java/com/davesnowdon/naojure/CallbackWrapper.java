package com.davesnowdon.naojure;

import clojure.lang.IFn;

import com.aldebaran.qimessaging.Callback;
import com.aldebaran.qimessaging.Future;

public class CallbackWrapper implements Callback {

    private final IFn onComplete;
    private final IFn onFailure;
    private final IFn onSuccess;

    public CallbackWrapper(IFn success, IFn failure, IFn complete) {
        this.onComplete = complete;
        this.onFailure = failure;
        this.onSuccess = success;
    }

    public CallbackWrapper(IFn success) {
        this(success, null, null);
    }

    public CallbackWrapper(IFn success, IFn failure) {
        this(success, failure, null);
    }

    @Override
    public void onComplete(Future future, Object[] values) {
        if (null != onComplete) {
            onComplete.invoke(values);
        }
    }

    @Override
    public void onFailure(Future future, Object[] values) {
        if (null != onFailure) {
            onFailure.invoke(values);
        }
    }

    @Override
    public void onSuccess(Future future, Object[] values) {
        if (null != onSuccess) {
            onSuccess.invoke(values);
        }
    }

}
