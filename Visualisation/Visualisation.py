import socket
import matplotlib.pyplot as plt
import numpy as np
import math
import thread
from time import sleep
from mpl_toolkits.mplot3d import Axes3D


class Visualizer:

    queue = []
    calibrated = True

    calibratedPose = np.matrix([0.0, 0.0, 0.0])

    def __init__(self):
        thread.start_new_thread(self.updateGUI, ())
        print "Started GUI thread"
        self.handle_connection()

    def handle_connection(self):

        try:
            readIP = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            readIP.connect(("8.8.8.8", 0))
            ip = readIP.getsockname()[0]
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
                            self.queue.insert(0,msg)

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
        fig = plt.figure()

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
            if len(self.queue) > 0:

                msg = self.queue.pop()
                print msg
                if msg[0] == "absolute" and self.calibrated:

                    try:
                        theta = [float(msg[1]), float(msg[2]), float(msg[3])]

                        [xs, ys, zs] = self.rotatePoint(points, theta)

                        plot._offsets3d = (xs,ys,zs)
                        fig.canvas.draw()

                    except ValueError:
                        print repr(msg[1])
                        print repr(msg[2])
                        print repr(msg[3])
                        print "Non float value in message"

                elif msg[0] == "calibrate":
                    try:
                        print "Calibrating"
                        self.calibratedPose[0, 0] = float(msg[1])
                        self.calibratedPose[0, 1] = float(msg[2])
                        self.calibratedPose[0, 2] = float(msg[3])
                        self.calibrated = True
                    except ValueError:
                        print repr(msg[1])
                        print repr(msg[2])
                        print repr(msg[3])
                        print "Non float value in message"
            else:
                sleep(0.01)
        plt.show()

    def rotatePoint(self, points, theta):
        rads = np.matrix(theta) - self.calibratedPose
        r = rads[0, 0]
        p = rads[0, 1]
        y = rads[0, 2]
        R = np.matrix([[math.cos(r)*math.cos(p), math.cos(r)*math.sin(p)*math.sin(y) - math.sin(r) * math.cos(y), math.cos(r)*math.sin(p)*math.cos(y) + math.sin(r)*math.sin(y)],
                       [math.sin(r)*math.cos(p), math.cos(r)*math.sin(p)*math.sin(y) + math.cos(r) * math.cos(y), math.sin(r)*math.sin(p)*math.cos(y) - math.cos(r)*math.sin(y)],
                       [-math.sin(p), math.cos(p)*math.sin(y), math.cos(p)*math.cos(y)]])
        transformed = points * R
        xs = np.squeeze(np.asarray(transformed[:, 0]))
        ys = np.squeeze(np.asarray(transformed[:, 1]))
        zs = np.squeeze(np.asarray(transformed[:, 2]))
        return [xs, ys, zs]

if __name__ == '__main__':
    vis = Visualizer()