import os
import json
import requests

CONFIG_PATH = os.path.join(os.path.dirname(__file__), '../storage/config.json')
BLOCKLIST_DIR = os.path.join(os.path.dirname(__file__), '../storage/blocklists/')

class BlocklistManager:
    def __init__(self):
        self.blocklists = []
        self.custom_blocked = set()
        self.custom_allowed = set()
        self.load_config()
        self.load_blocklists()

    def load_config(self):
        if os.path.exists(CONFIG_PATH):
            with open(CONFIG_PATH, 'r') as f:
                config = json.load(f)
                self.custom_blocked = set(config.get('custom_blocked', []))
                self.custom_allowed = set(config.get('custom_allowed', []))
                self.blocklists = config.get('blocklists', [])
        else:
            self.save_config()

    def save_config(self):
        config = {
            'custom_blocked': list(self.custom_blocked),
            'custom_allowed': list(self.custom_allowed),
            'blocklists': self.blocklists
        }
        with open(CONFIG_PATH, 'w') as f:
            json.dump(config, f)

    def load_blocklists(self):
        self.blocked_domains = set()
        for filename in os.listdir(BLOCKLIST_DIR):
            with open(os.path.join(BLOCKLIST_DIR, filename), 'r') as f:
                for line in f:
                    domain = line.strip()
                    if domain and not line.startswith('#'):
                        self.blocked_domains.add(domain)

    def is_blocked(self, domain):
        if domain in self.custom_allowed:
            return False
        if domain in self.custom_blocked:
            return True
        return domain in self.blocked_domains

    def update_blocklists(self):
        os.makedirs(BLOCKLIST_DIR, exist_ok=True)
        for url in self.blocklists:
            try:
                r = requests.get(url)
                if r.status_code == 200:
                    filename = url.split('/')[-1] or "list.txt"
                    with open(os.path.join(BLOCKLIST_DIR, filename), 'w') as f:
                        f.write(r.text)
            except Exception as e:
                print(f"Error updating {url}: {e}")
        self.load_blocklists()
