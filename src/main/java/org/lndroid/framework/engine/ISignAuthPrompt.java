package org.lndroid.framework.engine;

import androidx.fragment.app.FragmentActivity;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.common.ISigner;

public interface ISignAuthPrompt {
    void auth(ISigner signer, FragmentActivity activity, WalletData.User u, IResponseCallback cb);
}
