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
        self.local_dns = {}
        self.client_rules = {} # {client_ip: {"blocked": bool}}
        self.upstream_dns = ["8.8.8.8", "8.8.4.4"]
        self.doh_upstream = ""
        self.dot_upstream = ""
        self.dashboard_password = ""
        self.privacy_level = "SHOW_ALL"
        self.load_config()
        self.load_blocklists()

    def load_config(self):
        if os.path.exists(CONFIG_PATH):
            try:
                with open(CONFIG_PATH, 'r') as f:
                    config = json.load(f)
                    self.custom_blocked = set(config.get('custom_blocked', []))
                    self.custom_allowed = set(config.get('custom_allowed', []))
                    self.blocklists = config.get('blocklists', [])
                    self.upstream_dns = config.get('upstream_dns', ["8.8.8.8", "8.8.4.4"])
                    self.local_dns = config.get('local_dns', {})
                    self.client_rules = config.get('client_rules', {})
                    self.doh_upstream = config.get('doh_upstream', "")
                    self.dot_upstream = config.get('dot_upstream', "")
                    self.dashboard_password = config.get('dashboard_password', "")
                    self.privacy_level = config.get('privacy_level', "SHOW_ALL")
            except Exception as e:
                print(f"Error loading config: {e}")
                self.save_config()
        else:
            self.save_config()

    def save_config(self):
        config = {
            'custom_blocked': list(self.custom_blocked),
            'custom_allowed': list(self.custom_allowed),
            'blocklists': self.blocklists,
            'upstream_dns': self.upstream_dns,
            'local_dns': self.local_dns,
            'client_rules': self.client_rules,
            'doh_upstream': self.doh_upstream,
            'dot_upstream': self.dot_upstream,
            'dashboard_password': self.dashboard_password,
            'privacy_level': self.privacy_level
        }
        os.makedirs(os.path.dirname(CONFIG_PATH), exist_ok=True)
        with open(CONFIG_PATH, 'w') as f:
            json.dump(config, f)

    def load_blocklists(self):
        self.blocked_domains = set()
        if not os.path.exists(BLOCKLIST_DIR):
            os.makedirs(BLOCKLIST_DIR, exist_ok=True)
            return
        for filename in os.listdir(BLOCKLIST_DIR):
            try:
                with open(os.path.join(BLOCKLIST_DIR, filename), 'r') as f:
                    for line in f:
                        domain = line.strip()
                        if domain and not domain.startswith('#'):
                            if ' ' in domain: domain = domain.split(' ')[-1]
                            self.blocked_domains.add(domain)
            except Exception as e: print(f"Error loading {filename}: {e}")

    def is_blocked(self, domain, client_ip=None):
        if client_ip and self.client_rules.get(client_ip, {}).get("blocked", False):
            return True
        if domain in self.custom_allowed: return False
        if domain in self.custom_blocked: return True
        return domain in self.blocked_domains

    def resolve_local(self, domain): return self.local_dns.get(domain)

    def update_blocklists(self):
        os.makedirs(BLOCKLIST_DIR, exist_ok=True)
        for url in self.blocklists:
            try:
                r = requests.get(url, timeout=10)
                if r.status_code == 200:
                    filename = url.replace('https://', '').replace('http://', '').replace('/', '_') + ".txt"
                    with open(os.path.join(BLOCKLIST_DIR, filename), 'w') as f: f.write(r.text)
            except Exception as e: print(f"Error updating {url}: {e}")
        self.load_blocklists()
