package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.PluginContext;

public class AddAppContact extends ActionBase<WalletData.Contact, WalletData.Contact> {

    // plugin's Dao must implement this
    public interface IDao extends IActionDao<WalletData.Contact, WalletData.Contact>{};

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 300000; // 5 min

    @Override
    protected int defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected int maxTimeout() {
        return MAX_TIMEOUT;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.Contact req, WalletData.User user) {
        return user.isRoot();
    }

    @Override
    protected WalletData.Contact createResponse(PluginContext ctx, WalletData.Contact req, long authUserId) {
        return req.toBuilder()
                .setId(server().getIdGenerator().generateId(WalletData.Contact.class))
                .setUserId(ctx.user.id())
                .setCreateTime(System.currentTimeMillis())
                .setAuthUserId(authUserId)
                .setRouteHints(Utils.assignRouteHintsIds(req.routeHints(), server().getIdGenerator()))
                .build();
    }

    @Override
    protected void signal(WalletData.Contact rep) {
        engine().onSignal(id(), DefaultTopics.NEW_CONTACT, rep);
        engine().onSignal(id(), DefaultTopics.CONTACT_STATE, rep);
    }

    @Override
    protected Type getResponseType() {
        return WalletData.Contact.class;
    }

    @Override
    protected boolean isValidUser(WalletData.User user) {
        return user.isApp();
    }

    @Override
    protected WalletData.Contact getRequestData(IPluginData in) {
        in.assignDataType(WalletData.AddAppContactRequest.class);
        try {
            // NOTE: we swap App-request with Contact object to
            // allow auth UI to fill in the Contact using QR code etc
            WalletData.AddAppContactRequest r = in.getData();
            // FIXME if user provides some options we'd have to store them somewhere

            // empty request for now
            return WalletData.Contact.builder().build();
        } catch (IOException e) {
            return null;
        }
    }

    public AddAppContact() {
    }

    @Override
    public String id() {
        return DefaultPlugins.ADD_APP_CONTACT;
    }

}