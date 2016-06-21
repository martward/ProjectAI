import socket
import csv
import time

class Visualizer:

    data = []
    millis = None

    def __init__(self):
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

            self.millis = int(round(time.time() * 1000))

            with open('datadump/%s.csv' % self.millis, 'wb') as csvfile:
                datadump = csv.writer(csvfile)
                datadump.writerow(['Acc X', 'Acc Y', 'Acc Z', 'Vel X', 'Vel Y', 'Vel Z', 'Tr X', 'Tr Y', 'Tr Z',
                                   'Euler X', 'Euler Y', 'Euler Z', 'Rot-Acc X', 'Rot-Acc Y', 'Rot-Acc Z',
                                   'Rot-Vel X', 'Rot-Vel Y', 'Rot-Vel Z', 'Rot-Tr X', 'Rot-Tr Y', 'Rot-Tr Z'])

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
                        self.data = msg
                        self.writeToCSV(msg)
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

    def writeToCSV(self, msg):
        with open('datadump/%s.csv' % self.millis, 'a') as csvfile:
            datadump = csv.writer(csvfile)
            datadump.writerow([msg[0], msg[1], msg[2], msg[3], msg[4], msg[5],
                               msg[6], msg[7], msg[8], msg[9], msg[10],
                               msg[11], msg[12], msg[13], msg[14], msg[15],
                               msg[16], msg[17], msg[18], msg[19], msg[20]])

if __name__ == '__main__':
    vis = Visualizer()
