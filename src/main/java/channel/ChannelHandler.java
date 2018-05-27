package channel;

import java.net.SocketAddress;

import buffer.ByteBuf;
import channel.BaseChannel;
import channel.ChannelPromise;
import channel.ChannelFuture;
import channel.DefaultChannelFuture;
import multithread.SingleThreadExecutor;
import multithread.TaskExecutor;

public abstract class ChannelHandler {
  protected BaseChannel channel;
  protected TaskExecutor executor;

  private ChannelHandler prev;
  private ChannelHandler next;

  public abstract boolean isInbound();

  public boolean isOutbound() {
    return !isInbound();
  }

  protected ChannelHandler findNextInbound() {
    ChannelHandler node = next;
    while (node != null) {
      if (node.isInbound()) {
        return node;
      }
      node = node.next;
    }
    return null;
  }

  protected ChannelHandler findNextOutbound() {
    ChannelHandler node = prev;
    while (node != null) {
      if (node.isOutbound()) {
        return node;
      }
      node = node.prev;
    }
    return null;
  }

  // Link a handler behind this one. If this one already has next handler, the new handler will
  // be insert between.
  protected void link(ChannelHandler handler) {
    ChannelHandler crtNext = this.next;
    this.next = handler;
    handler.prev = this;
    if (crtNext != null) {
      handler.next = crtNext;
      crtNext.prev = handler; 
    }
  }

  protected SingleThreadExecutor getExecutor() {
    if (executor != null) {
      return executor;
    } else {
      return channel.getEventLoop();
    }
  }

  protected void propagateInbound(Runnable task) {
    if (next == null) {
      return;
    }

    SingleThreadExecutor executor = next.getExecutor();
    if (executor.threadRunning()) {
      task.run();
    } else {
      executor.execute(task);
    }
  }

  protected void propagateOutbound(Runnable task) {
    SingleThreadExecutor executor;
    if (prev != null) {
      executor = prev.getExecutor();
    } else {
      // Reach the end of outbound pipeline and hand the IO task to channel itself.
      executor = channel.getEventLoop();
    }

    if (executor.threadRunning()) {
      task.run();
    } else {
      executor.execute(task);
    }
  }

  // ---------------------------------- Inbound events ------------------------------------------ //
  // channel register event.
  public void channelRegistered() {}

  public ChannelHandler fireChannelRegistered() {
    return this;
  }

  // channel unregister event.
  public void channelUnregistered(Object msg) {}

  public ChannelHandler fireChannelUnregistered() {
    return this;
  }

  // channel active event.
  public void channelActive() {}

  public ChannelHandler fireChannelActive() {
    return this;
  }

  // channel inactive event.
  public void channelInactive() {}

  public ChannelHandler fireChannelInactive() {
    return this;
  }

  // channel read event.
  public void channelRead() {}

  public ChannelHandler fireChannelRead() {
    return this;
  }

  // ---------------------------------- Outbound events ----------------------------------------- //
  // doXXX() methods propagates the operation the next outbound handler. User overridden outbound
  // methods must explicitly call doXXX().

  // bind
  public ChannelFuture bind(SocketAddress local) {
    DefaultChannelFuture future = new DefaultChannelFuture();
    bind(local, future);
    return future;
  }

  public void bind(SocketAddress local, ChannelPromise promise) {
    doBind(local, promise);
  }

  protected void doBind(SocketAddress local, ChannelPromise promise) {
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.bind(local, promise);
        } else {
          channel.doBind(local, promise);
        }
      }
    });
  }

  // connect
  public ChannelFuture connect(SocketAddress remote) {
    DefaultChannelFuture future = new DefaultChannelFuture();
    connect(remote, future);
    return future;
  }

  public void connect(SocketAddress remote, ChannelPromise promise) {
    doConnect(remote, promise);
  }

  protected void doConnect(SocketAddress local, ChannelPromise promise) {
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.connect(local, promise);
        } else {
          channel.doConnect(local, promise);
        }
      }
    });
  }

  // write
  public ChannelFuture write(Object msg) {
    DefaultChannelFuture future = new DefaultChannelFuture();
    write(msg, future);
    return future;
  }

  public void write(Object msg, ChannelPromise promise) {
    doWrite(msg, promise);
  }

  protected void doWrite(Object msg, ChannelPromise promise) {
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.write(msg, promise);
        } else {
          channel.doWrite((ByteBuf)msg, promise);
        }
      }
    });
  }

  // flush
  public ChannelFuture flush() {
    DefaultChannelFuture future = new DefaultChannelFuture();
    flush(future);
    return future;
  }

  public void flush(ChannelPromise promise) {
    doFlush(promise);
  }

  protected void doFlush(ChannelPromise promise) {
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.flush(promise);
        } else {
          channel.doFlush(promise);
        }
      }
    });
  }  

  // close
  public ChannelFuture close() {
    DefaultChannelFuture future = new DefaultChannelFuture();
    close(future);
    return future;
  }

  public void close(ChannelPromise promise) {
    doClose(promise);
  }

  protected void doClose(ChannelPromise promise) {
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.close(promise);
        } else {
          channel.doClose(promise);
        }
      }
    });
  }
}
