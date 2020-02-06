package org.lndroid.framework;

public interface IResponseCallback<Response> {
    void onResponse(Response r);
    void onError(String code, String e);
}
