package channel;

import multithread.IPromise;

public interface ChannelPromise extends IPromise<Void> {
  boolean setSuccess();
}
