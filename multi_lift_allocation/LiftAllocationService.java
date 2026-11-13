import java.util.ArrayList;
import java.util.TreeSet;

public class LiftAllocationService {
    ArrayList<Lift> lifts = new ArrayList<>();
    // saves the lift in the list, after correcting its direction
    public void addLift(Lift lift) {
        setDirection(lift);
        lifts.add(lift);
    }

    // the direction the user typed may not match the lift's next floors, so correcting it
    public void setDirection(Lift lift) {
        if (lift.nextFloors.isEmpty()) {
            lift.direction = Direction.IDLE;
        } else if (lift.direction == Direction.UP && lift.nextFloors.higher(lift.currentFloor) == null) {
            lift.direction = Direction.DOWN;
        } else if (lift.direction == Direction.DOWN && lift.nextFloors.lower(lift.currentFloor) == null) {
            lift.direction = Direction.UP;
        }
    }

    public int calculateCost(Lift lift, int pickupFloor, Direction requestDirection) {
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

    // no of next floors the lift stops at before it reaches the pickup floor
    private int countStopsBeforePickup(Lift lift, int pickupFloor, Direction requestDirection) {
        if (lift.direction == Direction.IDLE) return 0;

        TreeSet<Integer> stops = new TreeSet<>(lift.nextFloors);
        stops.remove(pickupFloor);      // the pickup floor itself is not a stop before the pickup
        int liftFloor = lift.currentFloor;

        if (lift.direction == Direction.UP) {
            if (requestDirection == Direction.UP) {
                if (pickupFloor >= liftFloor) {
                    return stops.subSet(liftFloor, false, pickupFloor, false).size();
                }
                return stops.size();
            }
            return stops.tailSet(Math.min(liftFloor, pickupFloor), false).size();
        } else {
            if (requestDirection == Direction.DOWN) {
                if (pickupFloor <= liftFloor) {
                    return stops.subSet(pickupFloor, false, liftFloor, false).size();
                }
                return stops.size();
            }
            return stops.headSet(Math.max(liftFloor, pickupFloor), false).size();
        }
    }

    // in tie breaker when req counts are same we rank the lift w.r.t its direction
    private int rank(Lift lift, Direction requestDirection) {
        if (lift.direction == Direction.IDLE) return 0;
        return lift.direction == requestDirection ? 1 : 2;
    }

    private boolean tieBreaker(Lift lift, int cost, Lift bestLift, int bestCost, int pickupFloor, Direction requestDirection) {
        if (cost != bestCost) return cost < bestCost;

        int stops = countStopsBeforePickup(lift, pickupFloor, requestDirection);
        int bestStops = countStopsBeforePickup(bestLift, pickupFloor, requestDirection);
        if (stops != bestStops) return stops < bestStops;

        int rank = rank(lift, requestDirection);
        int bestRank = rank(bestLift, requestDirection);
        if (rank != bestRank) return rank < bestRank;

        return lift.id < bestLift.id;
    }

    //check current lift is better then bestlift and updated acc.
    private Lift findBestLift(ArrayList<Lift> availableLifts, int pickupFloor, Direction requestDirection) {
    Lift bestLift = null;
    int bestCost = 0;
    for (Lift lift : availableLifts) {
        int cost = calculateCost(lift, pickupFloor, requestDirection);
        if (bestLift == null || isBetter(lift, cost, bestLift, bestCost, pickupFloor, requestDirection)) {
            bestLift = lift;
            bestCost = cost;
        }
    }
    return bestLift;
    }

    //doubt
    public void allocateLift(Request request) {
    int pickupFloor = request.getPickupFloor();
    ArrayList<Lift> availableLifts = new ArrayList<>(lifts);   // a copy, the real list is not changed

    for (Direction requestDirection : request.getDirection()) {
        Lift bestLift = findBestLift(availableLifts, pickupFloor, requestDirection);
        if (availableLifts.size() > 1) {
            availableLifts.remove(bestLift);    // this lift is now busy with the first passenger
        }
        System.out.println("Lift " + bestLift.id + " assigned for floor " + pickupFloor + " going " + requestDirection);
    }
    }
}
