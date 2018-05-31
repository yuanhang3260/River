package buffer;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.ReadOnlyBufferException;
import org.apache.log4j.Logger;

// This class is wrapper of java.nio.ByteBuffer. It supports flexible read and write operation,
// and no flip() is needed. Capacity is auto incremented when more space is needed.
//
// Internally, this buffer keeps two indexes for readings and writings separately.
//
// This class is NOT thread-safe.
public class ByteBuf {
  private static final int DEFAULT_CAPACITY = 64;
  private static final int CAPACITY_INC_THRESHOLD = 4194304;

  private static final Logger log = Logger.getLogger(ByteBuf.class);

  private ByteBuffer internal;
  private int capacity = 0;
  private int writeIndex = 0;

  private int mark = -1;

  private ByteBuf() {
    this.capacity = DEFAULT_CAPACITY;
    this.internal = ByteBuffer.allocate(this.capacity);
  }

  public static ByteBuf alloc() {
    return new ByteBuf();
  }

  public void clear() {
    this.capacity = DEFAULT_CAPACITY;
    this.internal = ByteBuffer.allocate(this.capacity);
    this.writeIndex = 0;
    this.mark = -1;
  }

  public int readableBytes() {
    return writeIndex - internal.position();
  }

  public int markReadIndex() {
    this.mark = internal.position();
    return this.mark;
  }

  public boolean resetReadIndex() {
    if (this.mark >= 0) {
      internal.position(this.mark);
      this.mark = -1;
      return true;
    }
    return false;
  }

  public byte get() throws BufferUnderflowException {
    if (readableBytes() < 1) {
      log.error("No readable byte in the buffer");
      throw new BufferUnderflowException();
    }
    return internal.get();
  }

  public ByteBuf get(byte[] dst) throws BufferUnderflowException {
    int readable = readableBytes();
    if (readable < dst.length) {
      log.error("buffer readableBytes = " + readable + ", but trying to read " + dst.length);
      throw new BufferUnderflowException();
    }
    internal.get(dst);
    return this;
  }

  public ByteBuf get(byte[] dst, int offset, int length)
      throws IndexOutOfBoundsException, BufferUnderflowException {
    int readable = readableBytes();
    if (readable < length) {
      log.error("buffer readableBytes = " + readable + ", but trying to read " + length);
      throw new BufferUnderflowException();
    }
    internal.get(dst, offset, length);
    return this;
  }

  public char getChar() throws BufferUnderflowException {
    int readable = readableBytes();
    if (readable < 2) {
      log.error("buffer readableBytes = " + readable + ", but trying to read char of length 2");
      throw new BufferUnderflowException();
    }
    return internal.getChar();
  }

  public double getDouble() throws BufferUnderflowException {
    int readable = readableBytes();
    if (readable < 8) {
      log.error("buffer readableBytes = " + readable + ", but trying to read double of length 8");
      throw new BufferUnderflowException();
    }
    return internal.getDouble();
  }

  public float getFloat() throws BufferUnderflowException {
    int readable = readableBytes();
    if (readable < 4) {
      log.error("buffer readableBytes = " + readable + ", but trying to read float of length 4");
      throw new BufferUnderflowException();
    }
    return internal.getFloat();
  }

  public int getInt() throws BufferUnderflowException {
    int readable = readableBytes();
    if (readable < 4) {
      log.error("buffer readableBytes = " + readable + ", but trying to read int of length 4");
      throw new BufferUnderflowException();
    }
    return internal.getInt();
  }

  public long getLong() throws BufferUnderflowException {
    int readable = readableBytes();
    if (readable < 8) {
      log.error("buffer readableBytes = " + readable + ", but trying to read long of length 8");
      throw new BufferUnderflowException();
    }
    return internal.getLong();
  }

  public short getShort() throws BufferUnderflowException {
    int readable = readableBytes();
    if (readable < 2) {
      log.error("buffer readableBytes = " + readable + ", but trying to read int of length 2");
      throw new BufferUnderflowException();
    }
    return internal.getShort();
  }  

  public ByteBuf put(byte b) throws BufferOverflowException {
    ensureWritable(1);
    try {
      internal.put(writeIndex++, b);
    } catch (ReadOnlyBufferException e) {
      e.printStackTrace();
    }
    return this;
  }

  public ByteBuf put(byte[] src) throws BufferOverflowException {
    ensureWritable(src.length);
    try {
      for (int i = 0; i < src.length; i++) {
        internal.put(writeIndex++, src[i]);
      }
    } catch (ReadOnlyBufferException e) {
      e.printStackTrace();
    }
    return this;
  }

