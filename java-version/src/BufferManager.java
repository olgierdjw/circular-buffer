import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.jcsp.lang.Alternative;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannelInt;

public class BufferManager implements CSProcess {
  ArrayList<One2OneChannelInt> buffers, producers, consumers;
  LinkedList<One2OneChannelInt> producersAndConsumersSources = new LinkedList<>();
  Guard[] readMessagesFrom;
  Alternative alternative;
  List<Integer> bufferValue = new ArrayList<>();

  int producerBufferIndex = 0, consumerBufferIndex = 0;
  int waitGroup;

  public static final String SET_PLAIN_TEXT = "\033[0;0m";
  public static final String SET_BOLD_TEXT = "\033[0;1m";

  public BufferManager(
      ArrayList<One2OneChannelInt> buffers,
      ArrayList<One2OneChannelInt> producers,
      ArrayList<One2OneChannelInt> consumers) {
    this.buffers = buffers;
    for (int i = 0; i < buffers.size(); i++) bufferValue.add(0);
    this.producers = producers;
    this.consumers = consumers;
    waitGroup = producers.size() + consumers.size();

    producersAndConsumersSources.addAll(producers);
    producersAndConsumersSources.addAll(consumers);
    readMessagesFrom =
        producersAndConsumersSources.stream()
            .map(One2OneChannelInt::in)
            .toList()
            .toArray(new Guard[] {});
    alternative = new Alternative(readMessagesFrom);
  }

  @Override
  public void run() {
    System.out.println(SET_BOLD_TEXT + "BUFFER MANAGER: CREATED!" + SET_PLAIN_TEXT);
    while (waitGroup > 0) {
      int sourceIndex = alternative.select();
      Message requestType = getRequestType(sourceIndex);

      // get portion
      One2OneChannelInt producerOrConsumer = producersAndConsumersSources.get(sourceIndex);
      int portion = producerOrConsumer.in().read();

      switch (requestType) {
        case PRODUCTION_REQUEST -> {
          System.out.println(
              SET_BOLD_TEXT
                  + "BUFFER MANAGER: PRODUCER REQUEST FROM P-"
                  + sourceIndex
                  + SET_PLAIN_TEXT);

          if (portion == -1) {
            System.out.println(
                SET_BOLD_TEXT
                    + "BUFFER MANAGER: DEREGISTER PRODUCER "
                    + sourceIndex
                    + SET_PLAIN_TEXT);
            waitGroup -= 1;
            continue;
          }

          // find buffer
          int bufferID = getProducerBufferIndex(portion);
          if (bufferID == -1) {
            producerOrConsumer.out().write(-1);
            System.out.println(
                SET_BOLD_TEXT
                    + "BUFFER MANAGER: FULL BUFFERS, PRODUCER "
                    + sourceIndex
                    + SET_PLAIN_TEXT);
            continue;
          }

          // connect producer with buffer
          int newValue = bufferValue.get(bufferID) + portion;
          bufferValue.set(bufferID, newValue);
          buffers.get(bufferID).out().write(Message.WAIT_FOR_INCOMING_PRODUCER.ordinal());
          producerOrConsumer.out().write(bufferID);
        }
        case CONSUMPTION_REQUEST -> {
          int consumerID = sourceIndex - producers.size();
          System.out.println(
              SET_BOLD_TEXT
                  + "BUFFER MANAGER: CONSUMPTION REQUEST FROM "
                  + consumerID
                  + SET_PLAIN_TEXT);

          if (portion == -1) {
            System.out.println(
                SET_BOLD_TEXT
                    + "BUFFER MANAGER: DEREGISTER CONSUMER "
                    + consumerID
                    + SET_PLAIN_TEXT);
            waitGroup -= 1;
            continue;
          }

          int bufferID = getConsumerBufferIndex(portion);
          if (bufferID == -1) {
            producerOrConsumer.out().write(-1);
            System.out.println(
                SET_BOLD_TEXT
                    + "BUFFER MANAGER: CONSUMER "
                    + consumerID
                    + " EMPTY BUFFER!"
                    + SET_PLAIN_TEXT);
            continue;
          }

          // connect producer with buffer
          bufferValue.set(bufferID, bufferValue.get(bufferID) - portion);
          buffers.get(bufferID).out().write(Message.WAIT_FOR_INCOMING_CONSUMER.ordinal());
          buffers.get(bufferID).out().write(portion);

          producerOrConsumer.out().write(bufferID);
        }
      }
    }

    System.out.println(
        SET_BOLD_TEXT + "BUFFER MANAGER: NO PRODUCERS AND CONSUMERS LEFT" + SET_PLAIN_TEXT);

    for (One2OneChannelInt b : buffers)
      b.out().write(Message.KILL_BUFFER_AND_SHOW_SUMMARY.ordinal());
  }

  private Message getRequestType(int guardID) {
    if (guardID < producers.size()) return Message.PRODUCTION_REQUEST;
    return Message.CONSUMPTION_REQUEST;
  }

  private int getProducerBufferIndex(int portion) {
    int buffersCnt = buffers.size();
    for (int step = 0; step < buffersCnt; step++) {
      producerBufferIndex = (producerBufferIndex + 1) % buffersCnt;
      if (bufferValue.get(producerBufferIndex) + portion <= Config.getBufferCapacity())
        return producerBufferIndex;
    }
    return -1;
  }

  private int getConsumerBufferIndex(int portion) {
    int buffersCnt = buffers.size();
    for (int step = 0; step < buffersCnt; step++) {
      consumerBufferIndex = (consumerBufferIndex + 1) % buffersCnt;
      if (bufferValue.get(consumerBufferIndex) - portion >= 0) return consumerBufferIndex;
    }
    return -1;
  }
}
