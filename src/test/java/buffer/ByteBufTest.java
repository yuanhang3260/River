package buffer;

import static org.junit.Assert.*;
import org.junit.Test;

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
}
