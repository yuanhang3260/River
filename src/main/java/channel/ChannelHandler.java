package channel;

import java.net.SocketAddress;

import channel.AbstractChannel;
import channel.ChannelPromise;
import channel.DefaultChannelFuture;
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
  public ChannelPromise bind(SocketAddress local) {
    return bind(local, new DefaultChannelFuture());
  }

  public ChannelPromise bind(SocketAddress local, ChannelPromise promise) {
    return promise;
  }

  // connect
  public ChannelPromise connect(SocketAddress local, SocketAddress remote) {
    return connect(local, remote, new DefaultChannelFuture());
  }

  public ChannelPromise connect(SocketAddress local, SocketAddress remote, ChannelPromise promise) {
    return promise;
  }

  // write
  public ChannelPromise write(Object msg) {
    return write(msg, new DefaultChannelFuture());
  }

  public ChannelPromise write(Object msg, ChannelPromise promise) {
    return promise;
  }

  // flush
  public ChannelPromise flush() {
    return flush(new DefaultChannelFuture());
  }

  public ChannelPromise flush(ChannelPromise promise) {
    return promise;
  }

  // close
  public ChannelPromise close() {
    return close(new DefaultChannelFuture());
  }

  public ChannelPromise close(DefaultChannelFuture promise) {
    return promise;
  }
}
