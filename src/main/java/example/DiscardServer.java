package example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.apache.log4j.Logger;

import bootstrap.NioServer;
import bootstrap.NioClient;
import buffer.ByteBuf;
import channel.ChannelFuture;
import channel.ChannelFutureListener;
import channel.ChannelHandlerContext;
import channel.ChannelInitializer;
import channel.NioChannel;
import handler.ChannelInboundHandler;
import handler.ChannelOutboundHandler;
import net.EventLoopGroup;

public class DiscardServer {
  private static final Logger log = Logger.getLogger(DiscardServer.class);

  private static class DiscardHandler extends ChannelInboundHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      log.info("Channel active");
      ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      log.info("Channel inactive");
      ctx.close();
      ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      log.info("Channel read");

      ByteBuf buf = (ByteBuf)msg;
      if (buf.readableBytes() < 12) {
        return;
      }

      log.info("received int: " + buf.getInt());
      log.info("received double: " + buf.getDouble());
      ctx.fireChannelRead(msg);
    }
  }

  private static class DiscardClientHandler extends ChannelOutboundHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      ByteBuf buf = ByteBuf.alloc();
      buf.putInt(1);
      buf.putDouble(3.5);

      ctx.write(buf);
      ChannelFuture future = ctx.flush();
      future.addListener(new ChannelFutureListener() {
        @Override
        // Callback to execute when future is done.
        public void taskDone(ChannelFuture future) throws Exception {
          ctx.close();
        }
      });
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    // Server thread.
    Thread t1 = new Thread(() -> {
      log.info("Starting Discard Server");
      EventLoopGroup bossGroup = new EventLoopGroup(1);
      EventLoopGroup workerGroup = new EventLoopGroup(4);
      try {
        NioServer server = new NioServer();
        server.group(bossGroup, workerGroup)
              .childHandler(new ChannelInitializer() {
                @Override
                public void initChannel(NioChannel channel) {
                  channel.addInboundHandler(new DiscardHandler());
                }
              });

        server.listen(new InetSocketAddress("localhost", 9090)).sync();
      } catch (Exception e) {
        e.printStackTrace();
        return;
      } finally {
        workerGroup.shutdown();
        bossGroup.shutdown();
      }
    });

    // Client thread.
    Runnable client = new Runnable() {
      @Override
      public void run() {
        log.info("Starting client");
        EventLoopGroup workerGroup = new EventLoopGroup(1);
        try {
          NioClient client = new NioClient();
          client.group(workerGroup)
                .handler(new ChannelInitializer() {
                  @Override
                  public void initChannel(NioChannel channel) {
                    channel.addInboundHandler(new DiscardClientHandler());
                  }
                });

          client.connect(new InetSocketAddress("localhost", 9090)).sync();
          client.channel().awaitClose();
        } catch (Exception e) {
          e.printStackTrace();
          return;
        } finally {
          workerGroup.shutdown();
        }
      }
    };

    Thread[] clients = new Thread[100];
    for (int i = 0; i < clients.length; i++) {
      clients[i] = new Thread(client);
    }

    if (args.length > 0) {
      if (args[0].equals("server")) {
        t1.start();
        t1.join();
      } else if (args[0].equals("client")) {
        for (int i = 0; i < clients.length; i++) {
          clients[i].start();
        }
        for (int i = 0; i < clients.length; i++) {
          clients[i].join();
        }
      }
    }
  }
}
