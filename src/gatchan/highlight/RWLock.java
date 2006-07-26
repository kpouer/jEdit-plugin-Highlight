package gatchan.highlight;

import org.gjt.sp.util.Log;

/**
 * @author Matthieu Casanova
 * @version $Id: RWLock.java,v 1.1 2005/05/29 22:48:45 kpouer Exp $
 */
public class RWLock {
  private int givenLocks;
  private int waitingWriters;

  private final Object mutex;


  public RWLock() {
    mutex = new Object();
    givenLocks = 0;
    waitingWriters = 0;
  }

  public void getReadLock() {
    synchronized (mutex) {
      try {
        while ((givenLocks == -1) || (waitingWriters != 0)) {
          mutex.wait();
        }
      }
      catch (java.lang.InterruptedException e) {
        Log.log(Log.ERROR,this,e);
      }
      givenLocks++;
    }
  }

  public void getWriteLock() {
    synchronized (mutex) {
      waitingWriters++;
      try {
        while (givenLocks != 0) {
          mutex.wait();
        }
      }
      catch (java.lang.InterruptedException e) {
        Log.log(Log.ERROR,this,e);
      }

      waitingWriters--;
      givenLocks = -1;

    }
  }


  public void releaseLock() {
    synchronized (mutex) {
      if (givenLocks == 0)
        return;

      if (givenLocks == -1)
        givenLocks = 0;
      else
        givenLocks--;

      mutex.notifyAll();
    }
  }


}



