package org.lndroid.framework.usecases;

import androidx.lifecycle.LiveData;
import androidx.paging.PagedList;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;

public interface IListData<Request extends WalletData.ListRequestBase, Response extends WalletDataDecl.EntityBase> {

    // load the first page of results
    void load(Request req);

    // load further pages
    void loadMore(WalletData.ListPage p);

    // a single callback can be attached, if needed
    void setCallback(IResponseCallback<WalletDataDecl.ListResultTmpl<Response>> cb);

    // observe to read results
    LiveData<WalletDataDecl.ListResultTmpl<Response>> results();

    // observe to read errors, including TX_INVALID which means
    // data set was invalidated and now reset+load must be executed
    // to update the data (preferable w/ req.page.aroundId set near the
    // previously observed page)
    LiveData<WalletData.Error> error();

    // reset list to allow for new 'load' call
    void reset();

    // call to release reader resources
    void destroy();

    // to integrate w/ AndroidPaging library, create a Pager,
    // after that the ListData object itself should not be used,
    // as Pager will submit it's own requests and a callback.
    IPager createPager(PagedList.Config config);

    interface IPager<Request, Response> {
        // observe new PagedLists and submit them to Adapter
        LiveData<PagedList<Response>> pagedList();

        // set request that will be used to retrieve all pages
        void setRequest(Request req);

        // force the list to refresh
        void invalidate();
    }
}