  // This method checks length, and ensures the src byte[] does not overflow.
  public ByteBuf put(byte[] src, int offset, int length)
      throws BufferOverflowException, IndexOutOfBoundsException {
    int l = Math.min(length, src.length - offset);
    ensureWritable(l);

    try {
      for (int i = offset; i < offset + l; i++) {
        internal.put(writeIndex++, src[i]);
      }
    } catch (ReadOnlyBufferException e) {
      e.printStackTrace();
    }
    return this;
  }

  public ByteBuf put(ByteBuffer buf) {
    int dataLength = buf.limit() - buf.position();  // readable bytes
    // Copy data from tmp to internal ByteBuffer.
    ensureWritable(dataLength);
    markReadIndex();
    internal.position(writeIndex);
    internal.put(buf);
    writeIndex += dataLength;
    resetReadIndex();
    return this;
  }

  public ByteBuf put(ByteBuf other) {
    int dataLength = other.readableBytes();

    ByteBuffer data = other.internal;
    data.limit(other.writeIndex);

    ensureWritable(dataLength);
    markReadIndex();
    internal.position(writeIndex);
    internal.put(data);
    writeIndex += dataLength;
    resetReadIndex();

    other.clear();
    return this;
  }

  public ByteBuf putChar(char value) throws BufferOverflowException {
    ensureWritable(2);
    try {
      internal.putChar(writeIndex, value);
    } catch (ReadOnlyBufferException e) {
      e.printStackTrace();
    }
    writeIndex += 2;
    return this;
  }

  public ByteBuf putDouble(double value) throws BufferOverflowException {
    ensureWritable(8);
    try {
      internal.putDouble(writeIndex, value);
    } catch (ReadOnlyBufferException e) {
      e.printStackTrace();
    }
    writeIndex += 8;
    return this;
  }

  public ByteBuf putFloat(float value) throws BufferOverflowException {
    ensureWritable(4);
    try {
      internal.putFloat(writeIndex, value);
    } catch (ReadOnlyBufferException e) {
      e.printStackTrace();
    }
    writeIndex += 4;
    return this;
  }

  public ByteBuf putInt(int value) throws BufferOverflowException {
    ensureWritable(4);
    try {
      internal.putInt(writeIndex, value);
    } catch (ReadOnlyBufferException e) {
      e.printStackTrace();
    }
    writeIndex += 4;
    return this;
  }

  public ByteBuf putLong(long value) throws BufferOverflowException {
    ensureWritable(8);
    try {
      internal.putLong(writeIndex, value);
    } catch (ReadOnlyBufferException e) {
      e.printStackTrace();
    }
    writeIndex += 8;
    return this;
  }

  public ByteBuf putShort(short value) throws BufferOverflowException {
    ensureWritable(2);
    try {
      internal.putShort(writeIndex, value);
    } catch (ReadOnlyBufferException e) {
      e.printStackTrace();
    }
    writeIndex += 2;
    return this;
  }

  public int readFromChannel(SocketChannel channel) {
    int totalBytesRead = 0;
    ByteBuffer tmp = ByteBuffer.allocate(1024);
    while (true) {
      int readLength = 0;
      try {
        readLength = channel.read(tmp);
        if (readLength == 0) {
          break;
        }
        if (readLength < 0) {
          if (totalBytesRead == 0) {
            // Reach EOF, channel is close by the other end.
            return -1;
          }
          return totalBytesRead;
        }
      } catch (IOException e) {
        e.printStackTrace();
        break;
      }
      tmp.flip();

      // Copy data from tmp to internal ByteBuffer.
      put(tmp);
      tmp.clear();
      totalBytesRead += readLength;
    }
    return totalBytesRead;
  }

  public int writeToChannel(SocketChannel channel) {
    try {
      internal.limit(writeIndex);
      return channel.write(internal);
    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    } finally {
      internal.limit(capacity);
    }
  }

  private void ensureWritable(int needLength) {
    if (writeIndex + needLength <= capacity) {
      return;
    }

    int newCapacity = calculateNewCapacity(needLength);
    ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);

    // Copy remaining data to new buffer.
    internal.limit(writeIndex);
    newBuffer.put(internal);
    this.writeIndex = newBuffer.position();
    newBuffer.position(0);
    newBuffer.limit(newCapacity);

    this.internal = newBuffer;
    this.capacity = newCapacity;
  }

  private int calculateNewCapacity(int needLength) {
    int total = readableBytes() + needLength;
    int newCapacity = 0;
    if (total < CAPACITY_INC_THRESHOLD) {
      newCapacity = DEFAULT_CAPACITY;
      // Double the capacity.
      while (newCapacity < total) {
        newCapacity <<= 1;
      }
    } else {
      // Just increase the capacity by 4MB every time.
      newCapacity = total / CAPACITY_INC_THRESHOLD * CAPACITY_INC_THRESHOLD;
      newCapacity += CAPACITY_INC_THRESHOLD;
    }
    return newCapacity;
  }
}
