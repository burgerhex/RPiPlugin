import grovepi
import socket
import time
import threading

ROTARY_PORT = 2  # port A2
SLEEP_TIME = 0.5

PORT = 57944

lock = threading.Lock()

# this is a dumb workaround that's needed to get the local ip of the rpi
# with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
#     s.connect(("8.8.8.8", 80))
#     HOST = s.getsockname()[0]

while True:
    with socket.socket() as server:
        addr = ('', PORT)
        print("hosting on:", addr)
        print("binding...")
        server.bind(addr)
        print("listening...")
        server.listen(1)

        print("waiting for connection...")
        sock, addr = server.accept()
        print("connected!")

        # TODO: one thread for sending, one thread for receiving

        with sock:
            print("Got a connection from:", addr)
            broken = False

            while not broken:
                with lock:
                    rotary_reading = grovepi.analogRead(ROTARY_PORT)
                    print(f"rotary reading: {rotary_reading}")
                    try:
                        sock.send((str(rotary_reading) + "\n").encode())
                    except BrokenPipeError:
                        broken = True

                time.sleep(SLEEP_TIME)

        print("Plugin instance lost, restarting...")
