import socket
import matplotlib.pyplot as plt
import numpy as np
import thread
from time import sleep
from mpl_toolkits.mplot3d import Axes3D


class Visualizer:

    data = []
    accelerometer = [0.]
    velocity = [0.]
    position = [0.]

    accelerometerLimits = [-1.0, 1.0]
    velocityLimits = [-1.0, 1.0]
    positionLimits = [-1.0, 1.0]

    def __init__(self):
        thread.start_new_thread(self.updateGUI, ())
        print "Started GUI thread"
        self.handle_connection()

    def handle_connection(self):
        try:
            readIP = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            readIP.connect(("8.8.8.8", 0))
            ip = readIP.getsockname()[0]
            print ip
            readIP.close()

            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            print "binding"
            try:
                s.bind((ip, 9090))
            except socket.error, msg:
                print msg

            print "Waiting"
            s.listen(1)

            c, address = s.accept()
            print "Connected"
            while 1:
                try:
                    while 1:
                        buf = ''
                        char = 0
                        while not char == '\n' and not char == "\n" and not char == "":
                            if char != 0 and ord(char) != 0:
                                buf = buf + char
                            char = c.recv(1)
                        msg = buf
                        msg = msg.split("/")
                        #print msg
                        self.data = msg
                except:
                    print "Connection Lost"
                    c.shutdown()
                    s.shutdown()
                    c.close()
                    s.close()
                    break
                finally:
                    c.shutdown()
                    s.shutdown()
                    c.close()
                    s.close()
        except:
            "No connection found"

    def updateGUI(self):

        plt.ion()
        fig2 = plt.figure(figsize=(22, 15))
        sf1 = fig2.add_subplot(3, 1, 1)
        sf1.set_title('Accelerometer')
        sf2 = fig2.add_subplot(3, 1, 2)
        sf2.set_title('Velocity')
        sf3 = fig2.add_subplot(3, 1, 3)
        sf3.set_title('Position')

        x = [float(0)]
        pl1 = sf1.plot(x, self.accelerometer)
        pl2 = sf2.plot(x, self.velocity)
        pl3 = sf3.plot(x, self.position)

        fig2.canvas.draw()

        while True:
            if self.data:
                try:
                    self.accelerometer.append(float(self.data[6]))
                    self.velocity.append(float(self.data[9]))
                    self.position.append(float(self.data[12]))

                    x = np.arange(0, len(self.accelerometer))
                    x.astype(float)
                    xnparray = np.array(x)
                    pl1[0].set_ydata(np.array(self.accelerometer))
                    pl1[0].set_xdata(xnparray)
                    pl2[0].set_ydata(np.array(self.velocity))
                    pl2[0].set_xdata(xnparray)
                    pl3[0].set_ydata(np.array(self.position))
                    pl3[0].set_xdata(xnparray)

                    if len(x) > 100:
                        sf1.set_xlim(len(x)-100, len(x))
                        sf2.set_xlim(len(x)-100, len(x))
                        sf3.set_xlim(len(x)-100, len(x))
                    else:
                        sf1.set_xlim(0, len(x))
                        sf2.set_xlim(0, len(x))
                        sf3.set_xlim(0, len(x))

                    # accelerometer 
                    if float(self.data[4]) < self.accelerometerLimits[0]:
                        self.accelerometerLimits[0] = float(self.data[4])

                    if float(self.data[5]) < self.accelerometerLimits[0]:
                        self.accelerometerLimits[0] = float(self.data[5])

                    if float(self.data[6]) < self.accelerometerLimits[0]:
                        self.accelerometerLimits[0] = float(self.data[6])
                        
                    if float(self.data[4]) > self.accelerometerLimits[1]:
                        self.accelerometerLimits[1] = float(self.data[4])

                    if float(self.data[5]) > self.accelerometerLimits[1]:
                        self.accelerometerLimits[1] = float(self.data[5])

                    if float(self.data[6]) > self.accelerometerLimits[1]:
                        self.accelerometerLimits[1] = float(self.data[6])

                    # velocity
                    if float(self.data[7]) < self.velocityLimits[0]:
                        self.velocityLimits[0] = float(self.data[7])

                    if float(self.data[8]) < self.velocityLimits[0]:
                        self.velocityLimits[0] = float(self.data[8])

                    if float(self.data[9]) < self.velocityLimits[0]:
                        self.velocityLimits[0] = float(self.data[9])

                    if float(self.data[7]) > self.velocityLimits[1]:
                        self.velocityLimits[1] = float(self.data[7])

                    if float(self.data[8]) > self.velocityLimits[1]:
                        self.velocityLimits[1] = float(self.data[8])

                    if float(self.data[9]) > self.velocityLimits[1]:
                        self.velocityLimits[1] = float(self.data[9])

                    # translation
                    if float(self.data[10]) < self.positionLimits[0]:
                        self.positionLimits[0] = float(self.data[10])

                    if float(self.data[11]) < self.positionLimits[0]:
                        self.positionLimits[0] = float(self.data[11])

                    if float(self.data[12]) < self.positionLimits[0]:
                        self.positionLimits[0] = float(self.data[12])

                    if float(self.data[10]) > self.positionLimits[1]:
                        self.positionLimits[1] = float(self.data[10])

                    if float(self.data[11]) > self.positionLimits[1]:
                        self.positionLimits[1] = float(self.data[11])

                    if float(self.data[12]) > self.positionLimits[1]:
                        self.positionLimits[1] = float(self.data[12])

                    sf1.set_ylim(self.accelerometerLimits[0], self.accelerometerLimits[1])
                    sf2.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                    sf3.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))

                    fig2.canvas.draw()

                    self.data = []

                except ValueError:
                    # print "Non float value in message"
                    pass
            else:
                sleep(0.01)
        plt.show()

if __name__ == '__main__':
    vis = Visualizer()
