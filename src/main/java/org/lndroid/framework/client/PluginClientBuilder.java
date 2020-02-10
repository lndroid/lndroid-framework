package org.lndroid.framework.client;

import android.os.Messenger;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.ICodecProvider;
import org.lndroid.framework.common.ISigner;
import org.lndroid.framework.common.IVerifier;
import org.lndroid.framework.defaults.DefaultIpcCodecProvider;
import org.lndroid.framework.defaults.DefaultVerifier;

public class PluginClientBuilder {

    private Messenger server_;
    private boolean ipc_;
    private ICodecProvider ipcCodecProvider_;
    private ISigner signer_;
    private IVerifier verifier_;
    private WalletData.UserIdentity userId_;
    private String servicePackageName_;
    private String serviceClassName_;
    private String servicePubkey_;

    public PluginClientBuilder setServer(Messenger server) {
        server_ = server;
        return this;
    }

    public PluginClientBuilder setIpc(boolean ipc) {
        ipc_ = ipc;
        return this;
    }

    public PluginClientBuilder setIpcCodecProvider(ICodecProvider codecProvider) {
        ipcCodecProvider_ = codecProvider;
        return this;
    }

    public PluginClientBuilder setSigner(ISigner signer) {
        signer_ = signer;
        return this;
    }

    public PluginClientBuilder setVerifier(IVerifier verifier) {
        verifier_ = verifier;
        return this;
    }

    public PluginClientBuilder setUserIdentity(WalletData.UserIdentity userId) {
        userId_ = userId;
        return this;
    }

    public PluginClientBuilder setUserId(int userId) {
        userId_ = WalletData.UserIdentity.builder().setUserId(userId).build();
        return this;
    }

    public PluginClientBuilder setServicePackageName(String servicePackageName) {
        servicePackageName_ = servicePackageName;
        return this;
    }

    public PluginClientBuilder setServiceClassName(String serviceClassName) {
        serviceClassName_ = serviceClassName;
        return this;
    }

    public PluginClientBuilder setServicePubkey(String servicePubkey) {
        servicePubkey_ = servicePubkey;
        return this;
    }

    public IPluginClient build() {
        if (!ipc_ && server_ == null)
            throw new RuntimeException("Plugin server not specified");
        if (userId_ == null)
            throw new RuntimeException("Plugin client user id not specified");
        if (ipcCodecProvider_ == null)
            ipcCodecProvider_ = new DefaultIpcCodecProvider();
        if (ipc_) {
            if (signer_ == null)
                throw new RuntimeException("Message signer not specified");
            if (verifier_ == null)
                verifier_ = new DefaultVerifier();
            if (serviceClassName_ == null)
                throw new RuntimeException("Plugin client service class name not specified");
            if (servicePackageName_ == null)
                throw new RuntimeException("Plugin client service package name not specified");
            if (servicePubkey_ == null)
                throw new RuntimeException("Plugin client service pubkey not specified");
        }

        return new PluginClient(userId_, server_, ipc_, ipcCodecProvider_,
                signer_, verifier_,
                servicePackageName_, serviceClassName_, servicePubkey_);
    }
}
