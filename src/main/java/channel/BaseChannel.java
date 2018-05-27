package channel;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import buffer.ByteBuf;
import channel.ChannelExceptions;
import channel.ChannelHeaderHandler;
import channel.ChannelTailHandler;
import channel.ChannelPromise;
import multithread.TaskExecutorGroup;
import net.EventLoop;

public abstract class BaseChannel {
  // SelectionKey that this channel is bound to.
  protected SelectionKey key;

  // EventLoop that this channel is bound to.
  protected EventLoop eventLoop;

  // Handler pipeline.
  protected ChannelHeaderHandler header;
  protected ChannelTailHandler tail;

  public BaseChannel() {
    header = new ChannelHeaderHandler();
    tail = new ChannelTailHandler();
    header.link(tail);
  }

  public EventLoop getEventLoop() {
    return this.eventLoop;
  }

  // Network APIs, which are all outbound operations. They simply delegate the call to tail
  // ChannelHandler, and propagate it all the way down through the outbound pipeline, until
  // eventually a doXXX() task is added to in EventLoop.
  public ChannelFuture bind(SocketAddress local) {
    return tail.bind(local);
  }

  public ChannelFuture connect(SocketAddress remote) {
    return tail.connect(remote);
  }

  public ChannelFuture write(ByteBuf buf) {
    return tail.write(buf);
  }

  public ChannelFuture flush() {
    return tail.flush();
  }

  public ChannelFuture close() {
    return tail.close();
  }

  // Handle IO events when SelectionKeys are active. This method is called inside event loop.
  public abstract void handleNetworkIOEvents() throws ChannelExceptions.UnexpectedException;

  // Outbound IO implementation. Note all these methods are executed in EventLoop, which ensures
  // thread safety. These methods are for subclasses to implement, which may differ based on what
  // type of channel is.
  protected abstract void doBind(SocketAddress local, ChannelPromise promise);
  protected abstract void doConnect(SocketAddress remote, ChannelPromise promise);
  protected abstract void doWrite(ByteBuf buf, ChannelPromise promise);
  protected abstract void doFlush(ChannelPromise promise);
  protected abstract void doClose(ChannelPromise promise);
}
