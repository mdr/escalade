package matt.utils
import scala.util.Random

class RandomUtils(private val random: Random) {
  
  def randomElementOf[T] (collection: Iterable[T]): T = {
    val collectionAsAList = collection.toList
    collectionAsAList(random.nextInt(collectionAsAList.size))
  }

}
 
object RandomUtils {
  
  implicit def randomToRandomUtils(random: Random) = new RandomUtils(random)
  
}
