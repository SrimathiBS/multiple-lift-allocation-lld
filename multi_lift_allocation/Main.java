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

        int buildingLowestFloor = 0, buildingHighestFloor = 0,currentFloor = 0;
        for (int liftNumber = 1; liftNumber <= noOfLifts; liftNumber++) {
            System.out.println("\nLift " + liftNumber);
            while (true) {
                try {
                    System.out.print("Enter lowest floor: ");
                    buildingLowestFloor = scanner.nextInt();
                    System.out.print("Enter highest floor: ");
                    buildingHighestFloor = scanner.nextInt();
                    if (buildingHighestFloor <= buildingLowestFloor) {
                        System.out.println("highest floor should not be less than or equal to lowest floor");
                        continue;
                    }
                    while (true) {
                        System.out.print("Enter current floor: ");
                        currentFloor = scanner.nextInt();
                        if (currentFloor < buildingLowestFloor || currentFloor > buildingHighestFloor) {
                            System.out.println("Enter current floor from " + buildingLowestFloor + " to " + buildingHighestFloor);
                        }else break;
                    }
                    break;

                } catch (InputMismatchException e) {
                    System.out.println("Enter valid floor number");
                    scanner.nextLine();
                }
            }
            Lift lift = new Lift(liftNumber, currentFloor, Direction.IDLE, buildingLowestFloor, buildingHighestFloor);
            while (true) {
                try {
                    System.out.print("Enter direction of the lift (UP/DOWN/IDLE): ");
                    lift.direction = Direction.valueOf(scanner.next().trim().toUpperCase());
                    break;
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid direction. Use UP, DOWN or IDLE.");
                }
            }

            // idle lift has no next floors. If no next floor is entered, addLift saves the lift as IDLE
            if (lift.getDirection() != Direction.IDLE) {
                while (true) {
                    System.out.print("Enter next floor (type done to finish): ");
                    try {
                        String input = scanner.next();
                        if (input.equalsIgnoreCase("done")) {
                            break;
                        }
                        int nextFloor = Integer.parseInt(input);
                        if (nextFloor >= buildingLowestFloor && nextFloor <= buildingHighestFloor && nextFloor != currentFloor) {
                            lift.nextFloors.add(nextFloor);
                        } else {
                            throw new IllegalArgumentException();
                        }
                    } catch (NumberFormatException e) {
                        scanner.nextLine();
                        System.out.println("Enter a whole number for the floor, or type done");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Enter valid floor from " + buildingLowestFloor + " to " + buildingHighestFloor + " and it should not be equal to current floor");
                    }
                }
            }

            allocationService.addLift(lift);

        }

        // only one pickup request is taken, because floors are never removed once the lift serves them
        while (true) {
            Request request = null;
            System.out.print("\nEnter pickup floor: ");
            try {
                int pickupFloor = scanner.nextInt();
                if (pickupFloor >= buildingLowestFloor && pickupFloor <= buildingHighestFloor) {
                    request = new Request(pickupFloor);
                } else {
                    throw new IllegalArgumentException();
                }
            } catch (InputMismatchException e) {
                scanner.nextLine();   // throw away the wrong input (letters, decimals...)
                System.out.println("Enter a whole number for the floor");
                continue;
            } catch (IllegalArgumentException e) {
                System.out.println("Enter valid floor from " + buildingLowestFloor + " to " + buildingHighestFloor);
                continue;
            }
            scanner.nextLine();

                System.out.print("Enter direction (UP/DOWN, comma-separated if both): ");
                String directions = scanner.nextLine();
                String[] directionValues = directions.split(",");
                for (String value : directionValues) {
                    try {
                        if (value.trim().toUpperCase().equals("IDLE")) {
                            throw new IllegalArgumentException();
                        } else {
                            Direction direction = Direction.valueOf(value.trim().toUpperCase());
                            if ((request.getPickupFloor() == buildingHighestFloor && direction == Direction.UP) ||
                                    (request.getPickupFloor() == buildingLowestFloor && direction == Direction.DOWN)) {
                                System.out.println("Invalid direction: " + direction + " is not possible for floor " + request.getPickupFloor());
                            } else {
                                request.addDirection(direction);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid direction: " + value + ". Use UP, DOWN, or UP,DOWN.");
                    }
                }


            if (request.getDirection().isEmpty()) {
                System.out.println("No valid direction entered, request not added");
                continue ;
            }
            allocationService.allocateLift(request);
            return;
        }
    }
}



//after giving a lift direction as up and if we did not enter any pickup request will the lift's direction change from up to idle?
//what does setdirection method in liftallocationservice does?explain in simple words why it is needed?
//in main while getting direction if the direction entered is not correct, then we have to get the correct direction as input right , for that how should i loop?
//also if there is pickup floor then direction should be there right becasue in real world when a user presses a diretion its direction and floor numbers are getting stored right, so if the direction is empty ask the user to enter the direction again.
//in allocatelift method why are u iterating w.r.t to request direction size ?
//how can we say that we cannot serve even if there is one lift , at some point of time it will serve , so dont have variable like cannotserve anol in liftallocationservice.
//how bestcost,bestlift is calculated
//like this there are many unwanted logic and conditions i think . u better review this code make correct updated like i did earlier, make sure there is no unwanted logic or unnecessary conditions. give fully updated zip file