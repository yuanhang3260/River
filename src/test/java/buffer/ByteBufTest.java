package buffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import static org.junit.Assert.*;
import org.junit.Test;
import java.util.*;

import buffer.ByteBuf;

public class ByteBufTest {
  private static final int DATA_LENGTH = 4194304 * 2 + 4194304 / 2;

  @Test
  public void testSingleByte() {
    ByteBuf bf = ByteBuf.alloc();
    for (int i = 0; i < DATA_LENGTH; i++) {
      bf.put((byte)i);
    }

    for (int i = 0; i < DATA_LENGTH; i++) {
      assertEquals(DATA_LENGTH - i, bf.readableBytes());
      assertEquals((byte)i, bf.get());
    }
    assertEquals(0, bf.readableBytes());
  }

  @Test
  public void testByteArray() {
    byte[] data = new byte[DATA_LENGTH];
    for (int i = 0; i < DATA_LENGTH; i++) {
      data[i]= (byte)i;
    }

    ByteBuf bf = ByteBuf.alloc();
    bf.put(data);

    byte[] receive = new byte[DATA_LENGTH];
    bf.get(receive);

    for (int i = 0; i < DATA_LENGTH; i++) {
      assertEquals(data[i], receive[i]);
    }
  }

  @Test
  public void testByteSubArray() {
    byte[] data = new byte[DATA_LENGTH];
    for (int i = 0; i < DATA_LENGTH; i++) {
      data[i]= (byte)i;
    }

    ByteBuf bf = ByteBuf.alloc();
    bf.put(data, 1, DATA_LENGTH - 2);

    byte[] receive = new byte[DATA_LENGTH];
    bf.get(receive, 1, DATA_LENGTH - 2);

    for (int i = 1; i < DATA_LENGTH - 1; i++) {
      assertEquals(data[i], receive[i]);
    }
  }

  @Test
  public void testMarkReset() {
    byte[] data = new byte[DATA_LENGTH];
    for (int i = 0; i < DATA_LENGTH; i++) {
      data[i]= (byte)i;
    }

    ByteBuf bf = ByteBuf.alloc();
    bf.put(data, 1, DATA_LENGTH - 2);

    bf.markReadIndex();

    // First read.
    byte[] receive = new byte[DATA_LENGTH];
    bf.get(receive, 1, DATA_LENGTH - 2);

    for (int i = 1; i < DATA_LENGTH - 1; i++) {
      assertEquals(data[i], receive[i]);
    }

    // Reset read index and read again.
    receive = new byte[DATA_LENGTH];
    assertTrue(bf.resetReadIndex());
    bf.get(receive, 1, DATA_LENGTH - 2);

    for (int i = 1; i < DATA_LENGTH - 1; i++) {
      assertEquals(data[i], receive[i]);
    }
  }

  @Test
  public void testInt() {
    ByteBuf bf = ByteBuf.alloc();

    final int range = 1024;
    for (int i = 0; i < range; i++) {
      bf.putInt(i);
    }

    for (int i = 0; i < range; i++) {
      assertEquals(i, bf.getInt());
    }

    for (int i = 0; i < range; i++) {
      bf.putInt(i);
      assertEquals(i, bf.getInt());
    }
  }

  @Test
  public void testShort() {
    ByteBuf bf = ByteBuf.alloc();

    final short range = 1024;
    for (short i = 0; i < range; i++) {
      bf.putShort(i);
    }

    for (short i = 0; i < range; i++) {
      assertEquals(i, bf.getShort());
    }

    for (short i = 0; i < range; i++) {
      bf.putShort(i);
      assertEquals(i, bf.getShort());
    }
  }

  @Test
  public void testLong() {
    ByteBuf bf = ByteBuf.alloc();

    final long range = 1024;
    for (long i = 0; i < range; i++) {
      bf.putLong(i);
    }

    for (long i = 0; i < range; i++) {
      assertEquals(i, bf.getLong());
    }

    for (long i = 0; i < range; i++) {
      bf.putLong(i);
      assertEquals(i, bf.getLong());
    }
  }

  @Test
  public void testChar() {
    ByteBuf bf = ByteBuf.alloc();

    final int range = 128;
    for (char i = 0; i < range; i++) {
      bf.putChar(i);
    }

    for (char i = 0; i < range; i++) {
      assertEquals(i, bf.getChar());
    }

    for (char i = 0; i < range; i++) {
      bf.putChar(i);
      assertEquals(i, bf.getChar());
    }
  }

  @Test
  public void testDouble() {
    ByteBuf bf = ByteBuf.alloc();

    for (double i = 0.0; i < 102.4; i = i + 0.1) {
      bf.putDouble(i);
    }

    for (double i = 0.0; i < 102.4; i = i + 0.1) {
      assertEquals(i, bf.getDouble(), 0.0001);
    }

    for (double i = 0.0; i < 102.4; i = i + 0.1) {
      bf.putDouble(i);
      assertEquals(i, bf.getDouble(), 0.0001);
    }
  }

