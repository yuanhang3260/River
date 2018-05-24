package multithread;

import java.lang.Runnable;
import java.util.concurrent.Callable;

import multithread.IFuture;

public interface IExecutor {
  IFuture<?> submit(Runnable runnable);

  public <V> IFuture<V> submit(Callable<V> callable);
}
