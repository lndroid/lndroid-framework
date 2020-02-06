package org.lndroid.framework.engine;

import java.util.List;

import org.lndroid.framework.WalletData;

public interface IPluginBackground {
    String id();

    // set the engine
    void init(IDaoProvider dp, IPluginBackgroundCallback engine);

    // do the background work
    void work();

    // informs the plugin that auth was performed
    void auth(WalletData.AuthRequest ar, WalletData.AuthResponse r);

    // checks if user can auth this request
    boolean isUserPrivileged(WalletData.User user, String requestType);

    // get topics we need to react to
    void getSubscriptions(List<String> topics);

    // get notified about topic change
    void notify(String topic, Object data);
}
