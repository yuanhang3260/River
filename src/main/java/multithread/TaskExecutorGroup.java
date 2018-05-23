package multithread;

import java.lang.IllegalArgumentException;
import java.lang.Runnable;
import java.lang.Thread;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.ArrayList;

import multithread.AbstractFuture;
import multithread.IFuture;
import multithread.TaskExecutor;

public class TaskExecutorGroup {
  private List<TaskExecutor> executors;
  private int size;
  private int index;

  public TaskExecutorGroup(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Must specify size for TaskExecutorGroup");
    }

    this.size = size;
    this.executors = new ArrayList<TaskExecutor>();
  }

  // Get the next TaskExecutor.
  public TaskExecutor next() {
    synchronized(this) {
      int i = index % size;
      index++;
      if (i >= this.executors.size()) {
        TaskExecutor newExecutor = new TaskExecutor();
        this.executors.add(newExecutor);
        return newExecutor;
      } else {
        return this.executors.get(i);
      }
    }
  }
}
