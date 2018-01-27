import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.Math;

public class SleepingBarber extends Thread {
 /* No of Chairs in the barbershop is 5. */
 public static final int CHAIRS = 5;

 /*we create the integer numberOfFreeSeats so that the customers can either sit on a free seat or leave the barbershop if there are no seats available*/
 public static int numberOfFreeSeats = CHAIRS;
 
 /* We create Customer pool which will be waiting for their hair cut.*/
 public static CustomerPool customers = new CustomerPool(CHAIRS);

 /*We create a ReentrantLock for barber with a condition to wait if the barber is not available  */
 public static Lock barber = new ReentrantLock();
 public static Condition barberAvailable = barber.newCondition();
 /* We create a ReentrantLock for chairs so that we can increment the counter safely */
 public static Lock accessSeats = new ReentrantLock();

 /* THE CUSTOMER THREAD */

 class Customer extends Thread {
  int id;
  boolean notCut = true;

  /* Constructor for the Customer */
  public Customer(int i) {
   id = i;
  }
  public void run() {
   while (notCut) {		 	     // as long as the customer is not cut
    accessSeats.lock(); 				// tries to get access to the chairs
    if (numberOfFreeSeats > 0) { 		// if there are any free seats
     System.out.println("Customer " + this.id + " just sat down.");
     numberOfFreeSeats--; 			// sitting down on a chair
     customers.releaseCustomer();		 // notify the barber that there is
       						          // a customer
     accessSeats.unlock();			 // don't need to lock the chairs
     barber.lock();
     try {
      barberAvailable.await(); 		   // now it's this customers turn
            						  // but we have to wait if the
             						  // barber is busy
     } catch (InterruptedException e) {
     } finally {
      barber.unlock(); 
     }
     notCut = false; 
     this.get_haircut();  					 // cutting...
    } else { 							// there are no free seats
     System.out.println("There are no free seats. Customer " + this.id + " has left the barbershop.");
     accessSeats.unlock();			 // release the lock on the seats
     notCut = false; 			// the customer will leave since there
    }
   }
  }

  /* this method will simulate getting a hair-cut */
  public void get_haircut() {
   System.out.println("Customer " + this.id + " is getting his hair cut");

   try {
       sleep(5000);
   } catch (InterruptedException ex) {
   }
  }
 }
 /* THE BARBER THREAD */
 class Barber extends Thread {
  public Barber() {
  }
  public void run() {
   while (true) { 						// runs in an infinite loop
    try {
     customers.acquireCustomer(); 	// tries to acquire a customer - if
         						     // none is available he goes to sleep
     accessSeats.lock();			 // at this time he has been awaken ->
          		             // want to modify the number of available seats
     numberOfFreeSeats++; 					// one chair gets free
     barber.lock();
     try {
      barberAvailable.signal(); 			// the barber is ready to cut
     } finally {
      barber.unlock(); 
     }
     accessSeats.unlock();       // we don't need the lock on the chairs now
     this.cutHair();			 // cutting...
    } catch (InterruptedException ex) {
    }
   }
  }
  /* this method will simulate cutting hair */
  public void cutHair() {
   System.out.println("The barber is cutting hair");
   try {
    sleep(5000);
   } catch (InterruptedException ex) {
   }
  }
 }
 /* main method */
 public static void main(String args[]) {
SleepingBarber barberShop = new SleepingBarber(); 
  barberShop.start();
 }
 public void run() {
  Barber barber = new Barber();
  barber.start(); 

  for (int i = 1; i < 16; i++) {
   Customer aCustomer = new Customer(i);
   aCustomer.start();
   try {
    sleep(2000);
   } catch (InterruptedException ex) {
   }
  }
 }
}
class CustomerPool {

    private final Lock lock = new ReentrantLock();
    private final Condition poolAvailable = lock.newCondition();
    private int num_customers;
    private final int max_num_customers;

    public CustomerPool(int num_customer_pools) {
            this.max_num_customers = num_customer_pools;
            this.num_customers = 0;
    }
    public void acquireCustomer() throws InterruptedException {
        lock.lock();
        try {
            while (num_customers <= 0)
                poolAvailable.await();
            --num_customers;
        } finally {
            lock.unlock();
        }
    }
    public void releaseCustomer() {
        lock.lock();
        try {
            // check to ensure release does not occur before acquire
            if(num_customers >= max_num_customers)      
                return;
            ++num_customers;
            poolAvailable.signal();
        } finally {
            lock.unlock();
        }
    }
    public int getNumOfCustomers() {
     return num_customers;
    }
}

