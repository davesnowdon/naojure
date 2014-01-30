package com.davesnowdon.naojure;

import clojure.lang.IFn;

public class InvokeWrapper {
    private final IFn ifn;

    private final String name;

    private final Object userData;

    public InvokeWrapper(String callbackName, IFn callback,
                         Object data) {
        ifn = callback;
        name = callbackName;
        userData = data;
    }

    public void invoke(Object o) {
        ifn.invoke(name, o, userData);
    }
}
