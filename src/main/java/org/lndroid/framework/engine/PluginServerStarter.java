package org.lndroid.framework.engine;

import android.os.Looper;
import android.os.Messenger;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.lndroid.framework.common.ICodecProvider;

public class PluginServerStarter {

    // a single global server is only allowed
    private static final Object lock_ = new Object();
    private static Messenger server_;

    private IPluginProvider pluginProvider_;
    private IDaoProvider daoProvider_;
    private ICodecProvider ipcCodecProvider_;
    private IAuthComponentProvider authComponentProvider_;

    static class Future extends FutureTask<Messenger> {

        Future() {
            // dumb noop callable
            super(new Callable<Messenger>(){
                @Override
                public Messenger call() throws Exception {
                    return null;
                }
            });
        }

        void setMessenger(Messenger m) {
            set(m);
        }
    }

    class Worker extends Thread {
        private Future future_;

        Worker(Future f){
            future_ = f;
        }

        @Override
        public void run() {
            Looper.prepare();
            Log.i("PluginServerStarter", "Looper prepared on thread "+Thread.currentThread().getId());
//            Looper.myLooper().setMessageLogging(new LogPrinter(Log.VERBOSE,"PluginServerLooper"));
            PluginServer server = new PluginServer(
                    pluginProvider_,
                    daoProvider_,
                    ipcCodecProvider_,
                    authComponentProvider_);

            server.init();
            future_.setMessenger(new Messenger(server));
            Looper.loop();
        }
    }

    public PluginServerStarter setPluginProvider(IPluginProvider provider) {
        pluginProvider_ = provider;
        return this;
    }

    public PluginServerStarter setDaoProvider(IDaoProvider provider) {
        daoProvider_ = provider;
        return this;
    }

    public PluginServerStarter setIpcCodecProvider(ICodecProvider cp) {
        ipcCodecProvider_ = cp;
        return this;
    }

    public PluginServerStarter setAuthComponentProvider(IAuthComponentProvider cp) {
        authComponentProvider_ = cp;
        return this;
    }

    public Messenger start(boolean mayExist) {

        if (pluginProvider_ == null)
            throw new RuntimeException("Plugin provider not specified");
        if (daoProvider_ == null)
            throw new RuntimeException("DAO provider not specified");
        if (ipcCodecProvider_ == null)
            throw new RuntimeException("Codec provider not specified");
        if (authComponentProvider_ == null)
            throw new RuntimeException("Auth component provider not specified");

        // to avoid several starters from racing
        synchronized (lock_) {
            if (server_ != null) {
                if (mayExist)
                    return server_;
                throw new RuntimeException("Plugin server already exists");
            }

            Future future = new Future();
            Worker w = new Worker(future);
            w.start();
            try {
                server_ = future.get();
            } catch (Exception e) { // interrupts
                throw new RuntimeException("Error while starting plugin server: " + e);
            }

            return server_;
        }
    }
}
