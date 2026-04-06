import asyncio
import os
import aiodns
import random
from .blocklist import BlocklistManager
from .logger import log_query, init_db
from .updater import Updater

UPSTREAM_DNS = ['8.8.8.8', '8.8.4.4']
DNS_PORT = 5353
DNS_HOST = '127.0.0.1'

# List of domains for simulated queries during mock testing
MOCK_DOMAINS = [
    "google.com", "facebook.com", "doubleclick.net", "analytics.google.com",
    "github.com", "amazon.com", "adnxs.com", "taboola.com", "reddit.com",
    "twitter.com", "googlesyndication.com", "yahoo.com", "bing.com"
]

class DNSServer:
    def __init__(self, blocklist_manager):
        self.blocklist_manager = blocklist_manager
        self.resolver = aiodns.DNSResolver(nameservers=UPSTREAM_DNS)

    async def handle_query(self, data, addr, transport):
        # Placeholder for real DNS parsing using dnslib
        # For simulation, we randomly pick a domain if parsing is not implemented
        domain = random.choice(MOCK_DOMAINS)
        client_ip = addr[0]

        if self.blocklist_manager.is_blocked(domain):
            status = "BLOCKED"
            log_query(domain, client_ip, status)
            print(f"Blocked: {domain} from {client_ip}")
            return

        status = "ALLOWED"
        log_query(domain, client_ip, status)
        print(f"Allowed: {domain} from {client_ip}")

        try:
            # result = await self.resolver.query(domain, 'A')
            pass
        except Exception as e:
            print(f"Error resolving {domain}: {e}")

    async def start(self):
        init_db()
        loop = asyncio.get_running_loop()
        transport, protocol = await loop.create_datagram_endpoint(
            lambda: DNSProtocol(self.handle_query),
            local_addr=(DNS_HOST, DNS_PORT)
        )
        print(f"DNS Server listening on {DNS_HOST}:{DNS_PORT}")
        try:
            while True:
                await asyncio.sleep(3600)
        finally:
            transport.close()

class DNSProtocol(asyncio.DatagramProtocol):
    def __init__(self, handler):
        self.handler = handler

    def datagram_received(self, data, addr):
        asyncio.create_task(self.handler(data, addr, self.transport))

    def connection_made(self, transport):
        self.transport = transport

def main():
    manager = BlocklistManager()

    # Start Updater
    updater = Updater(manager)
    updater.start()

    server = DNSServer(manager)
    asyncio.run(server.start())

if __name__ == "__main__":
    main()
