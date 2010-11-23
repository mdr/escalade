package matt.utils

object TimeUtils {
  
  def time[Result](message: String)(f: => Result): Result = {
    val start = System.nanoTime
    val result: Result = f
    val end = System.nanoTime
    if (false)
      println(message + ": " + (end - start )/ 1000000 + " ms")
    result
  }

}
