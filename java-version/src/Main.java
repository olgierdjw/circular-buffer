import java.util.ArrayList;
import java.util.LinkedList;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.One2OneChannelInt;
import org.jcsp.lang.Parallel;

public final class Main {

  public static void main(String[] args) throws InterruptedException {
    ArrayList<Producer> producers = new ArrayList<>();
    ArrayList<Consumer> consumers = new ArrayList<>();

    // create buffers
    ArrayList<Buffer> buffers = new ArrayList<>();
    for (int i = 0; i < Config.getNumberOfBuffers(); i++) {
      buffers.add(new Buffer(i, Config.getBufferCapacity()));
    }

    // create producers
    ArrayList<One2OneChannelInt> bufferProducerInputs = new ArrayList<>();
    for (Buffer b : buffers) bufferProducerInputs.add(b.getProducerChannel());
    for (int i = 0; i < Config.getNumberOfProducers(); i++) {
      producers.add(new Producer(i, bufferProducerInputs, 1));
    }

    // create consumers
    ArrayList<One2OneChannelInt> bufferConsumerOutputs = new ArrayList<>();
    for (Buffer b : buffers) bufferConsumerOutputs.add(b.getConsumerChannel());
    for (int i = 0; i < Config.getNumberOfConsumers(); i++) {
      consumers.add(new Consumer(i, bufferConsumerOutputs, 1));
    }

    // connect with buffer manager
    BufferManager bufferManager;
    ArrayList<One2OneChannelInt> bufferToManager = new ArrayList<>();
    for (Buffer b : buffers) bufferToManager.add(b.getManagerConnection());
    ArrayList<One2OneChannelInt> consumerToManager = new ArrayList<>();
    for (Consumer c : consumers) consumerToManager.add(c.getManagerConnection());
    ArrayList<One2OneChannelInt> producerToManager = new ArrayList<>();
    for (Producer p : producers) producerToManager.add(p.getManagerConnection());
    bufferManager = new BufferManager(bufferToManager, producerToManager, consumerToManager);

    // run all threads
    LinkedList<CSProcess> processes = new LinkedList<>();
    processes.add(bufferManager);
    processes.addAll(producers);
    processes.addAll(consumers);
    processes.addAll(buffers);
    Parallel parallel = new Parallel(processes.toArray(new CSProcess[0]));
    parallel.run();

    System.out.println("MAIN: EXIT!");
  }
}
