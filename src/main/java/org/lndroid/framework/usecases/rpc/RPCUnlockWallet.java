package org.lndroid.framework.usecases.rpc;

import org.lndroid.framework.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IAuthClient;

public class RPCUnlockWallet extends AuthRPCUseCaseBase<WalletData.UnlockWalletRequest, WalletData.UnlockWalletResponse> {

    public RPCUnlockWallet(IAuthClient client) {
        super(client, "RPCUnlockWallet");
    }

    @Override
    public void execute() {
        execute(new IExecutor<WalletData.UnlockWalletRequest, WalletData.UnlockWalletResponse>() {
            @Override
            public void execute(WalletData.UnlockWalletRequest r, IResponseCallback<WalletData.UnlockWalletResponse> cb) {
                client_.unlockWallet(r, cb);
            }
        });
    }
}
