import java.util.ArrayList;
import java.util.TreeSet;

public class LiftAllocationService {
    // cost given to a lift that can never serve the request
    // (pickup floor outside its floors, UP at its highest floor or DOWN at its lowest floor)
    static final int CANNOT_SERVE = Integer.MAX_VALUE;
    ArrayList<Lift> lifts = new ArrayList<>();

    public void addLift(Lift lift) {
        setDirection(lift);     // no next floors -> IDLE, even if the user gave UP / DOWN
        lifts.add(lift);
    }

    // keep the direction correct for the next floors
    public void setDirection(Lift lift) {
        Integer floorAbove = lift.nextFloors.higher(lift.currentFloor);
        Integer floorBelow = lift.nextFloors.lower(lift.currentFloor);

        if (floorAbove == null && floorBelow == null) {
            lift.direction = Direction.IDLE;
        } else if (lift.direction == Direction.IDLE) {
            // go to the nearest next floor
            if (floorBelow == null || (floorAbove != null && floorAbove - lift.currentFloor <= lift.currentFloor - floorBelow)) {
                lift.direction = Direction.UP;
            } else {
                lift.direction = Direction.DOWN;
            }
        } else if (lift.direction == Direction.UP && floorAbove == null) {
            lift.direction = Direction.DOWN;
        } else if (lift.direction == Direction.DOWN && floorBelow == null) {
            lift.direction = Direction.UP;
        }
    }

    // floors the lift travels before it opens its door at the pickup floor
    // the lift turns at its last (or first) next floor, it never goes further than that
    public int calculateCost(Lift lift, int pickupFloor, Direction requestDirection) {
        if (pickupFloor < lift.lowestfloor || pickupFloor > lift.highestfloor) return CANNOT_SERVE;
        if ((requestDirection == Direction.UP && pickupFloor == lift.highestfloor) ||
                (requestDirection == Direction.DOWN && pickupFloor == lift.lowestfloor)) return CANNOT_SERVE;

        int liftFloor = lift.currentFloor;
        if (lift.direction == Direction.IDLE) {
            return Math.abs(liftFloor - pickupFloor);
        }

        int highestNextFloor = lift.nextFloors.last();
        int lowestNextFloor = lift.nextFloors.first();

        if (lift.direction == Direction.UP) {
            if (requestDirection == Direction.UP) {
                if (pickupFloor >= liftFloor) return pickupFloor - liftFloor;
                int lowerTurnFloor = Math.min(lowestNextFloor, pickupFloor);
                return (highestNextFloor - liftFloor) + (highestNextFloor - lowerTurnFloor) + (pickupFloor - lowerTurnFloor);
            }
            int upperTurnFloor = Math.max(highestNextFloor, pickupFloor);
            return (upperTurnFloor - liftFloor) + (upperTurnFloor - pickupFloor);
        } else {
            if (requestDirection == Direction.DOWN) {
                if (pickupFloor <= liftFloor) return liftFloor - pickupFloor;
                int upperTurnFloor = Math.max(highestNextFloor, pickupFloor);
                return (liftFloor - lowestNextFloor) + (upperTurnFloor - lowestNextFloor) + (upperTurnFloor - pickupFloor);
            }
            int lowerTurnFloor = Math.min(lowestNextFloor, pickupFloor);
            return (liftFloor - lowerTurnFloor) + (pickupFloor - lowerTurnFloor);
        }
    }

