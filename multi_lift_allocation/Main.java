import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        LiftAllocationService allocationService = new LiftAllocationService();

        int noOfLifts = 0;
        while (true) {
            try {
                System.out.print("Enter number of lifts: ");
                noOfLifts = scanner.nextInt();
                if (noOfLifts > 0) {
                    break;
                }
                System.out.println("Number of lifts should be greater than 0");
            } catch (InputMismatchException e) {
                System.out.println("Enter a whole number");
                scanner.nextLine();
            }
        }

        int lowestFloor = 0, highestFloor = 0, currentFloor = 0;
        while (true) {
            try {
                System.out.print("Enter lowest floor: ");
                lowestFloor = scanner.nextInt();
                System.out.print("Enter highest floor: ");
                highestFloor = scanner.nextInt();
                if (highestFloor <= lowestFloor) {
                    System.out.println("highest floor should not be less than or equal to lowest floor");
                    continue;
                }
                break;
            } catch (InputMismatchException e) {
                System.out.println("Enter valid floor number");
                scanner.nextLine();
            }
        }

        for (int liftNumber = 1; liftNumber <= noOfLifts; liftNumber++) {
            System.out.println("\nLift " + liftNumber);
            while (true) {
                try {
                    System.out.print("Enter current floor: ");
                    currentFloor = scanner.nextInt();
                    if (currentFloor < lowestFloor || currentFloor > highestFloor) {
                        System.out.println("Enter current floor from " + lowestFloor + " to " + highestFloor);
                    } else {
                        break;
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Enter valid floor number");
                    scanner.nextLine();
                }
            }

            Lift lift = new Lift(liftNumber, currentFloor, Direction.IDLE);
            while (true) {
                try {
                    System.out.print("Enter direction of the lift (UP/DOWN/IDLE): ");
                    lift.direction = Direction.valueOf(scanner.next().trim().toUpperCase());
                    break;
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid direction. Use UP or DOWN or IDLE.");
                }
            }

            // idle lift will not have next floors
            if (lift.getDirection() != Direction.IDLE) {
                while (true) {
                    System.out.print("Enter next floor (type done to finish): ");
                    try {
                        String input = scanner.next();
                        if (input.equalsIgnoreCase("done")) {
                            break;
                        }
                        int nextFloor = Integer.parseInt(input);
                        if (nextFloor >= lowestFloor && nextFloor <= highestFloor && nextFloor != currentFloor) {
                            lift.nextFloors.add(nextFloor);
                        } else {
                            throw new IllegalArgumentException();
                        }
                    } catch (NumberFormatException e) {
                        scanner.nextLine();
                        System.out.println("Enter a whole number for the floor, or type done");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Enter valid floor from " + lowestFloor + " to " + highestFloor + " and it should not be equal to current floor");
                    }
                }
            }

            allocationService.addLift(lift);
        }

        int pickupFloor = 0;
        Request request = null;
        while (true) {

            System.out.print("\nEnter pickup floor: ");
            try {
                pickupFloor = scanner.nextInt();
                if (pickupFloor >= lowestFloor && pickupFloor <= highestFloor) {
                    request = new Request(pickupFloor);
                    break;
                } else {
                    throw new IllegalArgumentException();
                }
            } catch (InputMismatchException e) {
                scanner.nextLine();
                System.out.println("Enter a whole number for the floor");
                continue;
            } catch (IllegalArgumentException e) {
                System.out.println("Enter valid floor from " + lowestFloor + " to " + highestFloor);
                continue;
            }
        }
        scanner.nextLine();


        boolean cond=true;
        while (cond) {
            System.out.print("Enter direction (UP/DOWN, comma-separated if both): ");
            String directions = scanner.nextLine();
            String[] directionValues = directions.split(",");
            int c=0;
            for (String value : directionValues) {
                try {
                    if (value.trim().toUpperCase().equals("IDLE")) {
                        throw new IllegalArgumentException();
                    } else {
                        Direction direction = Direction.valueOf(value.trim().toUpperCase());
                        if ((request.getPickupFloor() == highestFloor && direction == Direction.UP) ||
                                (request.getPickupFloor() == lowestFloor && direction == Direction.DOWN)) {
                            System.out.println("Invalid direction: " + direction + " is not possible for floor " + request.getPickupFloor());
                        }
                        else {
                            request.addDirection(direction);
                            c+=1;
                        }
                        if(c==directionValues.length){
                            cond=false;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid direction: " + value + ". Use UP, DOWN, or UP,DOWN.");
                }
            }
        }

        allocationService.allocateLift(request);

    }
}

//in countStopsBeforePickup why did u remove pickup from the stop treeset we used  return stops.subSet(liftFloor, false, pickupFloor, false).size(); so that it will not include pickupfloor in the subset right?
