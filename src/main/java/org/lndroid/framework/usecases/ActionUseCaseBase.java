package org.lndroid.framework.usecases;

import android.util.Log;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.lang.reflect.Type;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.client.IPluginTransaction;
import org.lndroid.framework.client.IPluginTransactionCallback;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.IPluginData;

import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;

public abstract class ActionUseCaseBase<Request, Response> implements IActionUseCase<Request, Response>, LifecycleObserver {
    private String pluginId_;
    private IPluginClient client_;
    protected String tag_;
    protected IPluginTransaction tx_;

    private LifecycleOwner factoryOwner_;
    private IRequestFactory<Request> factory_;
    private LifecycleOwner cbOwner_;
    private IResponseCallback<Response> cb_;
    private LifecycleOwner authOwner_;
    private IResponseCallback<WalletData.AuthRequest> authCb_;
    private LifecycleOwner authedOwner_;
    private IResponseCallback<WalletData.AuthResponse> authedCb_;

    private Request request_;
    private Response response_;
    private WalletData.AuthRequest authRequest_;
    private boolean authRequestNotified_;
    private WalletData.AuthResponse authResponse_;
    private boolean authResponseNotified_;
    private WalletData.Error error_;

    public ActionUseCaseBase(String pluginId, IPluginClient client, String tag) {
        pluginId_ = pluginId;
        client_ = client;
        tag_ = tag;
    }

    @OnLifecycleEvent(ON_DESTROY)
    public void onOwnerDestroy(LifecycleOwner owner) {
        owner.getLifecycle().removeObserver(this);
        if (owner == factoryOwner_)
            factory_ = null;
        if (owner == cbOwner_)
            cb_ = null;
        if (owner == authOwner_)
            authCb_ = null;
        if (owner == authedOwner_)
            authedCb_ = null;
    }

    @Override
    public void destroy() {
        reset();
    }

    private void reset() {
        if (tx_ != null)
            tx_.destroy();
        tx_ = null;
    }

    protected abstract Response getData(IPluginData in);
    protected abstract Type getRequestType();

    private void onAuthReady(WalletData.AuthRequest r) {
        authRequest_ = r;
        if (authCb_ != null) {
            authCb_.onResponse(r);
            authRequestNotified_ = true;
        }
    }

    private void onAuthedReady(WalletData.AuthResponse r) {
        authResponse_ = r;
        if (authedCb_ != null) {
            authedCb_.onResponse(r);
            authResponseNotified_ = true;
        }
    }

    protected void onResponseReady(Response r) {
        response_ = r;
        reset();
        if (cb_ != null) {
            cb_.onResponse(r);
        }
    }

    private void onErrorReady(String code, String e) {
        reset();
        error_ = WalletData.Error.builder().setCode(code).setMessage(e).build();
        if (cb_ != null) {
            cb_.onError(code, e);
        }
    }

    @Override
    public void setCallback(LifecycleOwner owner, IResponseCallback<Response> cb) {
        cbOwner_ = owner;
        cb_ = cb;
        owner.getLifecycle().addObserver(this);
    }

    @Override
    public void setRequestFactory(LifecycleOwner owner, IRequestFactory<Request> f) {
        factoryOwner_ = owner;
        factory_ = f;
        owner.getLifecycle().addObserver(this);
    }

    @Override
    public void setAuthCallback(LifecycleOwner owner, IResponseCallback<WalletData.AuthRequest> cb) {
        authOwner_ = owner;
        authCb_ = cb;
        owner.getLifecycle().addObserver(this);
    }

    @Override
    public void setAuthedCallback(LifecycleOwner owner, IResponseCallback<WalletData.AuthResponse> cb) {
        authedOwner_ = owner;
        authedCb_ = cb;
        owner.getLifecycle().addObserver(this);
    }

    @Override
    public boolean isExecuting() {
        return tx_ != null;
    }

    @Override
    public Request request() {
        return request_;
    }

    @Override
    public Response response() {
        return response_;
    }

    @Override
    public WalletData.AuthRequest authRequest() {
        return authRequest_;
    }

    @Override
    public WalletData.AuthResponse authResponse() {
        return authResponse_;
    }

    @Override
    public WalletData.Error error() {
        return error_;
    }

    @Override
    public void recover() {
        if (tx_ == null)
            throw new RuntimeException("Use case not executing");

        Log.i(tag_, "already executing");
        if (response_ != null) {
            Log.i(tag_, "result ready");
            onResponseReady(response_);
        } else if (error_ != null) {
            Log.i(tag_, "error ready " + error_);
            onErrorReady(error_.code(), error_.message());
        } else if (authResponse_ != null && !authResponseNotified_) {
            Log.i(tag_, "auth response " + authResponse_);
            onAuthedReady(authResponse_);
        } else if (authRequest_ != null && !authRequestNotified_) {
            Log.i(tag_, "auth request " + authRequest_);
            onAuthReady(authRequest_);
        } else {
            // wait until execute finishes
        }
    }

    @Override
    public void execute(String txId) {
        if (tx_ != null)
            throw new RuntimeException("Use case already executing");
        if (factory_ == null)
            throw new RuntimeException("Request factory not provided");

        // store
        request_ = factory_.create();

        // if factory failed, sitently return as client
        // should have reacted by showing an error message to user
        if (request_ == null)
            return;

        Log.i(tag_, "started");

        // clear results of previous call
        error_ = null;
        response_ = null;
        authRequestNotified_ = false;
        authResponseNotified_ = false;


        tx_ = client_.createTransaction(pluginId_, txId, new IPluginTransactionCallback() {

            @Override
            public void onResponse(IPluginData in) {
                Response r = getData(in);
                Log.i(tag_, "done " + r);
                if (r != null) {
                    onResponseReady(r);
                } else {
                    onErrorReady(Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
                }
            }

            @Override
            public void onAuth(WalletData.AuthRequest r) {
                Log.i(tag_, "auth request " + r);
                onAuthReady(r);
            }

            @Override
            public void onAuthed(WalletData.AuthResponse r) {
                Log.i(tag_, "auth response " + r);
                onAuthedReady(r);
            }

            @Override
            public void onError(String code, String e) {
                Log.i(tag_, "error " + code + " msg " + e);
                onErrorReady(code, e);
            }
        });

        // start
        tx_.start(request_, getRequestType());
    }
}
