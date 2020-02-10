package org.lndroid.framework.usecases.rpc;

import android.util.Log;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IAuthClient;
import org.lndroid.framework.usecases.IRequestFactory;

import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;

abstract class AuthRPCUseCaseBase<Request, Response> implements IRPCUseCase<Request, Response>, LifecycleObserver {
    protected IAuthClient client_;
    protected String tag_;

    private LifecycleOwner factoryOwner_;
    private IRequestFactory<Request> factory_;
    private LifecycleOwner cbOwner_;
    private IResponseCallback<Response> cb_;

    private Request request_;
    private Response response_;
    private WalletData.Error error_;
    private boolean executing_;

    AuthRPCUseCaseBase(IAuthClient client, String tag) {
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
    }

    private void reset() {
        executing_ = false;
    }

    protected interface IExecutor<Request, Response> {
        void execute(Request r, IResponseCallback<Response> cb);
    }

    private void onResponseReady(Response r) {
        response_ = r;
        if (cb_ != null) {
            cb_.onResponse(r);
            reset();
        }
    }

    private void onErrorReady(String code, String e) {
        if (cb_ != null) {
            cb_.onError(code, e);
            reset();
        } else {
            error_ = WalletData.Error.builder().setCode(code).setMessage(e).build();
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
    public boolean isExecuting() {
        return executing_;
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
    public WalletData.Error error() {
        return error_;
    }

    @Override
    public void recover() {
        if (!executing_)
            throw new RuntimeException("Use case not executing");

        Log.i(tag_, "already executing");
        if (response_ != null) {
            Log.i(tag_, "result ready");
            onResponseReady(response_);
        } else if (error_ != null) {
            Log.i(tag_, "error ready " + error_);
            onErrorReady(error_.code(), error_.message());
        } else {
            // wait until execute finishes
        }
    }

    protected void execute(IExecutor<Request, Response> e) {
        if (executing_)
            throw new RuntimeException("Use case already executing");

        Log.i(tag_, "started");
        executing_ = true;

        // clear previous response
        error_ = null;
        response_ = null;

        if (factory_ == null)
            throw new RuntimeException("Request factory not provided");

        // store
        request_ = factory_.create();

        // call
        e.execute(request_, new IResponseCallback<Response>() {
            @Override
            public void onResponse(Response r) {
                Log.i(tag_, "done " + r);
                onResponseReady(r);
            }

            @Override
            public void onError(String code, String e) {
                Log.i(tag_, "error " + code + " msg " + e);
                onErrorReady(code, e);
            }
        });
    }
}
