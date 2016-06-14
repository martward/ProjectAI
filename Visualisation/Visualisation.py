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
    accX = []
    accY = []
    accZ = []

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
                        print msg

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
        fig2 = plt.figure(figsize=(22,15 ))
        sf1 = fig2.add_subplot(3,2,1)
        sf1.set_title('translation X')
        sf3 = fig2.add_subplot(3,2,2)
        sf3.set_title('accelerometer X')
        sf5 = fig2.add_subplot(3,2,3)
        sf5.set_title('translation Y')
        sf2 = fig2.add_subplot(3,2,4)
        sf2.set_title('accelerometer Y')
        sf4 = fig2.add_subplot(3,2,5)
        sf4.set_title('translation Z')
        sf6 = fig2.add_subplot(3,2,6)
        sf6.set_title('accelerometer Z')
        sf1.set_xlim(0,100)
        sf1.set_ylim(-50,50)
        sf2.set_xlim(0,100)
        sf2.set_ylim(-50,50)
        sf3.set_xlim(0,100)
        sf3.set_ylim(-50,50)
        sf4.set_xlim(0,100)
        sf4.set_ylim(-50,50)
        sf5.set_xlim(0,100)
        sf5.set_ylim(-50,50)
        sf6.set_xlim(0,100)
        sf6.set_ylim(-50,50)
        self.transX.append(float(0))
        self.transY.append(float(0))
        self.transZ.append(float(0))
        self.accX.append(float(0))
        self.accY.append(float(0))
        self.accZ.append(float(0))

        x = [float(0)]
        pl1 = sf1.plot(x,self.transX)
        pl2 = sf2.plot(x,self.accX)
        pl3 = sf3.plot(x,self.transY)
        pl4 = sf4.plot(x,self.accY)
        pl5 = sf5.plot(x,self.transZ)
        pl6 = sf6.plot(x,self.accZ)
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
                        self.accX.append(float(self.data[8]))
                        self.accY.append(float(self.data[9]))
                        self.accZ.append(float(self.data[10]))
                        x = np.arange(0,len(self.transX))
                        x.astype(float)
                        pl1[0].set_ydata(np.array(self.transX))
                        pl1[0].set_xdata(np.array(x))
                        pl2[0].set_ydata(np.array(self.accX))
                        pl2[0].set_xdata(np.array(x))
                        pl3[0].set_ydata(np.array(self.transY))
                        pl3[0].set_xdata(np.array(x))
                        pl4[0].set_ydata(np.array(self.accY))
                        pl4[0].set_xdata(np.array(x))
                        pl5[0].set_ydata(np.array(self.transZ))
                        pl5[0].set_xdata(np.array(x))
                        pl6[0].set_ydata(np.array(self.accZ))
                        pl6[0].set_xdata(np.array(x))


                        #pl2[0].set_data(np.array(self.transX),x)
                        if len(x) > 100:
                            sf1.set_xlim(len(x)-100,len(x))
                            sf2.set_xlim(len(x)-100,len(x))
                            sf3.set_xlim(len(x)-100,len(x))
                            sf4.set_xlim(len(x)-100,len(x))
                            sf5.set_xlim(len(x)-100,len(x))
                            sf6.set_xlim(len(x)-100,len(x))
                        sf1.set_ylim(min(self.transX),max(self.transX))
                        sf3.set_ylim(min(self.transY),max(self.transY))
                        sf5.set_ylim(min(self.transZ),max(self.transZ))
                        sf2.set_ylim(min(self.accX),max(self.accX))
                        sf4.set_ylim(min(self.accY),max(self.accY))
                        sf6.set_ylim(min(self.accZ),max(self.accZ))
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
