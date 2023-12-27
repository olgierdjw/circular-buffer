package main

import (
	"errors"
	"fmt"
	"time"
)

const tickRate = 3 * time.Second

type ProductionRequest struct {
	ID                 int
	portion            int
	assignedBufferChan chan<- chan int
}
type ConsumerRequest struct {
	ID                 int
	portion            int
	bufferAssigned     chan<- bool
	portionDestination chan int
}

type BufferRecord struct {
	value          int
	producerInput  chan int
	consumerOrders chan<- ConsumerOrder
}

type BufferManager struct {
	productionRequests           chan ProductionRequest
	consumptionRequests          chan ConsumerRequest
	buffers                      []*BufferRecord
	producerIndex, consumerIndex int
}

func (bm *BufferManager) Run() {
	ticker := time.NewTicker(tickRate).C
	for {
		time.Sleep(time.Millisecond * 450)

		select {
		case productionRequest := <-bm.productionRequests:
			bufferRecord, err := bm.nextEmptyBuffer(productionRequest.portion)
			if err != nil {
				productionRequest.assignedBufferChan <- nil
				continue
			}

			bufferRecord.value += productionRequest.portion
			productionRequest.assignedBufferChan <- bufferRecord.producerInput

		case consumptionRequest := <-bm.consumptionRequests:
			bufferRecord, err := bm.nextFullBuffer(consumptionRequest.portion)
			if err != nil {
				consumptionRequest.bufferAssigned <- false
				continue
			}
			consumptionRequest.bufferAssigned <- true
			order := ConsumerOrder{
				portion:     consumptionRequest.portion,
				destination: consumptionRequest.portionDestination,
			}

			bufferRecord.consumerOrders <- order

		case <-ticker:
			fmt.Println("Buffer values:")
			for i, buffer := range bm.buffers {
				fmt.Printf("BUFFER %d: value = %d\n", i, buffer.value)
			}
			break
		}
	}
}

func (bm *BufferManager) nextFullBuffer(portion int) (buffer *BufferRecord, err error) {
	var nextBuffer *BufferRecord
	for step := 1; step <= len(bm.buffers); step++ {
		nextIndex := (bm.consumerIndex + step) % numBuffers
		nextBuffer = bm.buffers[nextIndex]
		if nextBuffer.value-portion >= 0 {
			bm.consumerIndex = nextIndex
			return nextBuffer, nil
		}
	}
	return nextBuffer, errors.New("empty buffers")
}

func (bm *BufferManager) nextEmptyBuffer(portion int) (buffer *BufferRecord, err error) {
	var nextBuffer *BufferRecord
	for step := 1; step <= len(bm.buffers); step++ {
		nextIndex := (bm.producerIndex + step) % numBuffers
		nextBuffer = bm.buffers[nextIndex]
		if nextBuffer.value+portion <= bufferSize {
			bm.producerIndex = nextIndex
			return nextBuffer, nil
		}
	}
	return nextBuffer, errors.New("full buffers")
}
