package org.lndroid.framework.common;

public interface IResponseCallback<Response> {
    void onResponse(Response r);
    void onError(String code, String e);
}
