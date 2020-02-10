package org.lndroid.framework.usecases;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;

// to be used in UI thread only
public abstract class GetData<DataType, /*optional*/IdType> extends GetDataBg<DataType, IdType> {

    private MutableLiveData<DataType> data_ = new MutableLiveData<>();
    private MutableLiveData<WalletData.Error> error_ = new MutableLiveData<>();

    public GetData(IPluginClient client, String pluginId) {
        super(client, pluginId);
        setCallback(new IResponseCallback<DataType>() {
            @Override
            public void onResponse(DataType r) {
                data_.setValue(r);
            }

            @Override
            public void onError(String code, String e) {
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
}
