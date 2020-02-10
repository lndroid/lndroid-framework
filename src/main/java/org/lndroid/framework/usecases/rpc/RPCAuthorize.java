package org.lndroid.framework.usecases.rpc;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IAuthClient;

public class RPCAuthorize extends AuthRPCUseCaseBase<WalletData.AuthResponse, Boolean> {

    public RPCAuthorize(IAuthClient client) {
        super(client, "RPCAuthorize");
    }

    public void execute() {
        execute(new IExecutor<WalletData.AuthResponse, Boolean>() {
            @Override
            public void execute(WalletData.AuthResponse r, IResponseCallback<Boolean> cb) {
                client_.authorize(r, cb);
            }
        });
    }
}
