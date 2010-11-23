package matt.utils

object CollectionUtils {
   def getArbitraryElement[T](iterable: Iterable[T]): T = iterable.elements.next
   def getFirstElement[T](iterable: Iterable[T]): T = iterable.elements.next
   def getOnlyElement[T](iterable: Iterable[T]): T = {
     val iterator = iterable.elements
     val element = iterator.next
     if (iterator.hasNext)
       throw new IllegalArgumentException("More than one element in " + iterable)
     return element
   }
   
   def set[T](iterable: Iterable[T]): Set[T] = Set.empty ++ iterable
   def map[K, V](iterable: Iterable[(K, V)]): Map[K, V] = Map() ++ iterable
   
   def iterableToOption[T](iterable: Iterable[T]): Option[T] = {
     if (iterable.elements.hasNext)
       Some(getOnlyElement(iterable))
     else
       None
   } 
   
   def not(x: Boolean)= !x
   
   def or(bs: Iterable[Boolean]): Boolean = bs.foldLeft(false)(_ || _)
     
   class RichBoolean(boolean: => Boolean) {
     def and(anotherBoolean: => Boolean) = boolean && anotherBoolean
     def or(anotherBoolean: => Boolean) = boolean || anotherBoolean
   }
   implicit def boolean2richBoolean(boolean: => Boolean): RichBoolean = new RichBoolean(boolean)
   
   implicit def iterable2Set[T](iterable: Iterable[T]): Set[T] = Set.empty ++ iterable
    
   class ContainedInWrapper[T](value: T) {
     def isEither(items: Any*) = items.contains(value) 
     def isOneOf(items: Any*) = items.contains(value) 
     def isNeither(items: Any*) = !items.contains(value) 
     def containedIn(items: Iterable[Any]) = items.contains(value)
   }
   implicit def item2containedInWrapper[T](item: T): ContainedInWrapper[T] = new ContainedInWrapper[T](item)
   
   def makeMapFromPairs[K, V](pairs: Iterable[(K, V)], combiner: (V, V) => V): Map[K, V] = {
     var result: Map[K, V] = Map.empty
     for ((key, value) <- pairs) {
       val updateValue = 
         if (result.contains(key)) 
           combiner(result(key), value)
         else 
           value
       result = result + (key -> updateValue)
     }
     result
   } 
   
   def max[A <% Ordered[A]](first: A, second: A): A = if (first >= second) first else second
   def min[A <% Ordered[A]](first: A, second: A): A = if (first <= second) first else second
   def sum(items: Iterable[Int]): Int = items.foldLeft(0)(_ + _)
   
   class IntIterable(iterable: Iterable[Int]) {
     def sum = CollectionUtils.sum(iterable)
   }
   implicit def iterable2IntIterable(iterable: Iterable[Int]): IntIterable = new IntIterable(iterable)
   
}
