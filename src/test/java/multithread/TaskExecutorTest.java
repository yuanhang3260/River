package multithread;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import multithread.TaskExecutor;
import multithread.IFuture;

public class TaskExecutorTest {
  private static int TASKS_NUM = 30;
  private static long CALCULATE_RANGE = 10000000;

  private long[] results = new long[TASKS_NUM];

  private TaskExecutor executor;

  private long calcualate(int index) {
    long sum = 0;
    for (long i = 1; i <= CALCULATE_RANGE; i++) {
      sum += i;
    }
    this.results[index] = sum;
    return sum;
  }

  @Before
  public void setup() throws Exception {
    executor = new TaskExecutor();
  }

  @After
  public void tearDown() throws Exception {
    executor.stop();
    executor.awaitTermination();
  }

  @Test
  public void testExecuteRunnable() throws InterruptedException, ExecutionException {
    for (int i = 0; i < TASKS_NUM; i++) {
      int index = i;
      IFuture<?> future = executor.submit(() -> { calcualate(index); });
      assertNull(future.get());
    }

    long expected = CALCULATE_RANGE * (CALCULATE_RANGE + 1) / 2;
    for (int i = 0; i < TASKS_NUM; i++) {
      assertEquals(expected, results[i]);
    }
  }

  @Test
  public void testExecuteCallable() throws InterruptedException, ExecutionException {
    long expected = CALCULATE_RANGE * (CALCULATE_RANGE + 1) / 2;
    for (int i = 0; i < TASKS_NUM; i++) {
      int index = i;
      IFuture<Long> future = executor.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          return calcualate(index);
        }
      });
      assertEquals(expected, (long)future.get());
    }
  }

  @Test
  public void testExecuteAfterCancel() throws InterruptedException, ExecutionException {
    IFuture<Long> future = executor.submit(new Callable<Long>() {
      @Override
      public Long call() throws Exception {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace(); 
        }
        return calcualate(0);
      }
    });

    future.cancel();
    assertTrue(future.isCancelled());
    assertTrue(future.getCause() instanceof CancellationException);
  }
}
