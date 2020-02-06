package org.lndroid.framework.usecases.rpc;

import org.lndroid.framework.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IAuthClient;

public class RPCInitWallet extends AuthRPCUseCaseBase<WalletData.InitWalletRequest, WalletData.InitWalletResponse> {

    public RPCInitWallet(IAuthClient client) {
        super(client, "RPCInitWallet");
    }

    public void execute() {
        execute(new IExecutor<WalletData.InitWalletRequest, WalletData.InitWalletResponse>() {
            @Override
            public void execute(WalletData.InitWalletRequest r, IResponseCallback<WalletData.InitWalletResponse> cb) {
                client_.initWallet(r, cb);
            }
        });
    }
}
