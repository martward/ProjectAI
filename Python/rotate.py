import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import math, time

THETAS = np.radians([[10.,10.,10.], [-10.,35.,35.], [180.,-45.,90.]])

def main():
    normal = np.array([[0,0,1]])
    xx, yy = np.meshgrid(range(6), range(6))
    z = (-normal[0][0] * xx - normal[0][1] * yy) * 1. /normal[0][2]
    
    plt3d = plt.figure().gca(projection='3d')
    plt3d.plot_surface(xx, yy, z)
    plt.draw()
    
    for i in range(3):
        THETA_X = THETAS[i][0]
        THETA_Y = THETAS[i][1]
        THETA_Z = THETAS[i][2]
        
        ROTX = np.array([[1,0,0],[0,math.cos(THETA_X),-math.sin(THETA_X)],[0,math.sin(THETA_X),math.cos(THETA_X)]])
        ROTY = np.array([[math.cos(THETA_Y),0,math.sin(THETA_Y)],[0,1,0],[-math.sin(THETA_Y),0,math.cos(THETA_Y)]])
        ROTZ = np.array([[math.cos(THETA_Z),-math.sin(THETA_Z),0],[math.sin(THETA_Z),math.cos(THETA_Z),0],[0,0,1]])
        ROT = ROTX.dot(ROTY.dot(ROTZ))
        normal = normal.dot(ROT)
        z = (-normal[0][0] * xx - normal[0][1] * yy) * 1. /normal[0][2]

        plt
        plt3d.plot_surface(xx, yy, z)
        plt.draw()
        
if __name__ == "__main__":
    main()
