package channel;

import java.net.SocketAddress;
import org.apache.log4j.Logger;

import buffer.ByteBuf;
import channel.BaseChannel;
import channel.ChannelPromise;
import channel.ChannelFuture;
import handler.ChannelHandler;
import multithread.SingleThreadExecutor;
import multithread.TaskExecutor;

public class ChannelHandlerContext {
  private static final Logger log = Logger.getLogger(ChannelHandlerContext.class);

  protected BaseChannel channel;
  private ChannelHandler handler;

  private ChannelHandlerContext prev;
  private ChannelHandlerContext next;

  protected TaskExecutor executor;

  public ChannelHandlerContext(BaseChannel channel, ChannelHandler handler) {
    this.channel = channel;
    this.handler = handler;
  }

  public boolean isInbound() {
    return handler.isInbound();
  }

  public boolean isOutbound() {
    return handler.isOutbound();
  }

  public ChannelHandlerContext getNext() {
    return next;
  }

  public ChannelHandlerContext getPrev() {
    return prev;
  }

  // Link a ChannelHandlerContext behind this one. If this one already has a next node, the new
  // ChannelHandlerContext will be insert between.
  protected void link(ChannelHandlerContext ctx) {
    ChannelHandlerContext crtNext = this.next;
    this.next = ctx;
    ctx.prev = this;
    if (crtNext != null) {
      ctx.next = crtNext;
      crtNext.prev = ctx; 
    }
  }

  protected SingleThreadExecutor getExecutor() {
    if (executor != null) {
      return executor;
    } else {
      return channel.getEventLoop();
    }
  }

  protected ChannelHandlerContext findNextInbound() {
    ChannelHandlerContext node = next;
    while (node != null) {
      if (node.isInbound()) {
        return node;
      }
      node = node.next;
    }
    return null;
  }

  protected ChannelHandlerContext findNextOutbound() {
    ChannelHandlerContext node = prev;
    while (node != null) {
      if (node.isOutbound()) {
        return node;
      }
      node = node.prev;
    }
    return null;
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

  // ------------------------------- Pipeline Inbound Operations -------------------------------- //
  public ChannelHandlerContext fireChannelRegistered() {
    propagateInbound(new Runnable() {
      @Override
      public void run() {
        if (next != null) {
          next.handler.channelRegistered(next);
        }
      }
    });
    return this;
  }

  public ChannelHandlerContext fireChannelUnregistered() {
    propagateInbound(new Runnable() {
      @Override
      public void run() {
        if (next != null) {
          next.handler.channelUnregistered(next);
        }
      }
    });
    return this;
  }

  public ChannelHandlerContext fireChannelActive() {
    propagateInbound(new Runnable() {
      @Override
      public void run() {
        if (next != null) {
          next.handler.channelActive(next);
        }
      }
    });
    return this;
  }

  public ChannelHandlerContext fireChannelInactive() {
    propagateInbound(new Runnable() {
      @Override
      public void run() {
        if (next != null) {
          next.handler.channelInactive(next);
        }
      }
    });
    return this;
  }

  public ChannelHandlerContext fireChannelRead(Object msg) {
    propagateInbound(new Runnable() {
      @Override
      public void run() {
        if (next != null) {
          next.handler.channelRead(next, msg);
        }
      }
    });
    return this;
  }

  // ------------------------------ Pipeline Outbound Operations -------------------------------- //
  // bind
  public ChannelFuture bind(SocketAddress local) {
    DefaultChannelFuture future = new DefaultChannelFuture();
    bind(local, future);
    return future;
  }

  public void bind(SocketAddress local, ChannelPromise promise) {
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        // log.info(handler.getName() + " context: bind");
        if (prev != null) {
          prev.handler.bind(prev, local, promise);
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

  public void connect(SocketAddress local, ChannelPromise promise) {
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.handler.connect(prev, local, promise);
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
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.handler.write(prev, msg, promise);
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
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.handler.flush(prev, promise);
        } else {
          channel.doFlush(promise);
        }
      }
    });
  }

  // writeAndFlush
  public ChannelFuture writeAndFlush(Object msg) {
    DefaultChannelFuture future = new DefaultChannelFuture();
    writeAndFlush(msg, future);
    return future;
  }

  public void writeAndFlush(Object msg, ChannelPromise promise) {
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.handler.write(prev, msg, promise);
          prev.handler.flush(prev, promise);
        } else {
          channel.doWriteAndFlush((ByteBuf)msg, promise);
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
    propagateOutbound(new Runnable() {
      @Override
      public void run() {
        if (prev != null) {
          prev.handler.close(prev, promise);
        } else {
          channel.doClose(promise);
        }
      }
    });
  }
}
