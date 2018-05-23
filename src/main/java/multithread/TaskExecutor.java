package multithread;

import java.lang.Runnable;
import java.lang.Thread;
import java.util.concurrent.Callable;
import java.util.Queue;
import java.util.LinkedList;

import multithread.AbstractFuture;
import multithread.IFuture;

// Single-threaded queued task executor.
public class TaskExecutor {

  protected class FutureTask<V> extends AbstractFuture<V> implements Runnable {
    private Callable<V> task;
    private Thread thread;

    public FutureTask(Callable<V> callable) {
      if (callable == null) {
        throw new NullPointerException();
      }
      this.task = callable;
    }

    public FutureTask(Runnable runnable) {
      if (runnable == null) {
        throw new NullPointerException();
      }
      this.task = new RunnableToCallableAdapter<V>(runnable);
    }

    @Override
    public void run() {
      try {
        if (!isDone()) {
          V result = this.task.call();
          setSuccess(result);
        }
      } catch (Exception e) {
        setFailure(e);
      }
    }

    // Set the thread which executes the task.
    public boolean setThread(Thread thread) {
      if (this.thread != null) {
        return false;
      }

      this.thread = thread;
      return true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (!super.cancel(mayInterruptIfRunning)) {
        return false;
      }

      if (mayInterruptIfRunning && this.thread != null) {
        this.thread.interrupt();
      }
      return true;
    }
  }

  protected class RunnableToCallableAdapter<V> implements Callable<V> {
    private Runnable runnable;

    public RunnableToCallableAdapter(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public V call() throws Exception {
      this.runnable.run();
      return null;
    }
  }

  private enum State {
    INIT,
    RUNNING,
    IDLE,
    STOPPED
  }

  private Thread worker;

  private Queue<FutureTask<?>> tasks = new LinkedList<FutureTask<?>>();
  private volatile State state = State.INIT;
  private Object lock = new Object();

  public TaskExecutor() {
    this.worker = new Thread(() -> { this.runWorker(); });
    this.state = State.IDLE;
    this.worker.start();
  }

  public Thread thread() {
    return this.worker;
  }

  public boolean isIdle() {
    return this.state == State.IDLE;
  }

  public boolean inEventLoop() {
    return this.thread == Thread.currentThread();
  }

  public IFuture<?> submit(Runnable runnable) {
    FutureTask<Void> ftask = new FutureTask<Void>(runnable);
    return submit0(ftask) ? ftask : null;
  }

  public <V> IFuture<V> submit(Callable<V> callable) {
    FutureTask<V> ftask = new FutureTask<V>(callable);
    return submit0(ftask) ? ftask : null;
  }

  private boolean submit0(FutureTask<?> task) {
    synchronized(this.lock) {
      if (this.state == State.STOPPED) {
        System.err.println("Thread pool is stopped, cannot add task");
        return false;
      }
      this.tasks.offer(task);
      this.state = State.RUNNING;
      this.lock.notify();
    }
    return true;
  }

  private void runWorker() {
    try {
      while (true) {
        FutureTask<?> task;

        // Wait for task to come in.
        synchronized(this.lock) {
          while (tasks.isEmpty() && this.state != State.STOPPED) {
            this.state = State.IDLE;
            this.lock.wait();
          }

          // We guarantee that all queued tasks are executed before this executor is shutdown.
          if (this.state == State.STOPPED && this.tasks.isEmpty()) {
            return;
          }

          task = this.tasks.poll();
          task.setThread(this.worker);
        }

        try {
          // New task received, run it!
          task.run();
        } catch (Exception e) {

        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void stop() {
    synchronized(this.lock) {
      this.state = State.STOPPED;
      this.lock.notifyAll();
    }
  }

  public void awaitTermination() {
    try {
      this.worker.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  } 
}
