package main

const (
	bufferSize    = 4
	numBuffers    = 10
	numProducers  = 5
	numConsumers  = 5
	numProduction = 1000
)

func main() {
	// init buffers
	buffers := make([]*Buffer, 0, numBuffers)
	bufferRecords := make([]*BufferRecord, 0, numBuffers)
	for i := 0; i < numBuffers; i++ {
		newBuffer := NewBuffer(i + 1)
		buffers = append(buffers, newBuffer)
		bufferRecords = append(bufferRecords, &BufferRecord{
			producerInput:  newBuffer.fromProducers,
			consumerOrders: newBuffer.consumerOrders,
		})
		go newBuffer.Run()
	}

	// global buffer manager
	manager := BufferManager{
		productionRequests:  make(chan ProductionRequest),
		consumptionRequests: make(chan ConsumerRequest),
		buffers:             bufferRecords,
	}

	for i := 0; i < numProducers; i++ {
		producer := NewProducer(i+1, 1, manager.productionRequests)
		go producer.Run()
	}
	for i := 0; i < numConsumers; i++ {
		consumer := NewConsumer(i+1, 1, manager.consumptionRequests)
		go consumer.Run()
	}

	manager.Run()
}
