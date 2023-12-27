package main

import (
	"fmt"
)

type Buffer struct {
	ID             int
	value          int
	fromProducers  chan int
	consumerOrders chan ConsumerOrder
}

type ConsumerOrder struct {
	portion     int
	destination chan<- int
}

func NewBuffer(ID int) *Buffer {
	fmt.Printf("BUFOR %d CREATED\n", ID)
	return &Buffer{
		ID:             ID,
		value:          0,
		fromProducers:  make(chan int, 10),
		consumerOrders: make(chan ConsumerOrder, 10),
	}
}

func (b *Buffer) Run() {
	for {
		select {
		case receivedPortion := <-b.fromProducers:
			b.value += receivedPortion
			//fmt.Println("Buffer", b.ID, "value:", b.value)
		case order := <-b.consumerOrders:
			b.value -= order.portion
			order.destination <- order.portion
		}

	}

}
