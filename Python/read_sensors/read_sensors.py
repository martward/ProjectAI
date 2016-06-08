import socket
import matplotlib.pyplot as plt
import numpy as np
import math, time

#from mpl_toolkits.mplot3d import axes3d
#from matplotlib.path import Path
#import matplotlib.patches as patches
from mpl_toolkits.mplot3d.art3d import Poly3DCollection

THETAS = np.radians([[10.,10.,10.], [-10.,35.,35.], [180.,-45.,90.]])

ax = 0

x = [-5,-5,5,5]
y = [-2.5,2.5,2.5,-2.5]
z = [0,0,0,0,0]

verts = [zip(x, y,z)]

def main():
    initPlot()
    plotPoints(0, 0, 0)
    handle_connection()


def handle_connection():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect(('8.8.8.8', 0))
        print "binding"
        try:
            s.bind((s.getsockname()[0], 9090))
        except socket.error, msg:
            print msg
        print "Listening"
        s.listen(1)
        c,addr = s.accept()
        print c
        print "jup"
        while 1:
            try:
                msg, addr = c.recvfrom(1024)
                msg = msg[2:]
                print msg
                msg = msg.split("/")
                if msg[0] == "stop":
                    print "Receiving messages stopped."
                    break
                elif msg[0] == "relative":
                    plotPoints(float(msg[1]), float(msg[2]), float(msg[3]))
                    continue
                else:
                    continue
            except:
                print "Connection Lost"
                c.close()
                s.close()
                break
    except:
        "No connection found"

def initPlot():
    global ax
    fig = plt.figure(figsize = (22,15))
    ax = fig.add_subplot(111, projection='3d')
    ax.set_xlim([-10,10])
    ax.set_ylim([-10,10])
    ax.set_zlim([-10,10])

def plotPoints(THETA_X, THETA_Y, THETA_Z):
    ROTX = np.array([[1,0,0],[0,math.cos(THETA_X),-math.sin(THETA_X)],[0,math.sin(THETA_X),math.cos(THETA_X)]])
    ROTY = np.array([[math.cos(THETA_Y),0,math.sin(THETA_Y)],[0,1,0],[-math.sin(THETA_Y),0,math.cos(THETA_Y)]])
    ROTZ = np.array([[math.cos(THETA_Z),-math.sin(THETA_Z),0],[math.sin(THETA_Z),math.cos(THETA_Z),0],[0,0,1]])
    ROT = ROTX.dot(ROTY.dot(ROTZ))

    downLeft = np.asarray(verts[0][0]).dot(ROT)
    upLeft = np.asarray(verts[0][1]).dot(ROT)
    upRight = np.asarray(verts[0][2]).dot(ROT)
    downRight = np.asarray(verts[0][3]).dot(ROT)

    verts[0][0] = tuple(map(tuple, [downLeft]))[0]
    verts[0][1] = tuple(map(tuple, [upLeft]))[0]
    verts[0][2] = tuple(map(tuple, [upRight]))[0]
    verts[0][3] = tuple(map(tuple, [downRight]))[0]

    ax.cla()
    surface = ax.add_collection3d(Poly3DCollection(verts))
    #plt.pause(1)

if __name__ == '__main__':
    main()