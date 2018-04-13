package top.wwqgtxx.calculator;

import com.eclipsesource.v8.Releasable;
import com.orhanobut.logger.Logger;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;

public class AutoRelease implements AutoCloseable {
    private static ObjectPool<AutoRelease> pool = new SoftReferenceObjectPool<>(new BasePooledObjectFactory<AutoRelease>() {

        @Override
        public AutoRelease create(){
            return new AutoRelease();
        }

        @Override
        public PooledObject<AutoRelease> wrap(AutoRelease obj) {
            return new DefaultPooledObject<>(obj);
        }

        @Override
        public void passivateObject(PooledObject<AutoRelease> p){
            AutoRelease autoRelease = p.getObject();
            autoRelease.releasable = null;
        }
    });
    private Releasable releasable;

    private AutoRelease() {
//        Logger.d("new AutoRelease()");
        releasable = null;
    }

    public static AutoRelease obtain(Releasable releasable) throws Exception {
        AutoRelease autoRelease = pool.borrowObject();
        autoRelease.releasable = releasable;
        return autoRelease;
    }

    @Override
    public void close() throws Exception {
        if(releasable != null){
            releasable.release();
        }
        pool.returnObject(this);
    }
}
