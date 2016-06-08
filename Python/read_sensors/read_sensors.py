import socket
import matplotlib.pyplot as plt
import numpy as np
import math
import thread
from time import sleep
from mpl_toolkits.mplot3d import Axes3D


class Visualizer:

    queue = []
    calibrated = False
    range = [-50, 50]

    calibratedPose = np.matrix([0.0, 0.0, 0.0])
    rotation = [0.0, 0.0, 0.0]
    translation = np.matrix([0.0, 0.0, 0.0])

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
                    msg, address = c.recvfrom(1024)
                    msg = msg[2:]
                    msg = msg.split("/")

                    if msg[0] == "stop":
                        print "Receiving messages stopped."
                        break
                    else:
                        self.queue.insert(0, msg)

                except:
                    print "Connection Lost"
                    c.close()
                    s.close()
                    break
        except:
            "No connection found"

    def updateGUI(self):

        points = np.matrix([[5, 2.5, 0], [-5, 2.5, 0], [-5, -2.5, 0], [5, -2.5, 0]])

        plt.ion()
        fig = plt.figure(figsize=(20, 10))

        ax = fig.add_subplot(111, projection='3d')
        ax.scatter([], [], [])
        colors = "black"

        ax.set_xlim(self.range)
        ax.set_ylim(self.range)
        ax.set_zlim(self.range)

        [xs, ys, zs] = self.rotatePoint(points, self.rotation, self.translation)

        ax.scatter(xs, ys, zs, c=colors)
        fig.canvas.draw()

        while True:
            if len(self.queue) > 0:

                msg = self.queue.pop()
                if msg[0] == "absolute" and self.calibrated:

                    try:
                        self.rotation = [float(msg[3]), float(msg[1]), float(msg[2])]

                    except ValueError:
                        print repr(msg[1])
                        print repr(msg[2])
                        print repr(msg[3])
                        print "Non float value in message"

                elif msg[0] == "translate" and self.calibrated:
                    try:
                        self.translation = self.translation + np.matrix([float(msg[1]), float(msg[2]), float(msg[3])])

                    except ValueError:
                        print repr(msg[1])
                        print repr(msg[2])
                        print repr(msg[3])
                        print "Non float value in message"

                elif msg[0] == "calibrate":
                    try:
                        print "Calibrating"
                        self.calibratedPose[0, 0] = float(msg[3])
                        self.calibratedPose[0, 1] = float(msg[1])
                        self.calibratedPose[0, 2] = float(msg[2])
                        self.calibrated = True

                        print "Calibrated"

                        continue

                    except ValueError:
                        print repr(msg[1])
                        print repr(msg[2])
                        print repr(msg[3])
                        print "Non float value in message"

                if self.calibrated:
                    [xs, ys, zs] = self.rotatePoint(points, self.rotation, self.translation)

                    ax.clear()
                    ax.set_xlim(self.range)
                    ax.set_ylim(self.range)
                    ax.set_zlim(self.range)
                    ax.scatter(xs, ys, zs, c=colors)
                    fig.canvas.draw()

    def rotatePoint(self, points, theta, translation):
        print theta, self.calibratedPose
        rads = np.radians(np.matrix(theta) - self.calibratedPose)
        r = rads[0, 0]
        p = rads[0, 1]
        y = rads[0, 2]

        r = np.matrix([[math.cos(r)*math.cos(p), math.cos(r)*math.sin(p)*math.sin(y) - math.sin(r) * math.cos(y), math.cos(r)*math.sin(p)*math.cos(y) + math.sin(r)*math.sin(y)],
                        [math.sin(r)*math.cos(p), math.cos(r)*math.sin(p)*math.sin(y) + math.cos(r) * math.cos(y), math.sin(r)*math.sin(p)*math.cos(y) - math.cos(r)*math.sin(y)],
                        [-math.sin(p), math.cos(p)*math.sin(y), math.cos(p)*math.cos(y)]])

        transformed = points*r

        #translated = np.add(transformed, translation)
        translated = transformed

        xs = np.squeeze(np.asarray(translated[:, 0]))
        ys = np.squeeze(np.asarray(translated[:, 1]))
        zs = np.squeeze(np.asarray(translated[:, 2]))

        return [xs, ys, zs]

if __name__ == '__main__':
    vis = Visualizer()
