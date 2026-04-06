import subprocess
import socket
import os

def scan_network():
    """
    Simple network scanner blueprint.
    On Android, scanning the ARP table (/proc/net/arp) is the most common method.
    """
    devices = []
    if os.path.exists('/proc/net/arp'):
        try:
            with open('/proc/net/arp', 'r') as f:
                next(f) # Skip header
                for line in f:
                    parts = line.split()
                    if len(parts) >= 4:
                        ip = parts[0]
                        mac = parts[3]
                        if mac != '00:00:00:00:00:00':
                            devices.append({"ip": ip, "mac": mac, "name": get_hostname(ip)})
        except Exception as e:
            print(f"Error scanning network: {e}")

    # Mock data if scan yields nothing (e.g. on emulator/sandbox)
    if not devices:
        devices = [
            {"ip": "127.0.0.1", "mac": "00:00:00:00:00:00", "name": "Localhost"},
            {"ip": "10.0.0.5", "mac": "AA:BB:CC:DD:EE:FF", "name": "Blueprint Device"}
        ]
    return devices

def get_hostname(ip):
    try:
        return socket.gethostbyaddr(ip)[0]
    except:
        return "Unknown"

if __name__ == "__main__":
    print(scan_network())
