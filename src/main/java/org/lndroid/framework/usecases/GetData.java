package org.lndroid.framework.usecases;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.paging.ItemKeyedDataSource;
import androidx.paging.PagedList;

import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

// to be used in UI thread only
public abstract class GetData<DataType, /*optional*/IdType> extends GetDataBg<DataType, IdType> {

    private MutableLiveData<DataType> data_ = new MutableLiveData<>();
    private MutableLiveData<WalletData.Error> error_ = new MutableLiveData<>();
    private IResponseCallback<DataType> pagerCb_;

    public GetData(IPluginClient client, String pluginId) {
        super(client, pluginId);
        setCallback(new IResponseCallback<DataType>() {
            @Override
            public void onResponse(DataType r) {
                if (pagerCb_ != null)
                    pagerCb_.onResponse(r);
                data_.setValue(r);
            }

            @Override
            public void onError(String code, String e) {
                if (pagerCb_ != null)
                    pagerCb_.onError(code, e);
                error_.setValue(WalletData.Error.builder().setCode(code).setMessage(e).build());
            }
        });
    }

    public LiveData<DataType> data() {
        return data_;
    }

    public LiveData<WalletData.Error> error() {
        return error_;
    }

    public Pager createPager(IFieldMapper<DataType> mapper) {
        return new Pager(mapper);
    }

    public interface IFieldMapper<DataType> {
        List<WalletData.Field> mapToFields(DataType t);
    }

    public interface IPager<IdType> {
        // observe new PagedLists and submit them to Adapter
        LiveData<PagedList<WalletData.Field>> pagedList();

        // request
        void setRequest(WalletDataDecl.GetRequestTmpl<IdType> req);

        // force the list to refresh
        void invalidate();
    }

    public class Pager implements IPager<IdType> {

        private IFieldMapper<DataType> mapper_;

        // current data source feeding data to current pagedlist
        private DataSource currentDataSource_;

        // observable paged list to be subscribed by UI
        private MutableLiveData<PagedList<WalletData.Field>> pagedList_ = new MutableLiveData<>();

        // request stored to reload data in case of invalidation
        private WalletDataDecl.GetRequestTmpl<IdType> request_;

        private Pager(IFieldMapper<DataType> mapper) {
            mapper_ = mapper;

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

        private void buildPagedList(){
            // get current cursor
            String initializeKey = null;
            if (pagedList_.getValue() != null) {
                initializeKey = (String)pagedList_.getValue().getLastKey();
            }

            // detach old datasource from list results
            if (currentDataSource_ != null)
                data_.removeObserver(currentDataSource_);

            // create new data source
            currentDataSource_ = new DataSource(mapper_);

            // make datasource observe list results
            data_.observeForever(currentDataSource_);

            // sync executor
            Executor executor = new Executor() {
                @Override
                public void execute(Runnable runnable) {
                    runnable.run();
                }
            };

            // creates a paged list and triggers loadInitial on the DataSource
            final PagedList<WalletData.Field> pagedList = new PagedList.Builder<String, WalletData.Field>(
                    currentDataSource_, new PagedList.Config.Builder().build())
                    // make sure load* calls are executed on the main thread
                    .setFetchExecutor(executor)
                    .setNotifyExecutor(executor)
                    // set initial key
                    .setInitialKey(initializeKey)
                    .build();

            // make sure that the after loadInitial is finished,
            // pagedList is delivered to the adapter
            pagerCb_ = new IResponseCallback<DataType>() {
                @Override
                public void onResponse(DataType r) {
                    pagedList_.setValue(pagedList);
                    setCallback(null);
                }

                @Override
                public void onError(String code, String e) {
                    pagedList_.setValue(pagedList);
                    setCallback(null);
                }
            };
        }

        @Override
        public LiveData<PagedList<WalletData.Field>> pagedList() {
            return pagedList_;
        }

        @Override
        public void setRequest(WalletDataDecl.GetRequestTmpl<IdType> req) {
            request_ = req;
            buildPagedList();
        }

        @Override
        public void invalidate() {
            buildPagedList();
        }

        public int count() {
            if (currentDataSource_ != null && currentDataSource_.fields_ != null)
                return currentDataSource_.fields_.size();
            else
                return 0;
        }

        private class DataSource
                // data source to be used by PagedList
                extends ItemKeyedDataSource<String, WalletData.Field>
                // Observer to watch for incoming results
                implements Observer<DataType>
        {
            private IFieldMapper<DataType> mapper_;
            private LoadInitialCallback<WalletData.Field> initialCallback_;
            private LoadCallback<WalletData.Field> callback_;
            private List<WalletData.Field> fields_;

            DataSource(IFieldMapper<DataType> mapper){
                mapper_ = mapper;
            }

            @Override
            public void onChanged(DataType res) {
                if (res == null)
                    fields_ = new ArrayList<>();
                else
                    fields_ = mapper_.mapToFields(res);

                if (initialCallback_ != null) {
                    Log.i("GI", "res " + fields_.size());
                    LoadInitialCallback<WalletData.Field> c = initialCallback_;
                    initialCallback_ = null;

                    c.onResult(fields_, 0, fields_.size());
                } else {
                    // only callable once
                    if (callback_ != null) {
                        LoadCallback<WalletData.Field> c = callback_;
                        callback_ = null;

                        c.onResult(fields_);
                    }
                }
            }

            @Override
            public void loadInitial(@NonNull LoadInitialParams<String> params,
                                    @NonNull final LoadInitialCallback<WalletData.Field> callback) {
                if (request_ == null) {
                    callback.onResult(new ArrayList<WalletData.Field>(), 0, 0);
                    return;
                }

                // save callback
                initialCallback_ = callback;

                GetData.this.setRequest(request_);
                start();
            }

            @Override
            public void loadAfter(@NonNull LoadParams<String> params,
                                  @NonNull LoadCallback<WalletData.Field> callback) {
                // no more data available
                callback_.onResult(new ArrayList<WalletData.Field>());
                // save callback
//                callback_ = callback;

//                GetData.this.setRequest(request_);
//                start();
            }

            @Override
            public void loadBefore(@NonNull LoadParams<String> params,
                                   @NonNull LoadCallback<WalletData.Field> callback) {
                callback_.onResult(new ArrayList<WalletData.Field>());
                // save callback
//                callback_ = callback;

                // request page
            }

            @NonNull
            @Override
            public String getKey(@NonNull WalletData.Field item) {
                return item.name();
            }

        }

    }

}
