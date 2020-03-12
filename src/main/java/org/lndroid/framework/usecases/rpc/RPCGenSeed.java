package org.lndroid.framework.usecases.rpc;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IAuthClient;

public class RPCGenSeed extends AuthRPCUseCaseBase<WalletData.GenSeedRequest, WalletData.GenSeedResponse> {

    public RPCGenSeed(IAuthClient client) {
        super(client, "RPCGenSeed");
    }

    public void execute() {
        execute(new IExecutor<WalletData.GenSeedRequest, WalletData.GenSeedResponse>() {
            @Override
            public void execute(WalletData.GenSeedRequest r, IResponseCallback<WalletData.GenSeedResponse> cb) {
                client_.genSeed(r, cb);
            }
        });
    }
}

