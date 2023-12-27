This project addresses the Producer-Consumer problem with multiple buffers using Communicating Sequential Processes.

#### Producer/Consumer
Processes submit production and consumption requests to the `BufferManager`, wait for the assigned buffer connection details and get a direct connection to the designated `Buffer`.

#### BufferManager
Manages producer/consumer requests, connecting them with suitable buffers to fulfill portions. It emulates a cyclic buffer by iterating over a list for balanced processing.


### Java JCSP library
```bash
cd java-version/ && javac -cp "libs/*" -d out src/*.java && cd out && java -cp .:../libs/* Main
```

### Golang
Golang supports channels out of the box, eliminating the need for third-party libraries. Golang's solution, thanks to the use of buffered channels, reduces the likelihood of blocking the `BufferManager` when a `Buffer` can't accept the next request while still processing the previous one.

``` bash
cd go-version/ && go run .
```
