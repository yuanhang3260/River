package multithread;

import java.lang.Runnable;
import java.lang.Thread;
import java.util.concurrent.Callable;

import multithread.IExecutor;

public interface SingleThreadExecutor extends IExecutor {
  // Get internal thread.
  Thread thread();

  // If the executor is in idle.
  boolean isIdle();

  // If current thread is the executing thread of this executor.
  boolean inEventLoop();
}
