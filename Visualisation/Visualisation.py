import socket
import matplotlib.pyplot as plt
import numpy as np
import thread
from time import sleep
from mpl_toolkits.mplot3d import Axes3D


class Visualizer:

    data = []
    transX = []
    transY = []
    transZ = []

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

        points = np.array([[5, 2.5, 0], [-5, 2.5, 0], [-5, -2.5, 0], [5, -2.5, 0]])

        plt.ion()
        fig = plt.figure(1,figsize=(22, 15))
        ax = fig.add_subplot(111, projection='3d')
        ax.scatter([], [], [])
        colors = "black"
        ax.set_xlim([-10, 10])
        ax.set_ylim([-10, 10])
        ax.set_zlim([-10, 10])

        [xs, ys, zs] = self.rotatePoint(points, np.array([0, 0, 0, 0]), np.array([0, 0, 0]))

        pl = ax.scatter(xs, zs, ys, c=colors)
        fig.canvas.draw()

        plt.ion()
        fig2 = plt.figure(figsize=(10,10))
        ax2 = fig2.add_subplot(3,1,1)
        ax3 = fig2.add_subplot(3,1,2)
        ax4 = fig2.add_subplot(3,1,3)
        ax2.set_xlim(0,100)
        ax2.set_ylim(-50,50)
        ax3.set_xlim(0,100)
        ax3.set_ylim(-50,50)
        ax4.set_xlim(0,100)
        ax4.set_ylim(-50,50)
        self.transX.append(float(0))
        self.transY.append(float(0))
        self.transZ.append(float(0))
        x = [float(0)]
        pl2 = ax2.plot(x,self.transX)
        pl3 = ax3.plot(x,self.transY)
        pl4 = ax4.plot(x,self.transZ)
        fig2.canvas.draw()

        while True:
            if self.data:
                if self.data[0] == "absolute":
                    try:
                        quaternion = np.array([float(self.data[1]), float(self.data[2]),
                                               -float(self.data[3]), float(self.data[4])])
                        translation = np.array([float(self.data[5]), float(self.data[6]), float(self.data[7])])
                        [xs, ys, zs] = self.rotatePoint(points, quaternion, translation)
                        pl._offsets3d = (xs, zs, ys)
                        fig.canvas.draw()

                        self.transX.append(float(self.data[5]))
                        self.transY.append(float(self.data[6]))
                        self.transZ.append(float(self.data[7]))
                        x = np.arange(0,len(self.transX))
                        x.astype(float)
                        pl2[0].set_ydata(np.array(self.transX))
                        pl2[0].set_xdata(np.array(x))
                        pl3[0].set_ydata(np.array(self.transY))
                        pl3[0].set_xdata(np.array(x))
                        pl4[0].set_ydata(np.array(self.transZ))
                        pl4[0].set_xdata(np.array(x))

                        #pl2[0].set_data(np.array(self.transX),x)
                        if len(x) > 100:
                            ax2.set_xlim(len(x)-100,len(x))
                            ax3.set_xlim(len(x)-100,len(x))
                            ax4.set_xlim(len(x)-100,len(x))
                        ax2.set_ylim(min(self.transX),max(self.transX))
                        ax3.set_ylim(min(self.transY),max(self.transY))
                        ax4.set_ylim(min(self.transZ),max(self.transZ))
                        fig2.canvas.draw()

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
