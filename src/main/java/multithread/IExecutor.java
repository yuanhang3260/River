package multithread;

import java.lang.Runnable;
import java.util.concurrent.Callable;

import multithread.IFuture;

public interface IExecutor {
  void execute(Runnable runnable);

  IFuture<?> submit(Runnable runnable);

  <V> IFuture<V> submit(Callable<V> callable);
}
