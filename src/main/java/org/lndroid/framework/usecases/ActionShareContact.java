package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;

public class ActionShareContact extends
        ActionUseCaseBase<WalletData.ShareContactRequest, WalletData.ShareContactResponse> {

    public ActionShareContact(IPluginClient client) {
        super(DefaultPlugins.SHARE_CONTACT, client, "ActionShareContact");
    }

    @Override
    protected WalletData.ShareContactResponse getData(IPluginData in) {
        in.assignDataType(WalletData.ShareContactResponse.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ShareContactResponse.class;
    }
}

