package handler;

import java.net.SocketAddress;
import org.apache.log4j.Logger;

import buffer.ByteBuf;
import channel.BaseChannel;
import channel.ChannelFuture;
import channel.ChannelHandlerContext;
import channel.ChannelPromise;
import channel.DefaultChannelFuture;
import multithread.SingleThreadExecutor;
import multithread.TaskExecutor;

public abstract class ChannelHandler {
  private static final Logger log = Logger.getLogger(ChannelHandler.class);

  protected BaseChannel channel;
  protected TaskExecutor executor;

  private ChannelHandler prev;
  private ChannelHandler next;

  private String name = "DefaultChannelHandler";

  public abstract boolean isInbound();

  public boolean isOutbound() {
    return !isInbound();
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  // ---------------------------------- Inbound events ------------------------------------------ //
  public void channelRegistered(ChannelHandlerContext ctx) {
    ctx.fireChannelRegistered();
  }

  public void channelUnregistered(ChannelHandlerContext ctx) {
    ctx.fireChannelUnregistered();
  }

  public void channelActive(ChannelHandlerContext ctx) {
    ctx.fireChannelActive();
  }

  public void channelInactive(ChannelHandlerContext ctx) {
    ctx.fireChannelInactive();
  }

  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ctx.fireChannelRead(msg);
  }

  // --------------------------------- Outbound Operations -------------------------------------- //
  // These methods are for user-defined sub-classes to override.
  public void bind(ChannelHandlerContext ctx, SocketAddress local, ChannelPromise promise) {
    // log.info(getName() + ": bind");
    ctx.bind(local, promise);
  }

  public void connect(ChannelHandlerContext ctx, SocketAddress remote, ChannelPromise promise) {
    ctx.connect(remote, promise);
  }

  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    ctx.write(msg, promise);
  }

  public void flush(ChannelHandlerContext ctx, ChannelPromise promise) {
    ctx.flush(promise);
  }

  public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
    ctx.close(promise);
  }
}
