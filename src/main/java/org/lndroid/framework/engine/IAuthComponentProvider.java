package org.lndroid.framework.engine;

import org.lndroid.framework.WalletData;

public interface IAuthComponentProvider {
    void assignAuthComponent(WalletData.AuthRequest.Builder b);
}
