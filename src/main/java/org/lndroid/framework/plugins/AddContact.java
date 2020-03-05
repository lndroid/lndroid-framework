package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.PluginContext;

public class AddContact extends ActionBase<WalletData.AddContactRequest, WalletData.Contact> {

    // plugin's Dao must implement this
    public interface IDao extends IActionDao<WalletData.AddContactRequest, WalletData.Contact>{};

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
    protected boolean isUserPrivileged(WalletData.AddContactRequest req, WalletData.User user) {
        return user.isRoot();
    }

    @Override
    protected WalletData.Contact createResponse(PluginContext ctx, WalletData.AddContactRequest req, long authUserId) {
        return WalletData.Contact.builder()
                .setId(server().getIdGenerator().generateId(WalletData.Contact.class))
                .setUserId(ctx.user.id())
                .setCreateTime(System.currentTimeMillis())
                .setAuthUserId(authUserId)
                .setName(req.name())
                .setDescription(req.description())
                .setPubkey(req.pubkey())
                .setUrl(req.url())
                .setFeatures(req.features())
                .setRouteHints(Utils.assignRouteHintsIds(req.routeHints(), server().getIdGenerator()))
                .build();
    }

    @Override
    protected IActionDao.OnResponseMerge<WalletData.Contact> getMerger() {
        return new IActionDao.OnResponseMerge<WalletData.Contact>() {
            @Override
            public WalletData.Contact merge(WalletData.Contact old, WalletData.Contact cur) {
                return old.toBuilder()
                        .setRouteHints(cur.routeHints())
                        .setFeatures(cur.features())
                        .setName(cur.name())
                        .setDescription(cur.description())
                        .setUrl(cur.url())
                        .build();
            }
        };
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
        return !user.isApp() && !user.isAnonymous();
    }

    @Override
    protected WalletData.AddContactRequest getRequestData(IPluginData in) {
        in.assignDataType(WalletData.AddContactRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String id() {
        return DefaultPlugins.ADD_CONTACT;
    }

}