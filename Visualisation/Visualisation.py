import socket
import matplotlib.pyplot as plt
import numpy as np
import thread
from time import sleep
import time
from mpl_toolkits.mplot3d import Axes3D


class Visualizer:

    time = 0

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

        N = 7
        times = [0, 0, 0, 0, 0, 0, 0]

        ind = np.arange(N)  # the x locations for the groups
        width = 0.35       # the width of the bars

        fig3, ax3 = plt.subplots()
        rects1 = ax3.bar(ind, times, width, color='r')

        # add some text for labels, title and axes ticks
        ax3.set_ylabel('Times')
        ax3.set_title('Time needed for vis')
        ax3.set_xticks(ind + width)
        ax3.set_xticklabels(('0', '1', '2', '3', '4'))

        fig3.canvas.draw()

        while True:
            if self.data:
                try:
                    print "1"

                    self.time = time.time()
                    '''

                    quaternion = np.array([float(self.data[0]), float(self.data[1]),
                                           float(self.data[2]), float(self.data[3])])
                    read_position = np.array([float(self.data[10]), float(self.data[11]), float(self.data[12])])
                    [xs, ys, zs] = self.rotatePoint(points, quaternion, read_position)
                    pl._offsets3d = (-xs, zs, -ys)
                    fig.canvas.draw()
                    '''
                    print "2"
                    times[0] = time.time() - self.time
                    self.time = time.time()

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
                    times[1] = time.time() - self.time
                    self.time = time.time()

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
                    times[3] = time.time() - self.time
                    self.time = time.time()

                    if len(x) > 50:
                        sf1.set_xlim(len(x)-50, len(x))
                        sf2.set_xlim(len(x)-50, len(x))
                        sf3.set_xlim(len(x)-50, len(x))
                        sf4.set_xlim(len(x)-50, len(x))
                        sf5.set_xlim(len(x)-50, len(x))
                        sf6.set_xlim(len(x)-50, len(x))
                        sf7.set_xlim(len(x)-50, len(x))
                        sf8.set_xlim(len(x)-50, len(x))
                        sf9.set_xlim(len(x)-50, len(x))
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
                    times[4] = time.time() - self.time
                    self.time = time.time()
                    # accelerometer
                    if float(self.data[4]) < self.accelerometerLimits[0]:
                        self.accelerometerLimits[0] = float(self.data[4])
                        sf1.set_ylim(self.accelerometerLimits[0], self.accelerometerLimits[1])
                        sf4.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))
                        sf7.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))

                    if float(self.data[5]) < self.accelerometerLimits[0]:
                        self.accelerometerLimits[0] = float(self.data[5])
                        sf1.set_ylim(self.accelerometerLimits[0], self.accelerometerLimits[1])
                        sf4.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))
                        sf7.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))

                    if float(self.data[6]) < self.accelerometerLimits[0]:
                        self.accelerometerLimits[0] = float(self.data[6])
                        sf1.set_ylim(self.accelerometerLimits[0], self.accelerometerLimits[1])
                        sf4.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))
                        sf7.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))
                        
                    if float(self.data[4]) > self.accelerometerLimits[1]:
                        self.accelerometerLimits[1] = float(self.data[4])
                        sf1.set_ylim(self.accelerometerLimits[0], self.accelerometerLimits[1])
                        sf4.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))
                        sf7.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))

                    if float(self.data[5]) > self.accelerometerLimits[1]:
                        self.accelerometerLimits[1] = float(self.data[5])
                        sf1.set_ylim(self.accelerometerLimits[0], self.accelerometerLimits[1])
                        sf4.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))
                        sf7.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))

                    if float(self.data[6]) > self.accelerometerLimits[1]:
                        self.accelerometerLimits[1] = float(self.data[6])
                        sf1.set_ylim(self.accelerometerLimits[0], self.accelerometerLimits[1])
                        sf4.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))
                        sf7.set_ylim((self.accelerometerLimits[0]), (self.accelerometerLimits[1]))

                    # velocity
                    if float(self.data[7]) < self.velocityLimits[0]:
                        self.velocityLimits[0] = float(self.data[7])
                        sf2.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf5.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf8.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))

                    if float(self.data[8]) < self.velocityLimits[0]:
                        self.velocityLimits[0] = float(self.data[8])
                        sf2.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf5.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf8.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))

                    if float(self.data[9]) < self.velocityLimits[0]:
                        self.velocityLimits[0] = float(self.data[9])
                        sf2.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf5.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf8.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))

                    if float(self.data[7]) > self.velocityLimits[1]:
                        self.velocityLimits[1] = float(self.data[7])
                        sf2.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf5.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf8.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))

                    if float(self.data[8]) > self.velocityLimits[1]:
                        self.velocityLimits[1] = float(self.data[8])
                        sf2.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf5.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf8.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))

                    if float(self.data[9]) > self.velocityLimits[1]:
                        self.velocityLimits[1] = float(self.data[9])
                        sf2.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf5.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))
                        sf8.set_ylim((self.velocityLimits[0]), (self.velocityLimits[1]))

                    # translation
                    if float(self.data[10]) < self.positionLimits[0]:
                        self.positionLimits[0] = float(self.data[10])
                        sf3.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf6.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf9.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))

                    if float(self.data[11]) < self.positionLimits[0]:
                        self.positionLimits[0] = float(self.data[11])
                        sf3.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf6.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf9.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))

                    if float(self.data[12]) < self.positionLimits[0]:
                        self.positionLimits[0] = float(self.data[12])
                        sf3.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf6.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf9.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))

                    if float(self.data[10]) > self.positionLimits[1]:
                        self.positionLimits[1] = float(self.data[10])
                        sf3.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf6.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf9.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))

                    if float(self.data[11]) > self.positionLimits[1]:
                        self.positionLimits[1] = float(self.data[11])
                        sf3.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf6.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf9.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))

                    if float(self.data[12]) > self.positionLimits[1]:
                        self.positionLimits[1] = float(self.data[12])
                        sf3.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf6.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))
                        sf9.set_ylim((self.positionLimits[0]), (self.positionLimits[1]))


                    #print self.accelerometerLimits
                    #print self.velocityLimits
                    #print self.positionLimits


                    print "6"
                    times[5] = time.time() - self.time
                    self.time = time.time()

                    #fig2.canvas.update()

                    sf1.draw_artist(sf1.patch)
                    sf1.draw_artist(pl1[0])
                    fig2.canvas.blit(sf1.bbox)

                    sf2.draw_artist(sf2.patch)
                    sf2.draw_artist(pl2[0])
                    fig2.canvas.blit(sf2.bbox)

                    sf3.draw_artist(sf3.patch)
                    sf3.draw_artist(pl3[0])
                    fig2.canvas.blit(sf3.bbox)

                    sf4.draw_artist(sf4.patch)
                    sf4.draw_artist(pl4[0])
                    fig2.canvas.blit(sf4.bbox)

                    sf5.draw_artist(sf5.patch)
                    sf5.draw_artist(pl5[0])
                    fig2.canvas.blit(sf5.bbox)

                    sf6.draw_artist(sf6.patch)
                    sf6.draw_artist(pl6[0])
                    fig2.canvas.blit(sf6.bbox)

                    sf7.draw_artist(sf7.patch)
                    sf7.draw_artist(pl7[0])
                    fig2.canvas.blit(sf7.bbox)
                    fig2.canvas.flush_events()

                    sf8.draw_artist(sf8.patch)
                    sf8.draw_artist(pl8[0])
                    fig2.canvas.blit(sf8.bbox)
                    fig2.canvas.flush_events()

                    sf9.draw_artist(sf9.patch)
                    sf9.draw_artist(pl9[0])
                    fig2.canvas.blit(sf9.bbox)
                    fig2.canvas.flush_events()

                    print "7"
                    times[6] = time.time() - self.time
                    self.time = time.time()

                    ax3.clear()
                    ax3.bar(ind, times, width, color='r')
                    fig3.canvas.draw()

                    self.data = []

                except ValueError:
                    # print "Non float value in message"
                    self.data = []
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
