package main

import (
	"fmt"
	"math/rand"
	"time"
)

type Consumer struct {
	ID                int
	portion           int
	managerConnection chan<- ConsumerRequest
	bufferAssigned    chan bool
	incomingPortion   chan int
}

func NewConsumer(ID, portion int, managerConnection chan<- ConsumerRequest) *Consumer {
	fmt.Printf("CONSUMER %d CREATED\n", ID)
	return &Consumer{
		ID:                ID,
		portion:           portion,
		managerConnection: managerConnection,
		bufferAssigned:    make(chan bool),
		incomingPortion:   make(chan int),
	}
}

func (c *Consumer) Run() {
	time.Sleep(time.Duration(rand.Int()%4) * time.Second)
	numSuccessConsumptions := 0
	numFailure := 0
	for numSuccessConsumptions < numProduction {
		if success := c.consume(); success {
			numFailure = 0
			numSuccessConsumptions += 1
		} else {
			numFailure += 1
			fmt.Printf("PRODUCER %d: (ATTEMPT %d) BUFFER NOT ASSIGNED\n", c.ID, numFailure)
			time.Sleep(time.Second * time.Duration(numFailure))
		}

	}
}

func (c *Consumer) consume() (success bool) {

	// send request
	c.managerConnection <- ConsumerRequest{
		ID:                 c.ID,
		portion:            c.portion,
		bufferAssigned:     c.bufferAssigned,
		portionDestination: c.incomingPortion,
	}

	// request response
	waitForBufferPortion := <-c.bufferAssigned

	if waitForBufferPortion == false {
		return false
	}

	portion := <-c.incomingPortion
	fmt.Printf("CONSUMER %d: RECEIVED %d\n", c.ID, portion)
	return true
}
