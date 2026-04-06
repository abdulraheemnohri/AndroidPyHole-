import asyncio
import os
import aiodns
import random
import json
from .blocklist import BlocklistManager, CONFIG_PATH
from .logger import log_query, init_db
from .updater import Updater

DNS_PORT = 5353
DNS_HOST = '127.0.0.1'

MOCK_DOMAINS = [
    "google.com", "facebook.com", "doubleclick.net", "analytics.google.com",
    "github.com", "amazon.com", "adnxs.com", "taboola.com", "reddit.com",
    "twitter.com", "googlesyndication.com", "yahoo.com", "bing.com"
]

class DNSServer:
    def __init__(self, blocklist_manager):
        self.blocklist_manager = blocklist_manager
        self.update_resolver()

    def update_resolver(self):
        self.resolver = aiodns.DNSResolver(nameservers=self.blocklist_manager.upstream_dns)

    async def handle_query(self, data, addr, transport):
        # Simulation: Pick random domain or use provided (if parsed)
        domain = random.choice(MOCK_DOMAINS)
        client_ip = addr[0]

        # Check Local DNS
        local_ip = self.blocklist_manager.resolve_local(domain)
        if local_ip:
            status = "LOCAL"
            log_query(domain, client_ip, status)
            print(f"Local Resolution: {domain} -> {local_ip}")
            return

        if self.blocklist_manager.is_blocked(domain):
            status = "BLOCKED"
            log_query(domain, client_ip, status)
            return

        status = "ALLOWED"
        log_query(domain, client_ip, status)

        try:
            # await self.resolver.query(domain, 'A')
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
    updater = Updater(manager)
    updater.start()
    server = DNSServer(manager)
    asyncio.run(server.start())

if __name__ == "__main__":
    main()
