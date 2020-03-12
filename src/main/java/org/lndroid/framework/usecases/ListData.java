package org.lndroid.framework.usecases;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.paging.ItemKeyedDataSource;
import androidx.paging.PagedList;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.client.IPluginTransaction;
import org.lndroid.framework.client.IPluginTransactionCallback;
import org.lndroid.framework.common.IPluginData;

abstract class  ListData<Request extends WalletData.ListRequestBase, Response extends WalletDataDecl.EntityBase>
        implements IListData<Request, Response> {

    private IPluginClient client_;
    private String pluginId_;
    private IPluginTransaction tx_;
    private IResponseCallback<WalletDataDecl.ListResultTmpl<Response>> cb_;
    private IResponseCallback<WalletDataDecl.ListResultTmpl<Response>> pagerCb_;
    private MutableLiveData<WalletDataDecl.ListResultTmpl<Response>> results_ = new MutableLiveData<>();
    private MutableLiveData<WalletData.Error> error_ = new MutableLiveData<>();

    public ListData(IPluginClient client, String pluginId) {
        client_ = client;
        pluginId_ = pluginId;
    }

    protected abstract WalletDataDecl.ListResultTmpl<Response> getData(IPluginData in);
    protected abstract Type getRequestType();

    public void setCallback(IResponseCallback<WalletDataDecl.ListResultTmpl<Response>> cb) {
        cb_ = cb;
    }

    private void onError(String code, String message) {
        // clear tx
        destroy();

        // notify observers
        error_.setValue(WalletData.Error.builder().setCode(code).setMessage(message).build());

        // exec callbacks
        if (cb_ != null)
            cb_.onError(code, message);
        if (pagerCb_ != null)
            pagerCb_.onError(code, message);
    }

    @Override
    public void load(Request req) {
        Log.i("LD", "load "+req+" thread "+Thread.currentThread().getId());
        // ensure we don't trigger auth
        if (!req.noAuth())
            throw new RuntimeException("Auth not supported");

        tx_ = client_.createTransaction(pluginId_, "", new IPluginTransactionCallback() {
            @Override
            public void onResponse(IPluginData in) {
                WalletDataDecl.ListResultTmpl<Response> r = getData(in);
                if (r != null) {
                    results_.setValue(r);

                    if (cb_ != null)
                        cb_.onResponse(r);
                    if (pagerCb_ != null)
                        pagerCb_.onResponse(r);
                } else {
                    ListData.this.onError(Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
                }
            }

            @Override
            public void onAuth(WalletData.AuthRequest r) {
                ListData.this.onError(Errors.PLUGIN_PROTOCOL, Errors.errorMessage(Errors.PLUGIN_PROTOCOL));
            }

            @Override
            public void onAuthed(WalletData.AuthResponse r) {
                ListData.this.onError(Errors.PLUGIN_PROTOCOL, Errors.errorMessage(Errors.PLUGIN_PROTOCOL));
            }

            @Override
            public void onError(String code, String message) {
                ListData.this.onError(code, message);
            }
        });

        tx_.start(req, getRequestType());
    }

    @Override
    public void loadMore(WalletData.ListPage p) {
        Log.i("LD", "load more "+p+" thread "+Thread.currentThread().getId());
        if (tx_ == null)
            throw new RuntimeException("Loader not started");

        tx_.send(p, WalletData.ListPage.class);
    }

    @Override
    public LiveData<WalletDataDecl.ListResultTmpl<Response>> results() {
        return results_;
    }

    @Override
    public LiveData<WalletData.Error> error() {
        return error_;
    }

    @Override
    public void reset() {
        destroy();
    }

    @Override
    public void destroy() {
        if (tx_ != null) {
            if (tx_.isActive())
                tx_.stop();
            tx_.destroy();
        }
        tx_ = null;
    }

    @Override
    public Pager createPager(PagedList.Config config) {
        return new Pager(config);
    }

    public class Pager implements IPager<Request, Response> {

        // config will be used to build new PagedLists after
        // data set is invalidated
        private PagedList.Config config_;

        // current data source feeding data to current pagedlist
        private DataSource currentDataSource_;

        // observable paged list to be subscribed by UI
        private MutableLiveData<PagedList<Response>> pagedList_ = new MutableLiveData<>();

        // request stored to reload data in case of invalidation
        private Request request_;

        // retry buildPagedList when current attempt finishes
        private boolean retry_;

        private Pager(PagedList.Config config) {
            config_ = config;

            // watch for INVALIDATE errors to rebuild the PagedList
            error_.observeForever(new Observer<WalletData.Error>() {
                @Override
                public void onChanged(WalletData.Error error) {
                    if (error.code().equals(Errors.TX_INVALIDATE)
                            || error.code().equals(Errors.TX_TIMEOUT)
                            || error.code().equals(Errors.TX_DONE)
                    ) {
                        buildPagedList();
                    }
                }
            });
        }

        private void buildPagedList() {

            // avoid parallel calls, instead set
            // a flag that we need to retry
            // when existing load finishes
            if (pagerCb_ != null) {
                retry_ = true;
                return;
            }

            // get current cursor
            Long initializeKey = null;
            if (pagedList_.getValue() != null) {
                initializeKey = (Long)pagedList_.getValue().getLastKey();
            }

            // detach old datasource from list results
            if (currentDataSource_ != null)
                results_.removeObserver(currentDataSource_);

            // create new data source
            currentDataSource_ = new DataSource();

            // make datasource observe list results
            results_.observeForever(currentDataSource_);

            // sync executor
            Executor executor = new Executor() {
                @Override
                public void execute(Runnable runnable) {
                    runnable.run();
                }
            };

            // creates a paged list and triggers loadInitial on the DataSource
            final PagedList<Response> pagedList = new PagedList.Builder<Long, Response>(currentDataSource_, config_)
                    // make sure load* calls are executed on the main thread
                    .setFetchExecutor(executor)
                    .setNotifyExecutor(executor)
                    // set initial key
                    .setInitialKey(initializeKey)
                    .build();

            // make sure that the after loadInitial is finished,
            // pagedList is delivered to the adapter
            pagerCb_ = new IResponseCallback<WalletDataDecl.ListResultTmpl<Response>>() {
                @Override
                public void onResponse(WalletDataDecl.ListResultTmpl<Response> r) {
                    pagedList_.setValue(pagedList);
                    pagerCb_ = null;
                    if (retry_) {
                        retry_ = false;
                        buildPagedList();
                    }
                }

                @Override
                public void onError(String code, String e) {
                    this.onResponse(null);
                }
            };
        }

        @Override
        public LiveData<PagedList<Response>> pagedList() {
            return pagedList_;
        }

        @Override
        public void setRequest(Request req) {
            if (!req.noAuth())
                throw new RuntimeException("Auth not supported");
            if (!req.enablePaging())
                throw new RuntimeException("Enable paging for the Pager");

            request_ = req;
            buildPagedList();
        }

        @Override
        public void invalidate() {
            buildPagedList();
        }

        public int count() {
            if (currentDataSource_ != null)
                return currentDataSource_.count_;
            else
                return 0;
        }

        private class DataSource
                // data source to be used by PagedList
                extends ItemKeyedDataSource<Long, Response>
                // Observer to watch for incoming results
                implements Observer<WalletDataDecl.ListResultTmpl<Response>>
        {

            private LoadInitialCallback<Response> initialCallback_;
            private LoadCallback<Response> callback_;
            private int count_;

            @Override
            public void onChanged(WalletDataDecl.ListResultTmpl<Response> res) {
                count_ = res.count();

                if (initialCallback_ != null) {
                    Log.i("LI", "res " + res.items().size() + " pos " + res.position() + " c " + res.count());
                    LoadInitialCallback<Response> c = initialCallback_;
                    initialCallback_ = null;

                    c.onResult(res.items(), res.position(), res.count());
                } else {
                    // only callable once
                    if (callback_ != null) {
                        LoadCallback<Response> c = callback_;
                        callback_ = null;

                        c.onResult(res.items());
                    }
                }
            }

            @Override
            public void loadInitial(@NonNull LoadInitialParams<Long> params, @NonNull final LoadInitialCallback<Response> callback) {
                if (request_ == null) {
                    callback.onResult(new ArrayList<Response>(), 0, 0);
                    return;
                }

                // save callback
                initialCallback_ = callback;

                // set params
                Request pagedRequest = (Request)request_.withPage(WalletData.ListPage.builder()
                        .setCount(params.requestedLoadSize)
                        .setAroundId(params.requestedInitialKey != null ? params.requestedInitialKey : 0)
                        .build());

                load(pagedRequest);
            }

            @Override
            public void loadAfter(@NonNull LoadParams<Long> params, @NonNull LoadCallback<Response> callback) {
                // save callback
                callback_ = callback;

                // request page
                WalletData.ListPage page = WalletData.ListPage.builder()
                        .setCount(params.requestedLoadSize)
                        .setAfterId(params.key != null ? params.key : 0)
                        .build();

                // ask plugin
                loadMore(page);
            }

            @Override
            public void loadBefore(@NonNull LoadParams<Long> params, @NonNull LoadCallback<Response> callback) {
                // save callback
                callback_ = callback;

                // request page
                WalletData.ListPage page = WalletData.ListPage.builder()
                        .setCount(params.requestedLoadSize)
                        .setBeforeId(params.key != null ? params.key : 0)
                        .build();

                // ask plugin
                loadMore(page);
            }

            @NonNull
            @Override
            public Long getKey(@NonNull Response item) {
                return item.id();
            }

        }

    }

}
