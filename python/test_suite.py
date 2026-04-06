import unittest
import os
import sqlite3
import json
from blocklist import BlocklistManager
from logger import init_db, log_query, get_recent_logs

class TestPyHole(unittest.TestCase):
    def setUp(self):
        self.db_path = os.path.join(os.path.dirname(__file__), '../storage/logs/query_log.db')
        self.config_path = os.path.join(os.path.dirname(__file__), '../storage/config.json')
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

if __name__ == "__main__":
    unittest.main()
