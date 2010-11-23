package castle.model
import matt.utils.BoneHeadHashCode 

abstract sealed class Direction(val rowDelta: Int, val columnDelta: Int)  

case object North extends Direction(-1, 0) with BoneHeadHashCode
case object East extends Direction(0, 1) with BoneHeadHashCode
case object South extends Direction(1, 0) with BoneHeadHashCode
case object West extends Direction(0, -1) with BoneHeadHashCode	
 
