package channel;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.apache.log4j.Logger;

import buffer.ByteBuf;
import bootstrap.NioServer;
import channel.BaseChannel;
import channel.ChannelExceptions;
import channel.ClientChannel;
import net.EventLoop;
import net.EventLoopGroup;

public class ServerListenChannel extends BaseChannel {
  private static final Logger log = Logger.getLogger(ServerListenChannel.class);

  private NioServer server;

  // Lower-level network IO.
  private ServerSocketChannel serverChannel;

  public ServerListenChannel(NioServer server, EventLoop serverEventLoop) throws IOException {
    this.server = server;
    this.eventLoop = serverEventLoop;

    serverChannel = ServerSocketChannel.open();
    serverChannel.configureBlocking(false);
  }

  @Override
  public void handleNetworkIOEvents() throws ChannelExceptions.UnexpectedException {
    if (key.isAcceptable()) {
      try {
        SocketChannel clientJavaChannel = serverChannel.accept();
        clientJavaChannel.configureBlocking(false);
        ClientChannel clientChannel = new ClientChannel(server, clientJavaChannel);

        // Register the client channel to a EventLoop. This will set the interest ops as OP_READ
        // for this channel.
        EventLoop clientEventLoop = server.getEventLoopGroup().next();
        clientChannel.register(clientEventLoop);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // ------------------------------ Lower level IO functions ------------------------------------ //
  @Override
  protected void doBind(SocketAddress local, ChannelPromise promise) {
    try {
      // Register server channel to selector.
      serverChannel.bind(local);
      serverChannel.register(this.eventLoop.getSelector(), SelectionKey.OP_ACCEPT);

      // TODO: Set promise?

    } catch (IOException e) {
      promise.setFailure(e);
    }
  }

  @Override
  protected void doConnect(SocketAddress remote, ChannelPromise promise) {
    promise.setFailure(
        new ChannelExceptions.MethodNotSupported("ServerListenChannel does not support connect"));
  }

  @Override
  protected void doWrite(ByteBuf buf, ChannelPromise promise) {
    promise.setFailure(
        new ChannelExceptions.MethodNotSupported("ServerListenChannel does not support write"));
  }

  @Override
  protected void doFlush(ChannelPromise promise) {
    promise.setFailure(
        new ChannelExceptions.MethodNotSupported("ServerListenChannel does not support flush"));
  }

  @Override
  protected void doClose(ChannelPromise promise) {
    promise.setFailure(
        new ChannelExceptions.MethodNotSupported("ServerListenChannel does not support close"));
  }
}
