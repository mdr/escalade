package matt.utils
import java.util.concurrent.locks._
object ThreadUtils {
  
  def doInNewThread(proc: => Unit) {
    val runnable = new Runnable() {
      override def run() { proc }
    }  
    new Thread(runnable).start()
  } 
  
   
  def withLock(mutex: Lock)(proc: => Unit) {
    mutex.lock();
    try {
      proc
    } finally {
      mutex.unlock()
    }
  }
  
}
