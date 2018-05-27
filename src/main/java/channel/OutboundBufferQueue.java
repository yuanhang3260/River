package channel;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import buffer.ByteBuf;
import channel.BaseChannel;
import channel.ChannelExceptions;
import channel.ChannelPromise;

public class OutboundBufferQueue {
  private Queue<ByteBuf> bufQueue = new LinkedList<ByteBuf>();
  private Map<ByteBuf, LinkedList<ChannelPromise>> flushRequests =
      new HashMap<ByteBuf, LinkedList<ChannelPromise>>();

  public boolean isEmpty() {
    return bufQueue.isEmpty();
  }

  public void enqueueOutputBuf(ByteBuf buf) {
    bufQueue.offer(buf);
  }

  public void addFlush(ChannelPromise promise) {
    ByteBuf last = ((LinkedList<ByteBuf>)bufQueue).getLast();
    LinkedList<ChannelPromise> waitingFlushes = flushRequests.get(last);
    if (waitingFlushes == null) {
      waitingFlushes = new LinkedList<ChannelPromise>();
      flushRequests.put(last, waitingFlushes);
    }
    waitingFlushes.offer(promise);
  }

  public void flushToChannel(SocketChannel channel) throws ChannelExceptions.UnexpectedException {
    while (!bufQueue.isEmpty()) {
      ByteBuf buf = bufQueue.peek();
      int pendingBytes = buf.readableBytes();
      int writeLen = buf.writeToChannel(channel);
      if (writeLen == pendingBytes) {
        // This output buffer is flushed to channel, set promises.
        bufQueue.poll();
        LinkedList<ChannelPromise> promises = flushRequests.get(buf);
        if (promises != null) {
          for (ChannelPromise promise : promises) {
            promise.setSuccess();
          }
        }
        flushRequests.remove(buf);
      } else if (writeLen < pendingBytes) {
        break;
      } else {
        // Should never happen.
        throw new ChannelExceptions.UnexpectedException(
            "ByteBuf flushed more than available bytes to SocketChannel.");
      }
    }
  }
}
