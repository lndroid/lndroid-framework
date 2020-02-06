package org.lndroid.framework.usecases;

import java.lang.reflect.Type;

import org.lndroid.framework.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.client.IPluginTransaction;
import org.lndroid.framework.client.IPluginTransactionCallback;
import org.lndroid.framework.common.IPluginData;

// usable by non-UI thread
public abstract class GetDataBg<DataType, /*optional*/IdType> {
    private IPluginClient client_;
    private String pluginId_;
    private IPluginTransaction tx_;
    private WalletDataDecl.GetRequestTmpl<IdType> req_;
    private IResponseCallback<DataType> cb_;

    public GetDataBg(IPluginClient client, String pluginId) {
        client_ = client;
        pluginId_ = pluginId;
    }

    public void setRequest(WalletDataDecl.GetRequestTmpl<IdType> r) {
        req_ = r;
        if (!req_.noAuth())
            throw new RuntimeException("Auth not supported");
    }

    public void setCallback(IResponseCallback<DataType> cb) {
        cb_ = cb;
    }

    public boolean isActive() {
        return tx_ != null && tx_.isActive();
    }

    protected abstract DataType getData(IPluginData in);
    protected abstract Type getRequestType();

    public void start() {
        if (tx_ != null)
            throw new RuntimeException("Tx already started");

        tx_ = client_.createTransaction(pluginId_, "", new IPluginTransactionCallback() {
            @Override
            public void onResponse(IPluginData in) {
                DataType data = getData(in);
                // NOTE: data might be null which is fine, plugins might return
                // empty replies if record is not found
                cb_.onResponse(data);
//                if (data != null) {
//                    cb_.onResponse(data);
//                } else {
//                    cb_.onError(Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
//                    destroy();
//                }
            }

            @Override
            public void onAuth(WalletData.AuthRequest r) {}

            @Override
            public void onAuthed(WalletData.AuthResponse r) {}

            @Override
            public void onError(String code, String message) {
                cb_.onError(code, message);
                destroy();
            }
        });

        tx_.start(req_, getRequestType());
    }

    public void stop() {
        if (tx_ != null && tx_.isActive())
            tx_.stop();
        destroy();
    }

    public void destroy() {
        if (tx_ != null)
            tx_.destroy();
        tx_ = null;
    }
}
