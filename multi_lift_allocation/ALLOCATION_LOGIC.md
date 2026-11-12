# Allocation logic

Names: pickupFloor = floor where the user waits, requestDirection = UP or DOWN (a request can have both),
liftFloor = lift's current floor, highestNextFloor / lowestNextFloor = last / first floor of the lift's nextFloors TreeSet,
upperTurnFloor = max(highestNextFloor, pickupFloor), lowerTurnFloor = min(lowestNextFloor, pickupFloor).
A lift only goes as far as its last / first next floor (or the pickup floor), never to the building's highest / lowest floor.

## Input
- Per lift: lowest floor < highest floor, current floor inside them, then the direction (UP/DOWN/IDLE).
- IDLE -> next floors are not asked. UP/DOWN -> next floors are asked (type done to finish).
- UP/DOWN but no next floor entered -> saved as IDLE (addLift -> setDirection).
- Negative floors (basement) are allowed. The next floor list ends with the word done.
- Only ONE pickup request is taken per run (floor inside the lowest..highest of all lifts, directions UP, DOWN or UP,DOWN).
  Served floors are never removed from nextFloors, so a second request would be calculated on old data.

## setDirection (when a lift is added)
- no next floor above or below -> IDLE
- IDLE with next floors -> go to the nearest one (tie -> UP)
- UP with nothing above -> DOWN, DOWN with nothing below -> UP

## Cost = floors travelled before the door opens at pickupFloor
CANNOT_SERVE (= Integer.MAX_VALUE, "infinite cost") when the pickup floor is outside the lift's floors,
or requestDirection is UP at the lift's highest floor, or DOWN at its lowest floor. Such a lift is skipped.

| Lift | requestDirection | Condition | Cost |
|---|---|---|---|
| IDLE | any | | abs(liftFloor - pickupFloor) |
| UP | UP | pickupFloor >= liftFloor | pickupFloor - liftFloor |
| UP | UP | pickupFloor < liftFloor | (highestNextFloor-liftFloor) + (highestNextFloor-lowerTurnFloor) + (pickupFloor-lowerTurnFloor) |
| UP | DOWN | | (upperTurnFloor-liftFloor) + (upperTurnFloor-pickupFloor) |
| DOWN | DOWN | pickupFloor <= liftFloor | liftFloor - pickupFloor |
| DOWN | DOWN | pickupFloor > liftFloor | (liftFloor-lowestNextFloor) + (upperTurnFloor-lowestNextFloor) + (upperTurnFloor-pickupFloor) |
| DOWN | UP | | (liftFloor-lowerTurnFloor) + (pickupFloor-lowerTurnFloor) |

## Choosing
1. Every direction of the request is allocated on its own.
2. Best lift per direction: lower cost, fewer stops before the pickup floor, IDLE, same direction, opposite direction, lower lift id.
   Stops before the pickup floor = only the next floors on the lift's way to the pickup floor (not the ones behind it, not its own floor,
   not the pickup floor itself). If the lift has to turn around first, the next floors of the whole sweep count.
   Example: lift at 4 going UP, next floors 2,3,4,5, pickup at 6 -> only floor 5 counts.
3. UP and DOWN got different lifts -> if one of them is equally good for the other direction too, use that one lift. Otherwise two lifts.
4. Print the assigned lift for each direction. The lifts are not changed (single request).
