package channel;

import channel.ChannelFuture;
import multithread.IFuture;
import multithread.IFutureListener;

public interface ChannelFutureListener extends IFutureListener<Void> {
  default void taskDone(IFuture<Void> future) throws Exception {
    taskDone((ChannelFuture)future);
  }

  void taskDone(ChannelFuture future) throws Exception;
}
