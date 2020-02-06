package org.lndroid.framework.usecases;

import androidx.lifecycle.LifecycleOwner;

import org.lndroid.framework.IResponseCallback;
import org.lndroid.framework.WalletData;

public interface IActionUseCase<Request, Response> {
    // release plugin client resources
    void destroy();

    void setCallback(LifecycleOwner owner, IResponseCallback<Response> cb);
    void setRequestFactory(LifecycleOwner owner, IRequestFactory<Request> f);

    // callback might be omitted if result is available when 'recover' is called
    void setAuthCallback(LifecycleOwner owner, IResponseCallback<WalletData.AuthRequest> cb);
    // callback might be omitted if result is available when 'recover' is called
    void setAuthedCallback(LifecycleOwner owner, IResponseCallback<WalletData.AuthResponse> cb);

    // txId might be specified if client-side code stores all sent txs
    // and would like to try to recover an interrupted tx on app restart
    void execute(String txId);
    void recover();

    // 'true' when 'execute' was called but Callback wasn't yet executed
    boolean isExecuting();

    Request request();
    Response response();
    WalletData.AuthRequest authRequest();
    WalletData.AuthResponse authResponse();
    WalletData.Error error();
}
