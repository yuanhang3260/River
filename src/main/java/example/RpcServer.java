package example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.*;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import bootstrap.NioServer;
import bootstrap.NioClient;
import buffer.ByteBuf;
import channel.ChannelFuture;
import channel.ChannelFutureListener;
import channel.ChannelHandlerContext;
import channel.ChannelInitializer;
import channel.NioChannel;
import codec.AbstractEncoder;
import codec.ReplayingDecoder;
import codec.ByteToMessageDecoder;
import handler.ChannelInboundHandler;
import handler.ChannelOutboundHandler;
import net.EventLoopGroup;

public class RpcServer {
  private static final Logger log = Logger.getLogger(RpcServer.class);

  // RPC request message.
  private static class RpcRequest {
    public int num1;
    public int num2;
    public char op;

    // magic is a generated string of random length 0 to 20. RPC response must echo back its md5
    // checksum for client to verify.
    public String magic;
  }

  // RPC response message.
  private static class RpcResponse {
    public double result;
    public String md5;
  }

  private static double calculate(int num1, int num2, char op) {
    if (op == '+') {
      return (double)(num1 + num2);
    } else if (op == '-') {
      return (double)(num1 - num2);
    } else if (op == '*') {
      return (double)(num1 * num2);
    } else {
      return (double)num1 / (double)num2;
    }
  }

  private static String computeChecksum(String str) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(str.getBytes());
      byte[] digest = md.digest();
      return Hex.encodeHexString(digest).toUpperCase();
    } catch (Exception e) {
      return "";
    }
  }

  // -------------------------------------- Server ---------------------------------------------- //
  private static class RpcRequestDecoder extends ReplayingDecoder {
    @Override
    public void decode(ByteBuf buf, List<Object> outs) throws Exception {
      int magicLength = buf.getInt() - 10;

      RpcRequest request = new RpcRequest();
      request.num1 = buf.getInt();
      request.num2 = buf.getInt();
      request.op = buf.getChar();

      // Decode magic string.
      byte[] array = new byte[magicLength];
      buf.get(array);
      request.magic = new String(array);

      outs.add(request);
    }
  }

  private static class RpcResponseEncoder extends AbstractEncoder {
    @Override
    public void encode(Object msg, ByteBuf buf) throws Exception {
      RpcResponse response = (RpcResponse)msg;
      buf.putDouble(response.result);
      buf.put(response.md5.getBytes());
    }
  }

  private static class RpcServerHandler extends ChannelInboundHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      log.info("Channel active");
      ctx.fireChannelActive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      log.info("Channel read");

      RpcRequest request = (RpcRequest)msg;
      RpcResponse response = new RpcResponse();
      response.result = RpcServer.calculate(request.num1, request.num2, request.op);
      response.md5 = RpcServer.computeChecksum(request.magic);

      ctx.write(response);
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

  // -------------------------------------- Client ---------------------------------------------- //
  private static class RpcRequestEncoder extends AbstractEncoder {
    @Override
    public void encode(Object msg, ByteBuf buf) throws Exception {
      RpcRequest request = (RpcRequest)msg;
      buf.putInt(10 + request.magic.length());
      buf.putInt(request.num1);
      buf.putInt(request.num2);
      buf.putChar(request.op);
      buf.put(request.magic.getBytes());
    }
  }

  private static class RpcResponseDecoder extends ByteToMessageDecoder {
    @Override
    public void decode(ByteBuf buf, List<Object> outs) throws Exception {
      if (buf.readableBytes() >= 40) {
        RpcResponse response = new RpcResponse();
        response.result = buf.getDouble();

        byte[] array = new byte[32];
        buf.get(array);
        response.md5 = new String(array);

        outs.add(response);
      }
    }
  }

  private static class RpcClientHandler extends ChannelOutboundHandler {
    private static final char[] ops = {'+', '-', '*', '/'};

    private int num1;
    private int num2;
    private char op;
    private String magic;
    private double result;

    private String genterateMagic() {
      int length = (int)(Math.random() * 20);
      char[] array = new char[length];
      for (int i = 0; i < length; i++) {
        array[i] = (char)(Math.random() * 128);
      }
      return new String(array);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      RpcRequest request = new RpcRequest();
      request.num1 = num1 = (int)(Math.random() * 1000);
      request.num2 = num2 = (int)(Math.random() * 1000);
      request.op = op = ops[(int)(Math.random() * 4)];
      request.magic = magic = genterateMagic();
      result = RpcServer.calculate(num1, num2, op);

      ctx.write(request);
      ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      log.info("Channel read");

      RpcResponse response = (RpcResponse)msg;
      if (Math.abs(response.result - this.result) > 0.0001 ||
          !response.md5.equals(RpcServer.computeChecksum(this.magic))) {
        log.error("Expect " + this.result + " with md5 " + RpcServer.computeChecksum(this.magic) +
                  ", but received " + response.result + " with md5 " + response.md5);
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
      log.info("Starting Rpc Server");
      EventLoopGroup bossGroup = new EventLoopGroup(1);
      EventLoopGroup workerGroup = new EventLoopGroup(4);
      try {
        NioServer server = new NioServer();
        server.group(bossGroup, workerGroup)
              .childHandler(new ChannelInitializer() {
                @Override
                public void initChannel(NioChannel channel) {
                  channel.addInboundHandler(new RpcRequestDecoder());
                  channel.addInboundHandler(new RpcServerHandler());
                  channel.addOutboundHandler(new RpcResponseEncoder());
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
                    channel.addInboundHandler(new RpcResponseDecoder());
                    channel.addInboundHandler(new RpcClientHandler());
                    channel.addOutboundHandler(new RpcRequestEncoder());
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
