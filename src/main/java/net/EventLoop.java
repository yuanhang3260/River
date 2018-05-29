package net;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import org.apache.log4j.Logger;

import channel.BaseChannel;
import channel.ChannelExceptions;
import multithread.TaskExecutor;
import net.EventLoopGroup;

public class EventLoop extends TaskExecutor {
  private static final Logger log = Logger.getLogger(EventLoop.class);
  private static final int SELECT_TIMOUT_MILLISECONDS = 1000;

  private EventLoopGroup group;
  private Selector selector;

  // Constructors.
  public EventLoop() throws IOException {
    this(null);
  }

  public EventLoop(EventLoopGroup group) throws IOException {
    super();

    this.group = group;
    this.selector = Selector.open();
  }

  public Selector getSelector() {
    return this.selector;
  }

  @Override
  protected boolean submit0(FutureTask<?> task) {
    if (super.submit0(task)) {
      selector.wakeup();
      return true;
    }
    return false;
  }

  @Override
  protected void runWorker() {
    while (this.state != State.STOPPED && !Thread.currentThread().isInterrupted()) {
      boolean hasTasks = false;
      synchronized(this.lock) {
        if (tasks.isEmpty()) {
          this.state = State.IDLE;
          hasTasks = false;
        }
      }

      try {
        // If task queue is empty, we enter a blocking select, otherwise do selectNow.
        if (!hasTasks) {
          log.info("Blocking Select");
          selector.select();
        } else {
          log.info("SelectNow");
          selector.selectNow();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      processIO();
      runTasks();
    }
  }

  private void processIO() {
    Set<SelectionKey> selectedKeys = selector.selectedKeys();
    Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext()) {
      SelectionKey key = it.next();
      it.remove();

      processSelectedKey(key);
    }
  }

  private void processSelectedKey(SelectionKey key) {
    BaseChannel channel = (BaseChannel)key.attachment();
    try {
      channel.handleNetworkIOEvents();
    } catch (ChannelExceptions.UnexpectedException e) {
      e.printStackTrace();
    }
  }

  private void runTasks() {
    ArrayList<FutureTask<?>> copy = new ArrayList<FutureTask<?>>();
    synchronized(this.lock) {
      for (FutureTask<?> task : this.tasks) {
        copy.add(task);
      }
      this.tasks.clear();
    }

    for (FutureTask<?> task : copy) {
      task.setThread(this.worker);
      try {
        task.run();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void stop() {
    synchronized(this.lock) {
      this.state = State.STOPPED;
      this.lock.notifyAll();
      this.selector.wakeup();
    }
  }
}
