package org.lndroid.framework.usecases.rpc;

import androidx.lifecycle.LifecycleOwner;

import org.lndroid.framework.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.usecases.IRequestFactory;

public interface IRPCUseCase<Request, Response> {
        /* USAGE by Activity (to avoid memory leaks and to preserve executing state):
            - set Callback and RequestFactory in onCreate, provide Activity as lifecycle owner
            - when Activity is ready to have Callback executed (onStart)
            - check isExecuted, if 'true'
             - update UI (show 'Executing') and call 'recover'
            - else, if not already executing
             - when user takes proper action:
             - update UI (show 'Executing') and call 'execute'
            - execute will throw if already executing
            - recover will throw if not executing
            - request and response and error are stored after the call
              for convenience, i.e. to let UI assign static
              Callbacks/Factories in onCreate so that
              factories could easily reference results of other use cases
            - RequestFactory is only accessed by 'execute',
              so no request objects need to be created if already
              executing
            - if Activity is destroyed, use case nulls the callback
              and factory and so won't hold activity from GC. If another
              activity is created it can re-assign itself as owner
              and proceed by setting it's own callback and factory
         */

    void setCallback(LifecycleOwner owner, IResponseCallback<Response> cb);
    void setRequestFactory(LifecycleOwner owner, IRequestFactory<Request> f);

    void execute();
    void recover();

    // 'true' when 'execute' was called but 'cb' wasn't yet executed
    boolean isExecuting();

    Request request();
    Response response();
    WalletData.Error error();
}
