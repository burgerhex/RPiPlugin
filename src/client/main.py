import grovepi
import socket
import time
import threading
import websocket

ROTARY_PORT = 2  # port A2
BUTTON_PORT = 2  # port D2
LED_PORT = 3  # port D3
TEMPERATURE_PORT = 1  # port A1
SLEEP_TIME = 0.5

PORT = 57944

grove_lock = threading.Lock()
socket_lock = threading.Lock()

# this is a dumb workaround that's needed to get the local ip of the rpi
# with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
#     s.connect(("8.8.8.8", 80))
#     HOST = s.getsockname()[0]


def rotary_recv_loop(server_socket):
    last_sent = None
    broken = False

    while not broken:
        with grove_lock:
            rotary_reading = grovepi.analogRead(ROTARY_PORT)

        print(f"rotary reading: {rotary_reading}")
        try:
            if last_sent is None or last_sent != rotary_reading:
                with socket_lock:
                    server_socket.send((str(rotary_reading) + "\n").encode())
                last_sent = rotary_reading
        except BrokenPipeError:
            broken = True

        time.sleep(SLEEP_TIME)


def on_open(w):
    w.send("Hello")
    w.close()


websocket.enableTrace(True)
ws = websocket.WebSocketApp("ws://localhost:" + str(PORT + 1),
                            on_open=on_open,
                            on_message=lambda w, m: print(m),
                            on_error=lambda w, e: print(e),
                            on_close=lambda w: print("closed"))


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
            t1 = threading.Thread(target=rotary_recv_loop, args=(sock,))
            t1.start()


        print("Plugin instance lost, restarting...")
