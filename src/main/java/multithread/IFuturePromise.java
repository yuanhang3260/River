package multithread;

import multithread.IFuture;
import multithread.IPromise;

public interface IFuturePromise<V> extends IFuture<V>, IPromise<V> {
}