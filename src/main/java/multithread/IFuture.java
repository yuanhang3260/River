package multithread;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface IFuture<V> extends Future<V> {
  // Check if task is "Done" and "Success".
  boolean isSuccess();

  // If the task is still cancellable.
  boolean isCancellable();

  // Cancel with mayInterruptIfRunning=false.
  boolean cancel();

  // If task is "Done" but "Failed", it will return the cause as a Throwable.
  Throwable getCause();

  // Wait infinitely for the task to be done. This method is interruptable.
  IFuture<V> await() throws InterruptedException;
  // Similar tp await(), but with timeout. This method is interruptable.
  IFuture<V> await(long timeout, TimeUnit unit) throws InterruptedException;

  // Add event listener. When task is done, listeners will be notified.
  IFuture<V> addListener(IFutureListener<V> listener);
}
