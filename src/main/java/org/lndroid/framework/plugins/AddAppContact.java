package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.PluginContext;

public class AddAppContact extends AddContact {

    // plugin's Dao must implement this
    public interface IDao extends AddContact.IDao{};

    protected boolean isValidUser(WalletData.User user) {
        return user.isApp();
    }

    @Override
    protected WalletData.AddContactRequest getRequestData(IPluginData in) {
        in.assignDataType(WalletData.AddAppContactRequest.class);
        try {
            // NOTE: we swap App-request with AddContactRequest object to
            // allow auth UI to fill in the Contact using QR code etc
            WalletData.AddAppContactRequest r = in.getData();

            return WalletData.AddContactRequest.builder()
                    .setName(r.name())
                    .setDescription(r.description())
                    .setUrl(r.url())
                    .build();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String id() {
        return DefaultPlugins.ADD_APP_CONTACT;
    }

}