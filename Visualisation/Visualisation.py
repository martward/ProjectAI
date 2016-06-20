import socket
import matplotlib.pyplot as plt
import numpy as np
import thread
from time import sleep
from mpl_toolkits.mplot3d import Axes3D


class Visualizer:

    data = []
    accelerometer = [[float(0)], [float(0)], [float(0)]]
    velocity = [[float(0)], [float(0)], [float(0)]]
    position = [[float(0)], [float(0)], [float(0)]]

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

        points = np.array([[5, 2.5, 0], [-5, 2.5, 0], [-5, -2.5, 0], [5, -2.5, 0]])

        plt.ion()
        fig = plt.figure(1, figsize=(22, 15))
        ax = fig.add_subplot(111, projection='3d')
        ax.scatter([], [], [])
        colors = "black"
        ax.set_xlabel("X")
        ax.set_ylabel("Y")
        ax.set_zlabel("Z")
        ax.set_xlim([-10, 10])
        ax.set_ylim([-10, 10])
        ax.set_zlim([-10, 10])

        [xs, ys, zs] = self.rotatePoint(points, np.array([0, 0, 0, 0]), np.array([0, 0, 0]))

        pl = ax.scatter(xs, zs, ys, c=colors)
        fig.canvas.draw()

        fig2 = plt.figure(figsize=(22, 15))
        sf1 = fig2.add_subplot(3, 3, 1)
        sf1.set_title('Accelerometer X')
        sf2 = fig2.add_subplot(3, 3, 2)
        sf2.set_title('Velocity X')
        sf3 = fig2.add_subplot(3, 3, 3)
        sf3.set_title('Position X')

        sf4 = fig2.add_subplot(3, 3, 4)
        sf4.set_title('Accelerometer Y')
        sf5 = fig2.add_subplot(3, 3, 5)
        sf5.set_title('Velocity Y')
        sf6 = fig2.add_subplot(3, 3, 6)
        sf6.set_title('Position Y')

        sf7 = fig2.add_subplot(3, 3, 7)
        sf7.set_title('Accelerometer Z')
        sf8 = fig2.add_subplot(3, 3, 8)
        sf8.set_title('Velocity Z')
        sf9 = fig2.add_subplot(3, 3, 9)
        sf9.set_title('Position Z')

        x = [float(0)]
        pl1 = sf1.plot(x, self.accelerometer[0])
        pl2 = sf2.plot(x, self.velocity[0])
        pl3 = sf3.plot(x, self.position[0])

        pl4 = sf4.plot(x, self.accelerometer[1])
        pl5 = sf5.plot(x, self.velocity[1])
        pl6 = sf6.plot(x, self.position[1])

        pl7 = sf7.plot(x, self.accelerometer[2])
        pl8 = sf8.plot(x, self.velocity[2])
        pl9 = sf9.plot(x, self.position[2])

        fig2.canvas.draw()

        while True:
            if self.data:
                try:
                    print "1"

                    quaternion = np.array([float(self.data[0]), float(self.data[1]),
                                           float(self.data[2]), float(self.data[3])])
                    read_position = np.array([float(self.data[10]), float(self.data[11]), float(self.data[12])])
                    [xs, ys, zs] = self.rotatePoint(points, quaternion, read_position)
                    pl._offsets3d = (-xs, zs, -ys)
                    fig.canvas.draw()

                    print "2"

                    self.accelerometer[0].append(float(self.data[4]))
                    self.accelerometer[1].append(float(self.data[5]))
                    self.accelerometer[2].append(float(self.data[6]))

                    self.velocity[0].append(float(self.data[7]))
                    self.velocity[1].append(float(self.data[8]))
                    self.velocity[2].append(float(self.data[9]))

                    self.position[0].append(float(self.data[10]))
                    self.position[1].append(float(self.data[11]))
                    self.position[2].append(float(self.data[12]))

                    print "3"

                    x = np.arange(0, len(self.accelerometer[0]))
                    x.astype(float)
                    xnparray = np.array(x)
                    pl1[0].set_ydata(np.array(self.accelerometer[0]))
                    pl1[0].set_xdata(xnparray)
                    pl2[0].set_ydata(np.array(self.velocity[0]))
                    pl2[0].set_xdata(xnparray)
                    pl3[0].set_ydata(np.array(self.position[0]))
                    pl3[0].set_xdata(xnparray)

                    pl4[0].set_ydata(np.array(self.accelerometer[1]))
                    pl4[0].set_xdata(xnparray)
                    pl5[0].set_ydata(np.array(self.velocity[1]))
                    pl5[0].set_xdata(xnparray)
                    pl6[0].set_ydata(np.array(self.position[1]))
                    pl6[0].set_xdata(xnparray)

                    pl7[0].set_ydata(np.array(self.accelerometer[2]))
                    pl7[0].set_xdata(xnparray)
                    pl8[0].set_ydata(np.array(self.velocity[2]))
                    pl8[0].set_xdata(xnparray)
                    pl9[0].set_ydata(np.array(self.position[2]))
                    pl9[0].set_xdata(xnparray)

                    print "4"

                    if len(x) > 100:
                        sf1.set_xlim(len(x)-100, len(x))
                        sf2.set_xlim(len(x)-100, len(x))
                        sf3.set_xlim(len(x)-100, len(x))
                        sf4.set_xlim(len(x)-100, len(x))
                        sf5.set_xlim(len(x)-100, len(x))
                        sf6.set_xlim(len(x)-100, len(x))
                        sf7.set_xlim(len(x)-100, len(x))
                        sf8.set_xlim(len(x)-100, len(x))
                        sf9.set_xlim(len(x)-100, len(x))
                    else:
                        sf1.set_xlim(0, len(x))
                        sf2.set_xlim(0, len(x))
                        sf3.set_xlim(0, len(x))
                        sf4.set_xlim(0, len(x))
                        sf5.set_xlim(0, len(x))
                        sf6.set_xlim(0, len(x))

                        sf7.set_xlim(0, len(x))
                        sf8.set_xlim(0, len(x))
                        sf9.set_xlim(0, len(x))

                    print "5"
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

                    #print self.accelerometerLimits
                    #print self.velocityLimits
                    #print self.positionLimits

                    sf1.set_ylim(self.accelerometerLimits[0], self.accelerometerLimits[1])
                    sf2.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                    sf3.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                    sf4.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))
                    sf5.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                    sf6.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                    sf7.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))
                    sf8.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                    sf9.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))

                    print "6"

                    fig2.canvas.draw()

                    print "7"

                    self.data = []

                except ValueError:
                    # print "Non float value in message"
                    pass
            else:
                sleep(0.01)
        plt.show()

    def rotatePoint(self, points, quaternion, position):
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
        transformed = points.dot(np.transpose(R))
        xs = np.squeeze(np.asarray(transformed[:, 0])) + position[0]
        ys = np.squeeze(np.asarray(transformed[:, 1]))
        zs = np.squeeze(np.asarray(transformed[:, 2])) - position[2]
        return [xs, ys, zs]

if __name__ == '__main__':
    vis = Visualizer()