    // number of next floors the lift stops at before it reaches the pickup floor (only the floors on its way)
    private int countStopsBeforePickup(Lift lift, int pickupFloor, Direction requestDirection) {
        if (lift.direction == Direction.IDLE) return 0;

        int liftFloor = lift.currentFloor;
        TreeSet<Integer> nextFloors = lift.nextFloors;
        int pickupFloorIsNextFloor = nextFloors.contains(pickupFloor) ? 1 : 0;   // the pickup floor itself is not counted

        if (lift.direction == Direction.UP) {
            if (requestDirection == Direction.UP) {
                if (pickupFloor >= liftFloor) {
                    return nextFloors.subSet(liftFloor, false, pickupFloor, false).size();
                }
                // goes up, comes down, then goes up to the pickup floor: stops at every next floor first
                return nextFloors.size() - pickupFloorIsNextFloor;
            }
            // goes up to the turning floor, then comes down to the pickup floor
            return nextFloors.tailSet(Math.min(liftFloor, pickupFloor), false).size()
                    - (pickupFloor > liftFloor ? pickupFloorIsNextFloor : 0);
        } else {
            if (requestDirection == Direction.DOWN) {
                if (pickupFloor <= liftFloor) {
                    return nextFloors.subSet(pickupFloor, false, liftFloor, false).size();
                }
                // goes down, goes up, then comes down to the pickup floor: stops at every next floor first
                return nextFloors.size() - pickupFloorIsNextFloor;
            }
            // goes down to the turning floor, then comes up to the pickup floor
            return nextFloors.headSet(Math.max(liftFloor, pickupFloor), false).size()
                    - (pickupFloor < liftFloor ? pickupFloorIsNextFloor : 0);
        }
    }

    // tie breaker: cost, no of stops before the pickup floor, idle, same direction, opposite, lift id
    private boolean isBetter(Lift lift, int cost, Lift bestLift, int bestCost, int pickupFloor, Direction requestDirection) {
        if (cost != bestCost) return cost < bestCost;
        int stops = countStopsBeforePickup(lift, pickupFloor, requestDirection);
        int bestStops = countStopsBeforePickup(bestLift, pickupFloor, requestDirection);
        if (stops != bestStops) return stops < bestStops;
        if (rank(lift, requestDirection) != rank(bestLift, requestDirection)) {
            return rank(lift, requestDirection) < rank(bestLift, requestDirection);
        }
        return lift.id < bestLift.id;
    }

    private int rank(Lift lift, Direction requestDirection) {
        if (lift.direction == Direction.IDLE) return 0;
        return lift.direction == requestDirection ? 1 : 2;
    }

    // every direction of the request gets its own best lift, so UP and DOWN can go to 2 different lifts
    public void allocateLift(Request request) {
        int pickupFloor = request.getPickupFloor();
        ArrayList<Direction> requestDirections = request.getDirection();
        Lift[] bestLifts = new Lift[requestDirections.size()];
        int[] bestCosts = new int[requestDirections.size()];

        for (int i = 0; i < requestDirections.size(); i++) {
            for (Lift lift : lifts) {
                int cost = calculateCost(lift, pickupFloor, requestDirections.get(i));
                if (cost != CANNOT_SERVE && (bestLifts[i] == null || isBetter(lift, cost, bestLifts[i], bestCosts[i], pickupFloor, requestDirections.get(i)))) {
                    bestLifts[i] = lift;
                    bestCosts[i] = cost;
                }
            }
        }

        // 2 different lifts, but one of them is equally good for both directions -> use that one lift
        if (requestDirections.size() == 2 && bestLifts[0] != null && bestLifts[1] != null && bestLifts[0] != bestLifts[1]) {
            if (calculateCost(bestLifts[0], pickupFloor, requestDirections.get(1)) == bestCosts[1]) {
                bestLifts[1] = bestLifts[0];
            } else if (calculateCost(bestLifts[1], pickupFloor, requestDirections.get(0)) == bestCosts[0]) {
                bestLifts[0] = bestLifts[1];
            }
        }

        // only one pickup request is processed, so the chosen lifts are not changed
        for (int i = 0; i < requestDirections.size(); i++) {
            if (bestLifts[i] == null) {
                System.out.println("No lift can serve floor " + pickupFloor + " going " + requestDirections.get(i));
            } else {
                System.out.println("Lift " + bestLifts[i].id + " assigned for floor " + pickupFloor + " going " + requestDirections.get(i));
            }
        }
    }
}
