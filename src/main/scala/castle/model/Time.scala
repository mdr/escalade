package castle.model
import java.util.Date
case class Time(ms: Long) extends Ordered[Time] {

  def +(seconds: Seconds) = Time(ms + seconds.seconds * 1000)
  
  def +(milliseconds: Milliseconds) = Time(ms + milliseconds.ms)
  
  def -(other: Time) = millisecondsDifference(other)
  
  def compare(other: Time) = this.ms.compare(other.ms)
  
  def secondsDifference(other: Time): Seconds = Seconds(((this.ms - other.ms) / 1000).intValue)
  
  def millisecondsDifference(other: Time): Milliseconds = Milliseconds((this.ms - other.ms).intValue)
  
}

object Time {
  def now(): Time = Time(System.currentTimeMillis())
}


case class Seconds(seconds: Int)

case class Milliseconds(ms: Int)
