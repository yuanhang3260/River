package multithread;

import java.lang.IllegalArgumentException;
import java.lang.Runnable;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

import multithread.AbstractFuture;
import multithread.IFuture;

public class FixedThreadPool {

  private class FutureTask<V> extends AbstractFuture<V> implements Runnable {
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

    // Set the which execute the task.
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

  private class RunnableToCallableAdapter<V> implements Callable<V> {
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

  public static int DEFAULT_CAPACITY = 4;

  private int capacity = 1;
  private List<Thread> workers;

  private Queue<FutureTask<?>> tasks;
  private boolean stop = false;
  private Object lock;

  public FixedThreadPool() {
    this(FixedThreadPool.DEFAULT_CAPACITY);
  }

  public FixedThreadPool(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("FixedThreadPool capacity must be >= 1");
    }

    this.capacity = capacity;
    init();
  }

  private void init() {
    this.tasks = new LinkedList<FutureTask<?>>();
    this.workers = new ArrayList<Thread>();
    this.lock = new Object();
  }

  public IFuture<?> execute(Runnable runnable) {
    FutureTask<Void> ftask = new FutureTask<Void>(runnable);
    return execute0(ftask) ? ftask : null;
  }

  public <V> IFuture<V> execute(Callable<V> callable) {
    FutureTask<V> ftask = new FutureTask<V>(callable);
    return execute0(ftask) ? ftask : null;
  }

  private boolean execute0(FutureTask<?> task) {
    synchronized(this.lock) {
      if (this.stop) {
        System.err.println("Thread pool is stopped, cannot add task");
        return false;
      }
      this.tasks.offer(task);
      this.lock.notify();
    }

    synchronized(this.workers) {
      if (this.workers.size() < this.capacity) {
        // Use lambda to create runnable.
        Thread worker = new Thread(() -> { this.runWorker(); });
        worker.start();
        this.workers.add(worker);
      }
    }
    return true;
  }

  private void runWorker() {
    try {
      while (true) {
        FutureTask<?> task;

        // Wait for task to come in.
        synchronized(this.lock) {
          while (tasks.isEmpty() && !this.stop) {
            this.lock.wait();
          }

          // If threadpool is stopped, and there is no pending task in queue,
          // return and terminate this worker. Note we guarantee that all
          // queued tasks are executed before this threadpool is shutdown.
          if (this.stop && this.tasks.isEmpty()) {
            return;
          }

          task = this.tasks.poll();
          task.setThread(Thread.currentThread());
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
      this.stop = true;
      this.lock.notifyAll();
    }
  }

  public void awaitTermination() {
    try {
      synchronized(this.workers) {
        for (Thread t: this.workers) {
          t.join();
        }
      }
      this.workers.clear();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  } 
}
