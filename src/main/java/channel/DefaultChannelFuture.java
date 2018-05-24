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
}
