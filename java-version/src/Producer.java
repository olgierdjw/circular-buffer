import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.One2OneChannelInt;

public class Producer implements CSProcess {

  int ID;
  One2OneChannelInt managerConnection;
  ArrayList<One2OneChannelInt> addToBuffer;
  int portion;

  public One2OneChannelInt getManagerConnection() {
    return managerConnection;
  }

  int numberOfSuccessProductions = 0;

  public Producer(int ID, ArrayList<One2OneChannelInt> addToBuffer, int portion) {
    this.ID = ID;
    this.managerConnection = Channel.one2oneInt();
    this.addToBuffer = addToBuffer;
    this.portion = portion;
  }

  @Override
  public void run() {
    System.out.printf("PRODUCER %d: CREATED\n", ID);
    while (numberOfSuccessProductions < Config.getNumberOfProductions()) {
      // notify buffer manager
      managerConnection.out().write(portion);

      // get buffer id
      int bufferID = managerConnection.in().read();

      if (bufferID == -1) {
        FullBufferAction fullBufferAction = Config.getFullBufferAction();
        if (fullBufferAction == FullBufferAction.TRY_AGAIN) {
          Long scalar = (long) ThreadLocalRandom.current().nextInt(10, 30);
          try {
            Thread.sleep(Config.getPresentationDelay() * scalar);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          continue;

        } else if (fullBufferAction == FullBufferAction.KILL) {
          System.out.printf("PRODUCER %d: FAILED TO ASSIGN BUFFER FROM MANAGER\n", ID);
          break;
        }
      }

      // send portion
      addToBuffer.get(bufferID).out().write(portion);
      numberOfSuccessProductions += 1;
      System.out.printf("PRODUCER %d: PORTION %d SENT TO BUFFER %d\n", ID, portion, bufferID);
    }

    // deregister
    managerConnection.out().write(-1);
  }
}
