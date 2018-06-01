package channel;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import buffer.ByteBuf;
import channel.ChannelExceptions;
import channel.ChannelHandlerContext;
import channel.ChannelPromise;
import channel.NioChannel;
import handler.ChannelHandler;
import handler.ChannelHeaderHandler;
import handler.ChannelTailHandler;
import multithread.TaskExecutorGroup;
import net.EventLoop;

public abstract class BaseChannel implements NioChannel {
  // SelectionKey that this channel is bound to.
  protected SelectionKey key;

  // EventLoop that this channel is bound to.
  protected EventLoop eventLoop;

  // Pipeline.
  protected ChannelHandlerContext header;
  protected ChannelHandlerContext tail;

  protected DefaultChannelFuture closeFuture = new DefaultChannelFuture();

  public BaseChannel() {
    createHandlerPipeline();
  }

  private void createHandlerPipeline() {
    header = new ChannelHandlerContext(this, new ChannelHeaderHandler());
    tail = new ChannelHandlerContext(this, new ChannelTailHandler());
    header.link(tail);
  }

  @Override
  public void addHandler(ChannelHandler handler) {
    this.tail.getPrev().link(new ChannelHandlerContext(this, handler));
  }

  public EventLoop getEventLoop() {
    return this.eventLoop;
  }

  public void awaitClose() throws Exception {
    this.closeFuture.sync();
  }

  // Inbound network events. They delegate the call to header ChannelHandlerContext to propagate
  // it all the way up to the end of pipeline.
  public BaseChannel fireChannelRegistered() {
    header.fireChannelRegistered();
    return this;
  }

  public BaseChannel fireChannelUnregistered() {
    header.fireChannelUnregistered();
    return this;
  }

  public BaseChannel fireChannelActive() {
    header.fireChannelActive();
    return this;
  }

  public BaseChannel fireChannelInactive() {
    header.fireChannelInactive();
    return this;
  }

  public BaseChannel fireChannelRead(Object msg) {
    header.fireChannelRead(msg);
    return this;
  }

  // Outbound network events. They simply delegate the call to tail ChannelHandler, and propagate
  // it all the way down through the outbound pipeline, until eventually a doXXX() task is added to
  // in EventLoop.
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
