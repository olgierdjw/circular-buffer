package main

import (
	"fmt"
	"math/rand"
	"time"
)

type Producer struct {
	ID                int
	portion           int
	managerConnection chan<- ProductionRequest
	assignedBuffer    chan chan int
}

func NewProducer(ID, portion int, managerConnection chan<- ProductionRequest) *Producer {
	fmt.Printf("PRODUCER %d CREATED\n", ID)
	return &Producer{
		ID:                ID,
		portion:           portion,
		managerConnection: managerConnection,
		assignedBuffer:    make(chan chan int),
	}
}

func (p *Producer) Run() {
	time.Sleep(time.Duration(rand.Int()%4) * time.Second)
	numSuccessProductions := 0
	numFailure := 0
	for numSuccessProductions < numProduction {
		if success := p.produce(); success {
			numFailure = 0
			numSuccessProductions += 1
		} else {
			numFailure += 1
			fmt.Printf("PRODUCER %d: (ATTEMPT %d) BUFFER NOT ASSIGNED\n", p.ID, numFailure)
			time.Sleep(time.Second * time.Duration(numFailure))
		}

	}
}

func (p *Producer) produce() (success bool) {

	// send request
	p.managerConnection <- ProductionRequest{
		ID:                 p.ID,
		portion:            p.portion,
		assignedBufferChan: p.assignedBuffer,
	}

	// get buffer connection
	bufferConnection := <-p.assignedBuffer

	if bufferConnection == nil {
		return false
	}

	bufferConnection <- p.portion
	fmt.Printf("PRODUCER %d: CONNECTED AND SENT\n", p.ID)

	// find buffer and send portion to add
	return true
}
