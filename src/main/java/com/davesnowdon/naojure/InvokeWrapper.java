package com.davesnowdon.naojure;

import clojure.lang.IFn;

public class InvokeWrapper {
    private final IFn ifn;

    private final String name;

    public InvokeWrapper(String callbackName, IFn callback) {
        ifn = callback;
        name = callbackName;
    }

    public void invoke(Object o) {
        ifn.invoke(name, o);
    }
}
