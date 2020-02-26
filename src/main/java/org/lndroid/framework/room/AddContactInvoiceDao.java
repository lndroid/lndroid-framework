package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.AddContactInvoice;

class AddContactInvoiceDao
        extends LndActionDaoBase<WalletData.AddContactInvoiceRequest, WalletData.AddContactInvoiceResponse>
        implements AddContactInvoice.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.ADD_CONTACT_INVOICE;

    private DaoRoom dao_;

    AddContactInvoiceDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);

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


    @Dao
    abstract static class DaoRoom
            extends RoomLndActionDaoBase<WalletData.AddContactInvoiceRequest, WalletData.AddContactInvoiceResponse> {
        @Query("SELECT identityPubkey FROM WalletInfo LIMIT 1")
        public abstract String getWalletPubkey();

        @Query("SELECT * FROM ChannelEdge WHERE node1Pubkey = :pubkey OR node2Pubkey = :pubkey")
        public abstract List<RoomData.ChannelEdge> getChannels(String pubkey);

        @Query("SELECT * FROM RoutingPolicy WHERE channelId = :channelId")
        public abstract List<RoomData.RoutingPolicy> getRoutingPolicies(long channelId);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract long upsertRequest(RoomTransactions.AddContactInvoiceRequest i);

        @Query("SELECT * FROM txAddContactInvoiceRequest WHERE id_ = :id")
        abstract RoomTransactions.AddContactInvoiceRequest getRequestRoom(long id);

        @Override
        @Transaction
        public WalletData.AddContactInvoiceResponse commitTransaction(
                long userId, String txId, WalletData.AddContactInvoiceResponse r, long time,
                ILndActionDao.OnResponseMerge<WalletData.AddContactInvoiceResponse> merger) {
            return commitTransactionImpl(userId, txId, r, time, merger);
        }

        @Override
        @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.AddContactInvoiceRequest req) {
            createTransactionImpl(tx, req);
        }

        @Override
        protected long insertRequest(WalletData.AddContactInvoiceRequest req) {
            // convert request to room object
            RoomTransactions.AddContactInvoiceRequest r = new RoomTransactions.AddContactInvoiceRequest();
            r.data = req;

            // insert request
            return upsertRequest(r);
        }

        @Override
        protected void updateRequest(long id, WalletData.AddContactInvoiceRequest req) {
            // convert request to room object
            RoomTransactions.AddContactInvoiceRequest r = new RoomTransactions.AddContactInvoiceRequest();
            r.id_ = id;
            r.data = req;

            // update
            upsertRequest(r);
        }

        @Override
        public WalletData.AddContactInvoiceRequest getRequest(long id) {
            RoomTransactions.AddContactInvoiceRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Override
        protected long insertResponse(
                WalletData.AddContactInvoiceResponse r,
                ILndActionDao.OnResponseMerge<WalletData.AddContactInvoiceResponse> merger) {
            // not stored
            return 0;
        }

        @Override
        public WalletData.AddContactInvoiceResponse getResponse(long id) {
            // not stored
            return null;
        }

        @Override
        @Transaction
        public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time,
                                       WalletData.AddContactInvoiceRequest authedRequest) {
            confirmTransactionImpl(txUserId, txId, txAuthUserId, time, authedRequest);
        }
    }
}

