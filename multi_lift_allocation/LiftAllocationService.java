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
    private Lift findBestLift(int pickupFloor, Direction requestDirection) {
        Lift bestLift = null;
        int bestCost = 0;
        for (Lift lift : lifts) {
            int cost = calculateCost(lift, pickupFloor, requestDirection);
            if (bestLift == null || tieBreaker(lift, cost, bestLift, bestCost, pickupFloor, requestDirection)) {
                bestLift = lift;
                bestCost = cost;
            }
        }
        return bestLift;
    }

    //doubt
    public void allocateLift(Request request) {
        int pickupFloor = request.getPickupFloor();
        ArrayList<Direction> requestDirections = request.getDirection();

        Lift[] bestLifts = new Lift[requestDirections.size()];
        for (int i = 0; i < bestLifts.length; i++) {
            bestLifts[i] = findBestLift(pickupFloor, requestDirections.get(i));
        }

        // UP and DOWN got different lifts, but one of them is equally good for the other direction too -> use that one lift
        if (bestLifts.length == 2 && bestLifts[0] != bestLifts[1]) {
            Direction first = requestDirections.get(0);
            Direction second = requestDirections.get(1);
            if (calculateCost(bestLifts[0], pickupFloor, second) == calculateCost(bestLifts[1], pickupFloor, second)) {
                bestLifts[1] = bestLifts[0];
            } else if (calculateCost(bestLifts[1], pickupFloor, first) == calculateCost(bestLifts[0], pickupFloor, first)) {
                bestLifts[0] = bestLifts[1];
            }
        }

        for (int i = 0; i < bestLifts.length; i++) {
            System.out.println("Lift " + bestLifts[i].id + " assigned for floor " + pickupFloor + " going " + requestDirections.get(i));
        }
    }
}
