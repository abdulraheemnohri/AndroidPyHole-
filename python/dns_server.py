import asyncio
import os
import aiodns
import random
import json
from dnslib import DNSRecord, QTYPE, RR, A
from .blocklist import BlocklistManager, CONFIG_PATH
from .logger import log_query, init_db
from .updater import Updater

DNS_PORT = 5353
DNS_HOST = '127.0.0.1'

class DNSServer:
    def __init__(self, blocklist_manager):
        self.blocklist_manager = blocklist_manager
        self.update_resolver()

    def update_resolver(self):
        self.resolver = aiodns.DNSResolver(nameservers=self.blocklist_manager.upstream_dns)

    async def handle_query(self, data, addr, transport):
        try:
            request = DNSRecord.parse(data)
        except Exception as e:
            print(f"Failed to parse DNS query: {e}")
            return

        qname = str(request.q.qname).rstrip('.')
        qtype = QTYPE[request.q.qtype]
        client_ip = addr[0]

        # Check Local DNS
        local_ip = self.blocklist_manager.resolve_local(qname)
        if local_ip and qtype == 'A':
            reply = request.reply()
            reply.add_answer(RR(qname, QTYPE.A, rdata=A(local_ip), ttl=60))
            transport.sendto(reply.pack(), addr)
            log_query(qname, client_ip, "LOCAL")
            return

        # Check Blocklist
        if self.blocklist_manager.is_blocked(qname, client_ip):
            reply = request.reply()
            # reply.header.rcode = getattr(RCODE, 'NXDOMAIN')
            transport.sendto(reply.pack(), addr)
            log_query(qname, client_ip, "BLOCKED")
            return

        log_query(qname, client_ip, "ALLOWED")

        # Forward to Upstream
        try:
            # Note: real implementation would handle non-A types and proper forwarding
            # Here we use aiodns for simplicity in the blueprint
            if qtype == 'A':
                result = await self.resolver.query(qname, 'A')
                reply = request.reply()
                for r in result:
                    reply.add_answer(RR(qname, QTYPE.A, rdata=A(r.host), ttl=r.ttl))
                transport.sendto(reply.pack(), addr)
        except Exception as e:
            print(f"Error resolving {qname}: {e}")
            # Send back original or error
            transport.sendto(data, addr)

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
