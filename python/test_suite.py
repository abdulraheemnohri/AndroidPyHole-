import unittest
import os
import sqlite3
import json
from blocklist import BlocklistManager
from logger import init_db, log_query, get_recent_logs

class TestPyHole(unittest.TestCase):
    def setUp(self):
        init_db()

    def test_logger(self):
        log_query("test.com", "127.0.0.1", "ALLOWED")
        logs = get_recent_logs(1)
        self.assertEqual(len(logs), 1)
        self.assertEqual(logs[0][1], "test.com")

    def test_blocklist_logic(self):
        manager = BlocklistManager()
        manager.custom_blocked.add("bad.com")
        self.assertTrue(manager.is_blocked("bad.com"))
        self.assertFalse(manager.is_blocked("good.com"))

        # Test client specific blocking
        manager.client_rules["10.0.0.5"] = {"blocked": True}
        self.assertTrue(manager.is_blocked("google.com", "10.0.0.5"))

if __name__ == "__main__":
    unittest.main()
