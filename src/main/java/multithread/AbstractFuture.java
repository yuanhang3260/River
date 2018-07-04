package multithread;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import multithread.IFuture;
import multithread.IFutureListener;
import multithread.IPromise;

public abstract class AbstractFuture<V> implements IFuture<V>, IPromise<V> {
  protected volatile Object result;

  List<IFutureListener<V>> listeners = new ArrayList<IFutureListener<V>>();

  // SUCCESS_VOID is a placeholder for a success-void result.
  private static final class SuccessVoid {}
  private static final SuccessVoid SUCCESS_VOID = new SuccessVoid();

  @Override
  public boolean isDone() {
    return result != null;
  }

  @Override
  public boolean isSuccess() {
    if (result == null) {
      return false;
    }
    return !(result instanceof FailureResult);
  }

  // FailureResult is a wrapper of failure cause. The cause can be any arbitrary exception threw by
  // the task, or a CancellationException if cancel() is called.
  private static final class FailureResult {
    private final Throwable cause;

    public FailureResult(Throwable cause) {
      this.cause = cause;
    }

    public Throwable getCause() {
      return this.cause;
    }
  }

  // Sub-classes need to implement interrupt.
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    // IsCancelled is a subset of isDone.
    if (isDone()) {
      return false;
    }

    synchronized(this) {
      // Double-check is safe since result is volatile.
      if (isDone()) {
        return false;
      }

      result = new FailureResult(new CancellationException());

      // Notify all threads that are blocking on get/await.
      notifyAll();
      notifyListeners();
    }
    return true;
  }

  @Override
  public boolean cancel() {
    return this.cancel(false);
  }

  @Override
  public boolean isCancelled() {
    return result != null && result instanceof FailureResult &&
           ((FailureResult)result).getCause() instanceof CancellationException;
  }

  @Override
  public boolean isCancellable() {
    return !this.isDone();
  }

  @Override
  public Throwable getCause() {
    if (result == null || !(result instanceof FailureResult)) {
      return null;
    }
    return ((FailureResult)result).getCause();
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    await();
    return get0();
  }

  @Override  
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    await(timeout, unit);

    if (isDone()) {
      return get0();
    } else {
      // Task is not done after timeout.
      throw new TimeoutException();
    }
  }

  @SuppressWarnings("unchecked")
  private V get0() throws InterruptedException, ExecutionException  {
    // Task is done and success, return the result. If return type is void, result will be set as
    // SUCCESS_VOID, and get() will return null.
    if (result != null && !(result instanceof FailureResult)) {
      return result == SUCCESS_VOID ? null : (V)result;
    }

    // Task is done and failure.
    Throwable cause = this.getCause();
    if (cause instanceof CancellationException) {
      throw (CancellationException)cause;
    }
    throw new ExecutionException(cause);
  }

  @Override
  public IFuture<V> await() throws InterruptedException {
    if (isDone()) {
      return this;
    }

    synchronized(this) {
      while (!isDone()) {
        try {
          // Wait for notify.
          this.wait();
        } catch (InterruptedException e) {
          throw e;
        }
      }
    }
    return this;
  }

  @Override
  public IFuture<V> await(long timeout, TimeUnit unit) throws InterruptedException {
    if (isDone()) {
      return this;
    }

    if (timeout < 0) {
      return this;
    }

    long timeoutNanos = unit.toNanos(timeout);
    long remainingTimeout = timeoutNanos;

    long startTime = System.nanoTime();
    synchronized(this) {
      while (!isDone()) {
        try {
          // Wait for notify.
          this.wait(remainingTimeout / 1000000, (int)(remainingTimeout % 1000000));
        } catch (InterruptedException e) {
          throw e;
        }

        // wait(timeout) does not reliably wait for timeout due to spurious wakeup.
        remainingTimeout = timeoutNanos - (System.nanoTime() - startTime);
        if (remainingTimeout <= 0) {
          return this;
        }
      }
    }
    return this;
  }

  @Override
  public IFuture<V> addListener(IFutureListener<V> listener) {
    if (isDone()) {
      try {
        listener.taskDone(this);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return this;
    }
    
    // Double check.
    synchronized(this) {
      if (isDone()) {
        try {
          listener.taskDone(this);
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        this.listeners.add(listener);
      }
      return this;
    }
  }

  private void notifyListeners() {
    for (IFutureListener<V> l : this.listeners) {
      try {
        // Execute listener callback.
        l.taskDone(this);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public boolean setSuccess(V result) {
    if (isDone()) {
      return false;
    }

    synchronized(this) {
      if (isDone()) {
        return false;
      }

      this.result = (result != null ? result : SUCCESS_VOID);
      notifyAll();
      notifyListeners();
    }
    return true;
  }

  @Override
  public boolean setFailure(Throwable cause) {
    if (isDone()) {
      return false;
    }

    synchronized(this) {
      if (isDone()) {
        return false;
      }

      this.result = new FailureResult(cause);
      notifyAll();
      notifyListeners();
    }
    return true;
  }
}