  @Test
  public void testFloat() {
    ByteBuf bf = ByteBuf.alloc();

    for (float i = (float)0.0; i < 102.4; i = (float)(i + 0.1)) {
      bf.putFloat(i);
    }

    for (float i = (float)0.0; i < 102.4; i = (float)(i + 0.1)) {
      assertEquals(i, bf.getFloat(), 0.0001);
    }

    for (float i = (float)0.0; i < 102.4; i = (float)(i + 0.1)) {
      bf.putFloat(i);
      assertEquals(i, bf.getFloat(), 0.0001);
    }
  }

  @Test
  public void testMixed() {
    ByteBuf bf = ByteBuf.alloc();
    bf.putChar((char)1);
    bf.putDouble(2.0);
    bf.putInt(3);
    bf.putShort((short)4);
    bf.putFloat((float)5.0);
    bf.putLong(6);

    assertEquals((char)1, bf.getChar());
    assertEquals(2.0, bf.getDouble(), 0.000001);
    assertEquals(3, bf.getInt());
    assertEquals((short)4, bf.getShort());
    assertEquals((float)5.0, bf.getFloat(), 0.000001);
    assertEquals((long)6, bf.getLong());
  }

  @Test
  public void testChannelCommunication() throws InterruptedException {
    ByteBuf bf = ByteBuf.alloc();
    bf.putChar((char)1);
    bf.putDouble(2.0);
    bf.putInt(3);
    bf.putShort((short)4);
    bf.putFloat((float)5.0);
    bf.putLong(6);

    byte[] data = new byte[10000];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte)i;
    }
    bf.put(data);

    int totalBytes = bf.readableBytes();

    ByteBuf recvBuf = ByteBuf.alloc();

    // Server thread.
    Thread t1 = new Thread(() -> {
      int bytesReceived = 0;
      try {
        // Create server socket channel and set it as non-blocking mode.
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // Bind server socket to local listening address.
        serverChannel.bind(new InetSocketAddress("localhost", 9090));
        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
          if (selector.select() == 0) {
            continue;
          }

          Set<SelectionKey> selectedKeys = selector.selectedKeys();
          Iterator<SelectionKey> it = selectedKeys.iterator();
          while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();

            if (key.isAcceptable()) {
              serverChannel = (ServerSocketChannel)key.channel();
              SocketChannel clientChannel = serverChannel.accept();
              clientChannel.configureBlocking(false);
              clientChannel.register(selector, SelectionKey.OP_READ);
            } else if (key.isReadable()) {
              SocketChannel clientChannel = (SocketChannel)key.channel();
              recvBuf.readFromChannel(clientChannel);
              if (recvBuf.readableBytes() == totalBytes) {
                // All data received.
                assertEquals((char)1, recvBuf.getChar());
                assertEquals(2.0, recvBuf.getDouble(), 0.000001);
                assertEquals(3, recvBuf.getInt());
                assertEquals((short)4, recvBuf.getShort());
                assertEquals((float)5.0, recvBuf.getFloat(), 0.000001);
                assertEquals((long)6, recvBuf.getLong());
                byte[] buffer = new byte[10000];
                recvBuf.get(buffer);
                for (int i = 0; i < buffer.length; i++) {
                  assertEquals((byte)i, buffer[i]);
                }
                clientChannel.close();
                return;
              }
            }
          }
        }
      } catch (IOException e) {
        fail(e.getMessage());
      }
    });

    // Client thread.
    Thread t2 = new Thread(() -> {
      try {
        SocketChannel clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(false);
        clientChannel.connect(new InetSocketAddress("localhost", 9090));

        // Create Selector and register channel.
        Selector selector = Selector.open();
        clientChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);

        while (true) {
          // block.
          if (selector.select() == 0) {
            continue;
          }

          Set<SelectionKey> selectedKeys = selector.selectedKeys();
          Iterator<SelectionKey> it = selectedKeys.iterator();
          while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();

            clientChannel = (SocketChannel)key.channel();

            if (key.isConnectable() && clientChannel.isConnectionPending()) {
              clientChannel.finishConnect();
              key.interestOps(SelectionKey.OP_WRITE);
            } else if (key.isWritable()) {
              bf.writeToChannel(clientChannel);
              if (bf.readableBytes() == 0) {
                clientChannel.close();
                return;
              }
            }
          }
        }
      } catch (IOException e) {
        fail(e.getMessage());
      }
    });

    t1.start();
    t2.start();
    t1.join();
    t2.join();
  }
}
