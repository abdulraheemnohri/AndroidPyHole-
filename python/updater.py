import time
import threading
from .blocklist import BlocklistManager

class Updater:
    def __init__(self, manager, interval=86400): # Default daily
        self.manager = manager
        self.interval = interval
        self.running = False

    def start(self):
        self.running = True
        self.thread = threading.Thread(target=self._run)
        self.thread.daemon = True
        self.thread.start()

    def _run(self):
        while self.running:
            print("Updating blocklists...")
            self.manager.update_blocklists()
            time.sleep(self.interval)

    def stop(self):
        self.running = False
