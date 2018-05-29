package channel;

import channel.ChannelFuture;
import channel.ChannelPromise;
import multithread.AbstractFuture;

public class DefaultChannelFuture
    extends AbstractFuture<Void> implements ChannelFuture, ChannelPromise {

  @Override
  public boolean setSuccess() {
    return this.setSuccess(null);
  }

  @Override
  public void sync() throws Exception {
    try {
      this.get();
    } catch (Exception e) {
      throw e;
    }
  }
}
