package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IUtxoWorkerDao {
    WalletData.Utxo getByOutpoint(String txidHex, int outputIndex);
    void update(WalletData.Utxo utxo);
}
