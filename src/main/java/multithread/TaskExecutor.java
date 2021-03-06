package multithread;

import java.lang.Runnable;
import java.lang.Thread;
import java.util.concurrent.Callable;
import java.util.Queue;
import java.util.LinkedList;

import multithread.AbstractFuture;
import multithread.SingleThreadExecutor;
import multithread.IFuture;

// Single-threaded queued task executor.
public class TaskExecutor implements SingleThreadExecutor {

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

  protected enum State {
    INIT,
    RUNNING,
    IDLE,
    STOPPED,
  }

  protected Thread worker;

  protected Queue<FutureTask<?>> tasks = new LinkedList<FutureTask<?>>();
  protected volatile State state = State.INIT;
  protected Object lock = new Object();

  public TaskExecutor() {
    this.worker = new Thread(() -> { this.runWorker(); });
    this.state = State.IDLE;
  }

  public void start() {
    this.worker.start();
  }

  @Override
  public Thread thread() {
    return this.worker;
  }

  @Override
  public boolean isIdle() {
    return this.state == State.IDLE;
  }

  @Override
  public boolean threadRunning() {
    return this.worker == Thread.currentThread();
  }

  @Override
  public void execute(Runnable runnable) {
    submit(runnable);
  }

  @Override
  public IFuture<?> submit(Runnable runnable) {
    FutureTask<Void> ftask = new FutureTask<Void>(runnable);
    return submit0(ftask) ? ftask : null;
  }

  @Override
  public <V> IFuture<V> submit(Callable<V> callable) {
    FutureTask<V> ftask = new FutureTask<V>(callable);
    return submit0(ftask) ? ftask : null;
  }

  protected boolean submit0(FutureTask<?> task) {
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

  protected void runWorker() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
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
          if (!task.isCancelled()) {
            // New task received, run it!
            task.run();
          }
        } catch (Exception e) {
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void stop() {
    synchronized(this.lock) {
      this.state = State.STOPPED;
      this.lock.notifyAll();
    }
  }

  @Override
  public void awaitTermination() {
    try {
      this.worker.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  } 
}
