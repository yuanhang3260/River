package net;

import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.Runnable;
import java.lang.Thread;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.ArrayList;

import net.EventLoop;

public class EventLoopGroup {
  private List<EventLoop> eventLoops;
  private int size;
  private int index;

  public EventLoopGroup(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Must specify size for EventLoopGroup");
    }

    this.size = size;
    this.eventLoops = new ArrayList<EventLoop>();
  }

  // Get the next EventLoop.
  public EventLoop next() throws IOException {
    synchronized(this) {
      int i = index % size;
      index++;
      if (i >= this.eventLoops.size()) {
        EventLoop newEventLoop = new EventLoop(this);
        this.eventLoops.add(newEventLoop);
        newEventLoop.start();
        return newEventLoop;
      } else {
        return this.eventLoops.get(i);
      }
    }
  }

  public void shutdown() {
    synchronized(this) {
      for (EventLoop eventLoop : eventLoops) {
        eventLoop.stop();
      }
    }
  }
}
