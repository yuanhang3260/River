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
  boolean threadRunning();

  // Stop the executor. This is graceful stop, which will wait for all pending tasks to be done.
  // Mostly it should be followed by awaitTermination;
  void stop();

  // Wait for all tasks to be done.
  void awaitTermination();
}
