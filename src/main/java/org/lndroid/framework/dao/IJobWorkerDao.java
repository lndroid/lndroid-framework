package org.lndroid.framework.dao;

import org.lndroid.framework.plugins.Transaction;

public interface IJobWorkerDao<Request> {

    /*
      FIXME problem is we don't have general job/tx entity!
       those are in different tables:
        - how to list all txs, say per user? per auth user? per time period?
        - we must create per-tx table, which is roughly equivalent to per-entity table that
        we are saving
        - BUT some requests that have same Request and Response now have confusion: tx input should
        be immutable!
         plus every getTransaction request must now also load the linked entity... if tx has classname
         that could be generalized... could it?
        - ok so only REAL argument is immutability? Seems so...
        - reading list of txs is same as list of Payments - just load linked entities from proper tables
        when a sample is ready
        - how to differenciate Request entities from real entities? Maybe separate tables?
         - sounds nice! in RoomTransaction, w/ autoincrement keys...

       so now every startTransaction needs to write request into separate table, and every get/list should
       reload them... not too scary! and we already have reader based on classname, just apply the same logic
     */

    Transaction<Request> getActiveJobs();
    void updateJob(Transaction<Request> tx);
    void setJobFailed(long userId, String txId, String code, String message);
    void setJobState(long userId, String txId, int state);
}
