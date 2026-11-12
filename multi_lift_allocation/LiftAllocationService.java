import java.util.ArrayList;
import java.util.TreeSet;

public class LiftAllocationService {
    ArrayList<Lift> lifts = new ArrayList<>();

    public void addLift(Lift lift) {
        setDirection(lift);
        lifts.add(lift);
    }

    // The direction the user gave may not match the next floors, so correct it:
    // no next floors -> nothing to do -> IDLE
    // UP but no next floor above (or DOWN but none below) -> it has to go the other way
    public void setDirection(Lift lift) {
        if (lift.nextFloors.isEmpty()) {
            lift.direction = Direction.IDLE;
        } else if (lift.direction == Direction.UP && lift.nextFloors.higher(lift.currentFloor) == null) {
            lift.direction = Direction.DOWN;
        } else if (lift.direction == Direction.DOWN && lift.nextFloors.lower(lift.currentFloor) == null) {
            lift.direction = Direction.UP;
        }
    }

    // a lift serves the pickup only if the floor is in its floors and it can go in the requested direction from there
    public boolean canServe(Lift lift, int pickupFloor, Direction requestDirection) {
        if (pickupFloor < lift.lowestfloor || pickupFloor > lift.highestfloor) return false;
        if (requestDirection == Direction.UP) return pickupFloor < lift.highestfloor;
        return pickupFloor > lift.lowestfloor;
    }

    // floors the lift travels before it opens its door at the pickup floor
    // the lift turns at its last (or first) next floor, it never goes further than that
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

    // number of next floors the lift stops at before it reaches the pickup floor (only the floors on its way)
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
                return stops.size();    // goes up, comes down, goes up again: stops at every next floor first
            }
            return stops.tailSet(Math.min(liftFloor, pickupFloor), false).size();   // goes up to the turn, comes down to the pickup
        } else {
            if (requestDirection == Direction.DOWN) {
                if (pickupFloor <= liftFloor) {
                    return stops.subSet(pickupFloor, false, liftFloor, false).size();
                }
                return stops.size();    // goes down, goes up, comes down again: stops at every next floor first
            }
            return stops.headSet(Math.max(liftFloor, pickupFloor), false).size();   // goes down to the turn, comes up to the pickup
        }
    }

    // 0 = idle, 1 = same direction as the request, 2 = opposite
    private int rank(Lift lift, Direction requestDirection) {
        if (lift.direction == Direction.IDLE) return 0;
        return lift.direction == requestDirection ? 1 : 2;
    }

    // tie breaker: cost, no of stops before the pickup floor, idle, same direction, opposite, lift id
    private boolean isBetter(Lift lift, int cost, Lift bestLift, int bestCost, int pickupFloor, Direction requestDirection) {
        if (cost != bestCost) return cost < bestCost;

        int stops = countStopsBeforePickup(lift, pickupFloor, requestDirection);
        int bestStops = countStopsBeforePickup(bestLift, pickupFloor, requestDirection);
        if (stops != bestStops) return stops < bestStops;

        int rank = rank(lift, requestDirection);
        int bestRank = rank(bestLift, requestDirection);
        if (rank != bestRank) return rank < bestRank;

        return lift.id < bestLift.id;
    }

    // goes through every lift that can serve the pickup and keeps the best one found so far
    private Lift findBestLift(int pickupFloor, Direction requestDirection) {
        Lift bestLift = null;
        int bestCost = 0;
        for (Lift lift : lifts) {
            if (!canServe(lift, pickupFloor, requestDirection)) continue;

            int cost = calculateCost(lift, pickupFloor, requestDirection);
            if (bestLift == null || isBetter(lift, cost, bestLift, bestCost, pickupFloor, requestDirection)) {
                bestLift = lift;
                bestCost = cost;
            }
        }
        return bestLift;
    }

    // a request has 1 direction (UP or DOWN) or 2 (UP and DOWN), every direction gets its own best lift
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
            if (canServe(bestLifts[0], pickupFloor, second)
                    && calculateCost(bestLifts[0], pickupFloor, second) == calculateCost(bestLifts[1], pickupFloor, second)) {
                bestLifts[1] = bestLifts[0];
            } else if (canServe(bestLifts[1], pickupFloor, first)
                    && calculateCost(bestLifts[1], pickupFloor, first) == calculateCost(bestLifts[0], pickupFloor, first)) {
                bestLifts[0] = bestLifts[1];
            }
        }

        for (int i = 0; i < bestLifts.length; i++) {
            System.out.println("Lift " + bestLifts[i].id + " assigned for floor " + pickupFloor + " going " + requestDirections.get(i));
        }
    }
}
