package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;

public class AddContactInvoiceDao extends
        LndActionDaoBase<WalletData.AddContactInvoiceRequest, WalletData.AddContactInvoiceResponse, RoomTransactions.AddContactInvoiceTransaction> {

    private AddContactInvoiceDaoRoom dao_;

    AddContactInvoiceDao(AddContactInvoiceDaoRoom dao) {
        super(dao, RoomTransactions.AddContactInvoiceTransaction.class);
        dao_ = dao;
    }

    public String getWalletContactName() {
        return "Contact name"; //dao_.getWalletContactName();
    }

    public String getWalletPubkey() {
        return dao_.getWalletPubkey();
    }

    public List<WalletData.ChannelEdge> getChannels(String pubkey) {
        List<RoomData.ChannelEdge> rcs = dao_.getChannels(pubkey);
        List<WalletData.ChannelEdge> cs = new ArrayList<>();
        for (RoomData.ChannelEdge rc: rcs) {
            WalletData.ChannelEdge.Builder c = rc.getData().toBuilder();

            List<RoomData.RoutingPolicy> rps = dao_.getRoutingPolicies(c.channelId());
            for(RoomData.RoutingPolicy rp: rps) {
                WalletData.RoutingPolicy p = rp.getData();
                if (p.reverse())
                    c.setNode2Policy(p);
                else
                    c.setNode1Policy(p);
            }

            cs.add(c.build());
        }
        return cs;
    }
}

@Dao
abstract class AddContactInvoiceDaoRoom
        implements IRoomLndActionDao<RoomTransactions.AddContactInvoiceTransaction,
                WalletData.AddContactInvoiceRequest, WalletData.AddContactInvoiceResponse>{

    @Query("SELECT identityPubkey FROM WalletInfo LIMIT 1")
    public abstract String getWalletPubkey();

    @Query("SELECT * FROM ChannelEdge WHERE node1Pubkey = :pubkey OR node2Pubkey = :pubkey")
    public abstract List<RoomData.ChannelEdge> getChannels(String pubkey);

    @Query("SELECT * FROM RoutingPolicy WHERE channelId = :channelId")
    public abstract List<RoomData.RoutingPolicy> getRoutingPolicies(long channelId);

    @Override @Query("SELECT * FROM AddContactInvoiceTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.AddContactInvoiceTransaction> getTransactions();

    @Override @Query("SELECT * FROM AddContactInvoiceTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.AddContactInvoiceTransaction getTransaction(int txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.AddContactInvoiceTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.AddContactInvoiceTransaction tx);

    @Query("UPDATE AddContactInvoiceTransaction " +
            "SET txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void confirmTransaction(int txUserId, String txId, int txAuthUserId, long time);

    @Override
    @Query("UPDATE AddContactInvoiceTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void rejectTransaction(int txUserId, String txId, int txAuthUserId, int txState, long time);

    @Override
    @Query("UPDATE AddContactInvoiceTransaction " +
            "SET txState = :txState, txDoneTime = :time, txError = :code " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(int txUserId, String txId, String code, int txState, long time);

    @Override
    @Query("UPDATE AddContactInvoiceTransaction " +
            "SET txState = :txState, txDoneTime = :time " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void timeoutTransaction(int txUserId, String txId, int txState, long time);

    @Override @Transaction
    public void confirmTransaction(int txUserId, String txId, int txAuthUserId, long time,
                                   WalletData.AddContactInvoiceRequest authedRequest) {

        if (authedRequest != null) {
            RoomTransactions.AddContactInvoiceTransaction tx = getTransaction(txUserId, txId);
            tx.txData.txAuthTime = time;
            tx.txData.txAuthUserId = txAuthUserId;
            tx.request = authedRequest;
            updateTransaction(tx);
        } else {
            confirmTransaction(txUserId, txId, txAuthUserId, time);
        }
    }

    @Override @Transaction
    public WalletData.AddContactInvoiceResponse commitTransaction(
            int txUserId, String txId,
            WalletData.AddContactInvoiceResponse rep, long time) {

        // get tx
        RoomTransactions.AddContactInvoiceTransaction tx = getTransaction(txUserId, txId);

        // update state
        tx.setResponse(rep);
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;

        // write tx
        updateTransaction(tx);

        return rep;
    }
}