import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.One2OneChannelInt;

public class Buffer implements CSProcess {
  int ID;
  int maxSize;
  int currentSize = 0;
  boolean continueMainLoop = true;
  One2OneChannelInt managerConnection = Channel.one2oneInt();
  One2OneChannelInt consumerChannel = Channel.one2oneInt();
  One2OneChannelInt producerChannel = Channel.one2oneInt();

  public One2OneChannelInt getConsumerChannel() {
    return consumerChannel;
  }

  public One2OneChannelInt getProducerChannel() {
    return producerChannel;
  }

  public One2OneChannelInt getManagerConnection() {
    return managerConnection;
  }

  public Buffer(int ID, int maxSize) {
    this.ID = ID;
    this.maxSize = maxSize;
  }

  private void showSummaryAndExit() {
    continueMainLoop = false;
    System.out.printf("BUFFER %d: value = %d EXIT\n", ID, currentSize);
  }

  @Override
  public void run() {
    System.out.printf("BUFFER %d: CREATED\n", ID);
    while (continueMainLoop) {
      try {
        Thread.sleep(Config.getPresentationDelay());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      Message managerOrder = Message.values()[managerConnection.in().read()];
      switch (managerOrder) {
        case WAIT_FOR_INCOMING_CONSUMER -> {
          System.out.printf("BUFFER %d: WAITING FOR INCOMING CONSUMER\n", ID);

          // parse additional portion request
          int portion = managerConnection.in().read();
          currentSize -= portion;
          consumerChannel.out().write(portion);
        }
        case WAIT_FOR_INCOMING_PRODUCER -> {
          int portionFromProducer = producerChannel.in().read();
          currentSize += portionFromProducer;
          System.out.printf(
              "BUFFER %d: ADDED %d, NEW VALUE %d\n", ID, portionFromProducer, currentSize);
        }
        case KILL_BUFFER_AND_SHOW_SUMMARY -> showSummaryAndExit();

        default -> {
          System.out.printf("BUFFER %d: INVALID MESSAGE\n", ID);
        }
      }
    }
  }
}
