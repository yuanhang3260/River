package channel;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.apache.log4j.Logger;

import buffer.ByteBuf;
import channel.BaseChannel;
import channel.ChannelExceptions;
import channel.OutboundBufferQueue;
import bootstrap.NioBootStrap;
import net.EventLoop;
import net.EventLoopGroup;

public class ClientChannel extends BaseChannel {
  private static final Logger log = Logger.getLogger(ClientChannel.class);

  private NioBootStrap bootstrap;

  // Lower-level network IO.
  private SocketChannel javaChannel;
  private ByteBuf inboundBuf;
  private OutboundBufferQueue outboundBufs;

  SocketAddress remote;
  ChannelPromise connectPromise;

  public ClientChannel(NioBootStrap bootstrap) {
    this(bootstrap, null);
  }

  public ClientChannel(NioBootStrap bootstrap, SocketChannel channel) {
    this.bootstrap = bootstrap;
    this.javaChannel = channel;

    this.inboundBuf = ByteBuf.alloc();
    this.outboundBufs = new OutboundBufferQueue();
  }

  @Override
  public void handleNetworkIOEvents() throws ChannelExceptions.UnexpectedException {
    if (key.isConnectable() &&
        (javaChannel.isConnectionPending() || javaChannel.isConnected())) {
      try {
        javaChannel.finishConnect();
        log.info("Connecting finished");

        connectPromise.setSuccess();
        key.interestOps(SelectionKey.OP_READ);
      } catch (IOException e) {
        e.printStackTrace();
        connectPromise.setFailure(e);
      }
    }

    if (key.isReadable()) {
      inboundBuf.readFromChannel(javaChannel);
      header.fireChannelRead();
    }

    if (key.isWritable()) {
      outboundBufs.flushToChannel(javaChannel);
      if (outboundBufs.isEmpty()) {
        // No more data is waiting for flushing, remove OP_WRITE from interest ops.
        key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
      }
    }
  }

  // Register the ClientChannel to a EventLoop.
  public void register(EventLoop eventLoop) throws ClosedChannelException {
    // Register the channel to selector with OP_READ. Note this ClientChannel Object is attached to
    // registration.
    this.key = javaChannel.register(eventLoop.getSelector(), SelectionKey.OP_READ, this);
    this.eventLoop = eventLoop;
  }

  public void register(EventLoop eventLoop, int interestOps) throws ClosedChannelException {
    this.key = javaChannel.register(eventLoop.getSelector(), interestOps, this);
    this.eventLoop = eventLoop;
  }

  // ------------------------------ Lower level IO functions ------------------------------------ //
  @Override
  protected void doBind(SocketAddress local, ChannelPromise promise) {
    promise.setFailure(
        new ChannelExceptions.MethodNotSupported("ClientChannel does not support bind"));
  }

  @Override
  protected void doConnect(SocketAddress remote, ChannelPromise promise) {
    if (remote != null) {
      promise.setFailure(
          new ChannelExceptions.ChannelUsedException(
            "ClientChannel already in connection with " + this.remote));
      return;
    }

    try {
      if (this.javaChannel == null) {
        this.javaChannel = SocketChannel.open();
        this.javaChannel.configureBlocking(false);
      }
      this.javaChannel.connect(remote);

      // Register this ClientChannel to a EventLoop and set the interestOps as OP_CONNECT.
      EventLoop eventloop = bootstrap.getEventLoopGroup().next();
      register(eventloop, SelectionKey.OP_CONNECT);

      // Set remote address, marking this ClientChannel is in connection state.
      this.remote = remote;
      this.connectPromise = promise;
    } catch (IOException e1) {
      promise.setFailure(e1);
      try {
        javaChannel.close();
      } catch (IOException e2) {
        e2.printStackTrace();
      }
      javaChannel = null;
    }
  }

  @Override
  protected void doWrite(ByteBuf buf, ChannelPromise promise) {
    outboundBufs.enqueueOutputBuf(buf);
    promise.setSuccess();
  }

  @Override
  protected void doFlush(ChannelPromise promise) {
    // Add flush request to OutboundBufferQueue, and wait for channel to be writable.
    outboundBufs.addFlush(promise);
    int interestOps = key.interestOps();
    if ((interestOps & SelectionKey.OP_WRITE) == 0) {
      key.interestOps(interestOps | SelectionKey.OP_WRITE);
    }
  }

  @Override
  protected void doClose(ChannelPromise promise) {
    try {
      javaChannel.close();
      promise.setSuccess();
    } catch (IOException e) {
      promise.setFailure(e);
    }
  }
}
