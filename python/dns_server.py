import asyncio
import os
import aiodns
from .blocklist import BlocklistManager
from .logger import log_query, init_db

UPSTREAM_DNS = ['8.8.8.8', '8.8.4.4']
DNS_PORT = 5353
DNS_HOST = '127.0.0.1'

class DNSServer:
    def __init__(self, blocklist_manager):
        self.blocklist_manager = blocklist_manager
        self.resolver = aiodns.DNSResolver(nameservers=UPSTREAM_DNS)

    async def handle_query(self, data, addr, transport):
        # Placeholder for DNS parsing logic
        # In a real app, use 'dnslib' to parse 'data'
        domain = "example.com" # Mocked domain for demonstration
        client_ip = addr[0]

        if self.blocklist_manager.is_blocked(domain):
            status = "BLOCKED"
            log_query(domain, client_ip, status)
            # Return NXDOMAIN or similar (mocked)
            return

        status = "ALLOWED"
        log_query(domain, client_ip, status)

        try:
            # Mock resolution
            result = await self.resolver.query(domain, 'A')
            # Mock sending back data
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
    server = DNSServer(manager)
    asyncio.run(server.start())

if __name__ == "__main__":
    main()
