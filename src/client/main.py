import grovepi
import socket
import time
import threading

ROTARY_PORT = 2  # port A2
SLEEP_TIME = 1

# noinspection SpellCheckingInspection
SERVER_HOST = "Avivs-MBP-2.mshome.net"
SERVER_PORT = 57944

lock = threading.Lock()

sock = socket.socket()
sock.connect((SERVER_HOST, SERVER_PORT))

while True:
    msg = sock.recv(1024)
    if msg:
        print("received: " + msg.decode())
        sock.send("hello server! from rpi".encode())

    with lock:
        rotary_reading = grovepi.analogRead(ROTARY_PORT)
        print("rotary reading: " + rotary_reading)

    time.sleep(SLEEP_TIME)
