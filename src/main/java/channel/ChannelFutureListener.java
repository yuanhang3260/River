package channel;

import channel.ChannelFuture;
import multithread.IFuture;
import multithread.IFutureListener;

public class ChannelFutureListener implements IFutureListener<Void> {
  public void taskDone(IFuture<Void> future) throws Exception {
    taskDone((ChannelFuture)future);
  }

  public void taskDone(ChannelFuture future) throws Exception {}
}
