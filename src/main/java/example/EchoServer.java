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

public class EchoServer {
  private static final Logger log = Logger.getLogger(EchoServer.class);

  private static class EchoServerHandler extends ChannelInboundHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      log.info("Channel active");
      ctx.fireChannelActive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      log.info("Channel read");

      ByteBuf buf = (ByteBuf)msg;
      // We expect an integer and a double.
      if (buf.readableBytes() < 12) {
        return;
      }

      int vInt = buf.getInt();
      log.info("received int: " + vInt);
      double vDobule = buf.getDouble();
      log.info("received double: " + vDobule);

      ctx.fireChannelRead(msg);

      // Echo back value * 2
      ByteBuf buf2 = ByteBuf.alloc();
      buf2.putInt(vInt * 2);
      buf2.putDouble(vDobule * 2);
      ctx.write(buf2);

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

  private static class EchoClientHandler extends ChannelOutboundHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      ByteBuf buf = ByteBuf.alloc();
      buf.putInt(1);
      buf.putDouble(3.5);

      ctx.write(buf);
      ChannelFuture future = ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      log.info("Channel read");

      ByteBuf buf = (ByteBuf)msg;
      if (buf.readableBytes() < 12) {
        return;
      }

      int vInt = buf.getInt();
      double vDobule = buf.getDouble();

      if (vInt != 2 || Math.abs(vDobule - 7.0) > 0.001) {
        log.error("Received wrong value " + vInt + " and " + vDobule);
        System.exit(1);
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      log.info("Channel inactive");
      ctx.close();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    // Server thread.
    Thread t1 = new Thread(() -> {
      log.info("Starting Echo Server");
      EventLoopGroup bossGroup = new EventLoopGroup(1);
      EventLoopGroup workerGroup = new EventLoopGroup(4);
      try {
        NioServer server = new NioServer();
        server.group(bossGroup, workerGroup)
              .childHandler(new ChannelInitializer() {
                @Override
                public void initChannel(NioChannel channel) {
                  channel.addInboundHandler(new EchoServerHandler());
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
                    channel.addInboundHandler(new EchoClientHandler());
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
