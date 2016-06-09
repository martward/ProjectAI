import socket
import matplotlib.pyplot as plt
import numpy as np
import math
import thread
from time import sleep
from mpl_toolkits.mplot3d import Axes3D


class Visualizer:

    data = []

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
            print c
            print "Connected"
            while 1:
                try:
                    while 1:
                        msg, address = c.recvfrom(1024)
                        msg = msg[2:]
                        msg = msg.split("/")
                        if msg[0] == "stop":
                            print "Receiving messages stopped."
                            break
                        else:
                            self.data = msg;
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

        points = np.matrix([[0, 2.5, 5], [0, 2.5, -5], [0, -2.5, -5], [0, -2.5, 5]])

        plt.ion()
        fig = plt.figure(figsize=(22,15))
        ax = fig.add_subplot(111, projection='3d')
        ax.scatter([], [], [])
        colors = "black"
        ax.set_xlim([-10, 10])
        ax.set_ylim([-10, 10])
        ax.set_zlim([-10, 10])

        [xs, ys, zs] = self.rotatePoint(points, [0, 0, 0])

        plot = ax.scatter(xs, ys, zs, c=colors)
        fig.canvas.draw()

        while True:
            if self.data:
                if self.data[0] == "absolute":
                    try:
                        euler = [float(self.data[1]), float(self.data[2]), float(self.data[3])]
                        [xs, ys, zs] = self.rotatePoint(points, euler)
                        plot._offsets3d = (xs, ys, zs)
                        fig.canvas.draw()
                        self.data = []
                    except ValueError:
                        print repr(self.data[1])
                        print repr(self.data[2])
                        print repr(self.data[3])
                        print "Non float value in message"
                else:
                    continue
            else:
                sleep(0.01)
        plt.show()

    def rotatePoint(self, points, euler):
        rads = np.matrix(euler)
        r = rads[0, 0]
        p = rads[0, 1]
        y = rads[0, 2]
        R = np.matrix([[math.cos(r)*math.cos(p), math.cos(r)*math.sin(p)*math.sin(y) - math.sin(r) * math.cos(y),
                        math.cos(r)*math.sin(p)*math.cos(y) + math.sin(r)*math.sin(y)],
                       [math.sin(r)*math.cos(p), math.cos(r)*math.sin(p)*math.sin(y) + math.cos(r) * math.cos(y),
                        math.sin(r)*math.sin(p)*math.cos(y) - math.cos(r)*math.sin(y)],
                       [-math.sin(p), math.cos(p)*math.sin(y), math.cos(p)*math.cos(y)]])
        transformed = points * R
        xs = np.squeeze(np.asarray(transformed[:, 0]))
        ys = np.squeeze(np.asarray(transformed[:, 1]))
        zs = np.squeeze(np.asarray(transformed[:, 2]))
        return [xs, ys, zs]

if __name__ == '__main__':
    vis = Visualizer()
