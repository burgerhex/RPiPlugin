import math
import sys
import grovepi
import socket
import time
import threading
import websockets
import asyncio
import random

ROTARY_PORT = 2  # port A2
BUTTON_PORT = 2  # port D2
LED_PORT = 3  # port D3
TEMPERATURE_PORT = 1  # port A1
SLEEP_TIME = 0.5

HOST = "35.236.43.231"
PORT = 57944

if len(sys.argv) > 1 and sys.argv[1] == "test":
    HOST = "Avivs-MBP.mshome.net"

grove_lock = threading.Lock()
socket_lock = threading.Lock()

# this is a dumb workaround that's needed to get the local ip of the rpi
# with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
#     s.connect(("8.8.8.8", 80))
#     HOST = s.getsockname()[0]


def rotary_recv_loop():
    last_sent = None
    broken = False

    while not broken:
        with grove_lock:
            rotary_reading = grovepi.analogRead(ROTARY_PORT)

        try:
            if last_sent is None or last_sent != rotary_reading:
                with socket_lock:
                    sock.send((str(rotary_reading) + "\n").encode())
                last_sent = rotary_reading
                print(f"new rotary reading: {rotary_reading}")
        except BrokenPipeError:
            broken = True

        time.sleep(SLEEP_TIME)


async def counter(websocket, path):
    # register(websocket) sends user_event() to websocket
    print(f"new request: ws = {websocket}, path = {path}")
    await websocket.send("welcome")
    while True:
        await asyncio.sleep(SLEEP_TIME)
        rotary_reading = grovepi.analogRead(ROTARY_PORT)
        await websocket.send(str(rotary_reading / 1023))


async def serve():
    server = await websockets.serve(counter, "0.0.0.0", PORT + 1)
    await server.wait_closed()


# asyncio.get_event_loop().run_until_complete(start_server)
# asyncio.get_event_loop().run_forever()
loop = asyncio.get_event_loop()
loop.create_task(serve())

while True:
    with socket.socket() as sock:
        addr = (HOST, PORT)
        print("connecting to:", addr)
        sock.connect(addr)
        print("connected to server!")

        t1 = threading.Thread(target=rotary_recv_loop)
        t1.start()
        t1.join()

        print("Plugin instance lost, restarting...")
