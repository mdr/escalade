package castle.model
import matt.utils.CollectionUtils._

case class RegionOwnerInfo(regionOwnerMap: Map[Region, Player]) extends Iterable[(Region, Player)] {
  def allRegions: Set[Region] = set(regionOwnerMap.keySet)
  
  def iterator = regionOwnerMap.iterator

  def regionsOwnedBy(player: Player): Set[Region] = 
    for {
      (region, owner) <- regionOwnerMap
      if player == owner
    } yield region
    
  def owner(region: Region): Player = regionOwnerMap(region)
  
}
