package channel;

import multithread.IFuture;

public interface ChannelFuture extends IFuture<Void> {
  void sync() throws Exception;
}
