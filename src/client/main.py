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
TEMPERATURE_PORT = 4  # port D4
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
        # print("rotary requesting lock")
        with grove_lock:
            # print("rotary acquired lock")
            rotary_reading = grovepi.analogRead(ROTARY_PORT)
        # print("rotary released lock")

        try:
            if last_sent is None or last_sent != rotary_reading:
                with socket_lock:
                    sock.send(f"rotary {rotary_reading}\n".encode())
                last_sent = rotary_reading
                print(f"new rotary reading: {rotary_reading}")
        except BrokenPipeError:
            broken = True

        time.sleep(SLEEP_TIME)


def temp_recv_loop():
    last_sent_temp = None
    last_sent_humidity = None
    broken = False

    while not broken:
        # print("temp requesting lock")
        with grove_lock:
            # print("temp acquired lock")
            try:
                # print("reading temp")
                temp_reading, humidity_reading = grovepi.dht(TEMPERATURE_PORT, 0)
                # print("done reading temp")
                if math.isnan(temp_reading) or math.isnan(humidity_reading):
                    print("nans, skipping")
                    time.sleep(SLEEP_TIME * 3)
                    continue
            except ValueError as e:
                print(e)
                pass
        # print("temp released lock")

        try:
            if last_sent_temp is None or last_sent_humidity is None or \
                    last_sent_temp != temp_reading or last_sent_humidity != humidity_reading:
                with socket_lock:
                    sock.send(f"temp {temp_reading}\nhumidity {humidity_reading}\n".encode())
                last_sent_temp = temp_reading
                last_sent_humidity = humidity_reading
                print(f"new temp and humidity readings: {temp_reading}, {humidity_reading}")
        except BrokenPipeError:
            broken = True

        time.sleep(SLEEP_TIME * 3)


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
        t2 = threading.Thread(target=temp_recv_loop)
        t1.start()
        t2.start()
        t1.join()
        t2.join()

        print("Plugin instance lost, restarting...")
