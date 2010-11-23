package matt.utils
import CollectionUtils._

class MaximalElementFinder[T](partialOrdering: PartialOrdering[T]) {

  def findMaximalElements(iterable: Iterable[T]): Set[T] = {
    var remaining = set(iterable)
    var maximalElements = Set.empty : Set[T] 
    while (!remaining.isEmpty) {
      var maximalSoFar = getArbitraryElement(remaining)
      
      var toRemove = Set(maximalSoFar)
      var continueLoop = true
      while (continueLoop) {
        var redoLoop = false
        for (element <- remaining) {
          if (partialOrdering.gteq(element, maximalSoFar)) {
            maximalSoFar = element
            toRemove += element
            redoLoop = true
          } else if (partialOrdering.lt(element, maximalSoFar)) {
            toRemove += element
          } 
        }
        remaining --= toRemove
        toRemove = Set()
        continueLoop = redoLoop
      }
      maximalElements += maximalSoFar
    }
    maximalElements
  }
  
}
