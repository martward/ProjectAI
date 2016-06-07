import socket


def main():
    handle_connection()


def handle_connection():
    ip = "192.168.0.100"
    port = 9090
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        print "binding"
        try:
            s.bind((ip,port))
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


if __name__ == '__main__':
    main()
