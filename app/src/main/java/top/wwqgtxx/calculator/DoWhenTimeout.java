package top.wwqgtxx.calculator;

import android.os.Handler;

import com.orhanobut.logger.Logger;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;

public class DoWhenTimeout implements AutoCloseable {
    private static ObjectPool<DoWhenTimeout> pool = new SoftReferenceObjectPool<>(new BasePooledObjectFactory<DoWhenTimeout>() {

        @Override
        public DoWhenTimeout create() {
            return new DoWhenTimeout();
        }

        @Override
        public PooledObject<DoWhenTimeout> wrap(DoWhenTimeout obj) {
            return new DefaultPooledObject<>(obj);
        }

        @Override
        public void passivateObject(PooledObject<DoWhenTimeout> p) {
            DoWhenTimeout doWhenTimeout = p.getObject();
            doWhenTimeout.handler = null;
            doWhenTimeout.runnable = null;
        }
    });
    private Handler handler;
    private Runnable runnable;

    private DoWhenTimeout() {
//        Logger.d("new DoWhenTimeout()");
        handler = null;
        runnable = null;
    }


    public static DoWhenTimeout obtain(Handler handler, Runnable runnable, long delayMillis) throws Exception {
        DoWhenTimeout doWhenTimeout = pool.borrowObject();
        doWhenTimeout.handler = handler;
        doWhenTimeout.runnable = runnable;
        doWhenTimeout.handler.postDelayed(runnable, delayMillis);
        return doWhenTimeout;
    }

    @Override
    public void close() throws Exception {
        if (this.handler != null && this.runnable != null) {
            this.handler.removeCallbacks(this.runnable);
        }
        pool.returnObject(this);
    }
}
