from multiprocessing import Queue

class QueueHandler(object):
    def __new__(cls, *args, **kwargs):
        if not hasattr(cls, "_instance"):
            print("__new__ is called\n")
            cls._instance = super().__new__(cls)
            max_size = 10000
            cls.queue = Queue(maxsize=max_size)
        return cls._instance

    def __init__(self):
        print("__init__ is called\n")

    def put(self, item):
        self.queue.put(item)

    def get(self):
        return self.queue.get()

    def current_size(self):
        return self.queue.qsize()



if __name__ == '__main__':
    que = QueueHandler()
    que.put('AAAA')
    print(que.current_size())
    print(que.get())
    print(que.current_size())
