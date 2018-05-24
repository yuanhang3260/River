package channel;

import java.net.SocketAddress;

import channel.AbstractChannel;
import channel.ChannelFuture;
import multithread.TaskExecutor;

public abstract class ChannelHandler {
  protected AbstractChannel channel;
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
  // bind
  public ChannelFuture bind(SocketAddress local) {
    return bind(local, new ChannelFuture());
  }

  public ChannelFuture bind(SocketAddress local, ChannelFuture future) {
    return future;
  }

  // connect
  public ChannelFuture connect(SocketAddress local, SocketAddress remote) {
    return connect(local, remote, new ChannelFuture());
  }

  public ChannelFuture connect(SocketAddress local, SocketAddress remote, ChannelFuture future) {
    return future;
  }

  // write
  public ChannelFuture write(Object msg) {
    return write(msg, new ChannelFuture());
  }

  public ChannelFuture write(Object msg, ChannelFuture future) {
    return future;
  }

  // flush
  public ChannelFuture flush() {
    return flush(new ChannelFuture());
  }

  public ChannelFuture flush(ChannelFuture future) {
    return future;
  }

  // close
  public ChannelFuture close() {
    return close(new ChannelFuture());
  }

  public ChannelFuture close(ChannelFuture future) {
    return future;
  }
}
