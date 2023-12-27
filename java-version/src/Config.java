public class Config {
  private static int numberOfProducers = 5;
  private static int numberOfConsumers = 2;
  private static int numberOfBuffers = 10;
  private static int numberOfProductions = 8;
  private static int bufferCapacity = 100;
  private static int presentationDelay = 50;

  private static FullBufferAction fullBufferAction = FullBufferAction.TRY_AGAIN;

  public static FullBufferAction getFullBufferAction() {
    return fullBufferAction;
  }

  public static int getPresentationDelay() {
    return presentationDelay;
  }

  public static int getBufferCapacity() {
    return bufferCapacity;
  }

  public static int getNumberOfProducers() {
    return numberOfProducers;
  }

  public static int getNumberOfConsumers() {
    return numberOfConsumers;
  }

  public static int getNumberOfBuffers() {
    return numberOfBuffers;
  }

  public static int getNumberOfProductions() {
    return numberOfProductions;
  }
}
