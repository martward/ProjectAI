import socket
import matplotlib.pyplot as plt
import numpy as np
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

                        if msg[0] == "stop":
                            print "Receiving messages stopped."
                            break
                        else:
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

        points = np.array([[5, 2.5, 0], [-5, 2.5, 0], [-5, -2.5, 0], [5, -2.5, 0]])

        plt.ion()
        fig = plt.figure(figsize=(22, 15))
        ax = fig.add_subplot(111, projection='3d')
        ax.scatter([], [], [])
        colors = "black"
        ax.set_xlim([-10, 10])
        ax.set_ylim([-10, 10])
        ax.set_zlim([-10, 10])

        [xs, ys, zs] = self.rotatePoint(points, np.array([0, 0, 0, 0]), np.array([0, 0, 0]))

        plot = ax.scatter(xs, zs, ys, c=colors)
        fig.canvas.draw()

        while True:
            if self.data:
                if self.data[0] == "absolute":
                    try:
                        quaternion = np.array([float(self.data[1]), float(self.data[2]),
                                               -float(self.data[3]), float(self.data[4])])
                        translation = np.array([float(self.data[5]), float(self.data[6]), float(self.data[7])])
                        [xs, ys, zs] = self.rotatePoint(points, quaternion, translation)
                        plot._offsets3d = (xs, zs, ys)
                        fig.canvas.draw()
                        self.data = []
                    except ValueError:
                        #  print "Non float value in message"
                        pass
                else:
                    continue
            else:
                sleep(0.01)
        plt.show()

    def rotatePoint(self, points, quaternion, translation):
        x = quaternion[0]
        y = quaternion[1]
        z = quaternion[2]
        w = quaternion[3]

        n = x*x + y*y + z*z + w*w
        s = 0
        if n != 0:
            s = 2/n
        wx = s * x * w
        wy = s * y * w
        wz = s * z * w
        xx = s * x * x
        xy = s * x * y
        xz = s * x * z
        yy = s * y * y
        yz = s * y * z
        zz = s * z * z
        R = np.array([[1 - (yy + zz), xy - wz, xz + wy],
                      [xy + wz, 1 - (xx + zz), yz - wx],
                      [xz - wy, yz + wx, 1 - (xx + yy)]])
        transformed = points.dot(R)
        print translation
        xs = np.squeeze(np.asarray(transformed[:, 0]))
        ys = np.squeeze(np.asarray(transformed[:, 1]))
        zs = np.squeeze(np.asarray(transformed[:, 2]))
        return [xs, ys, zs]

if __name__ == '__main__':
    vis = Visualizer()
