import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.One2OneChannelInt;

public class Consumer implements CSProcess {

  int ID;
  One2OneChannelInt managerConnection;
  ArrayList<One2OneChannelInt> consumeFromBuffer;
  int portion;
  int numberOfSuccessConsumptions = 0;

  public Consumer(int ID, ArrayList<One2OneChannelInt> consumeFromBuffer, int portion) {
    this.ID = ID;
    this.managerConnection = Channel.one2oneInt();
    this.consumeFromBuffer = consumeFromBuffer;
    this.portion = portion;
  }

  public One2OneChannelInt getManagerConnection() {
    return managerConnection;
  }

  @Override
  public void run() {
    System.out.printf("CONSUMER %d: CREATED\n", ID);

    while (numberOfSuccessConsumptions < Config.getNumberOfProductions()) {
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
          System.out.printf("CONSUMER %d: BUFFER CAN NOT BE ASSIGNED\n", ID);
          break;
        }
      }

      // read portion from the buffer
      int portionFromBuffer = consumeFromBuffer.get(bufferID).in().read();
      if (portionFromBuffer != portion) {
        throw new IllegalStateException("INVALID PORTION SIZE");
      }
      numberOfSuccessConsumptions += 1;
      System.out.printf("CONSUMER %d: CONSUMED PORTION %d FROM BUFFER %d\n", ID, portion, bufferID);
    }

    managerConnection.out().write(-1);
  }
}
