package org.lndroid.framework.usecases.rpc;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.engine.IAuthClient;

public class RPCCreateRoot extends AuthRPCUseCaseBase<WalletData.AddUserRequest, WalletData.User> {

    public RPCCreateRoot(IAuthClient client) {
        super(client, "RPCCreateRoot");
    }

    public void execute() {
        execute(new IExecutor<WalletData.AddUserRequest, WalletData.User>() {
            @Override
            public void execute(WalletData.AddUserRequest r, IResponseCallback<WalletData.User> cb) {
                client_.createRoot(r, cb);
            }
        });
    }
}


