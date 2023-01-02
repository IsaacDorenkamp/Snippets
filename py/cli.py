import socket
import json
import traceback

if __name__ == '__main__':
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('127.0.0.1', 3194))

    auth = input('Enter auth token > ')
    
    inp = input('> ')
    closed = False

    try:
        while inp.strip() != '.exit':
            if inp != '-':
                obj = None
                try:
                    obj = json.loads(inp)
                except json.decoder.JSONDecodeError:
                    print("Please enter a valid JSON object.")
                    inp = input('> ')
                    continue
                obj['auth'] = auth
                toSend = json.dumps(obj)
            else:
                toSend = '-'
            s.send(toSend.encode()) # TODO: write code to guarantee all content is sent
            response = s.recv(1024).decode() # TODO: write code to guarantee all content is received
            if response == '-':
                # connection is ended
                closed = True
                break
            data = json.loads(response)
            print("Success" if data['code'] == 0 else "Error")
            print(data['data'])
            inp = input('> ')
    except Exception:
        traceback.print_exc()
    finally:
        if not closed:
            s.send("-".encode())
            resp = s.recv(1).decode()
            if resp != "-":
                print("Warning: Connection did not close properly.")
            else:
                print("Connection closed successfully.")
        
