import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from provider_budget import ProviderBudget

class ProviderBudgetTests(unittest.TestCase):
    def setUp(self):
        self.folder=tempfile.TemporaryDirectory()
        self.env=patch.dict(os.environ,{'AI_RATE_STATE_PATH':str(Path(self.folder.name)/'budget.sqlite3'),'AI_REQUESTS_PER_MINUTE':'6'})
        self.env.start()
    def tearDown(self):
        self.env.stop();self.folder.cleanup()
    def test_shared_reservations_block_concurrency_and_bursts(self):
        with patch('provider_budget.time.time',return_value=1000):
            first=ProviderBudget('nvidia');self.assertTrue(first.acquire())
            self.assertFalse(ProviderBudget('nvidia').acquire())
            first.release()
            self.assertFalse(ProviderBudget('nvidia').acquire())
            self.assertTrue(ProviderBudget('openrouter').acquire())
        with patch('provider_budget.time.time',return_value=1011):
            self.assertTrue(ProviderBudget('nvidia').acquire())
    def test_provider_cooldown_persists_across_instances(self):
        with patch('provider_budget.time.time',return_value=1000):
            first=ProviderBudget('nvidia');self.assertTrue(first.acquire())
            first.cooldown('60');first.release()
        with patch('provider_budget.time.time',return_value=1020):
            other=ProviderBudget('nvidia');self.assertFalse(other.acquire())
            self.assertGreaterEqual(other.retry_after,40)
    def test_parallel_workers_share_one_active_reservation(self):
        from concurrent.futures import ThreadPoolExecutor
        with ThreadPoolExecutor(max_workers=4) as executor:
            results=list(executor.map(lambda _:ProviderBudget('nvidia').acquire(),range(4)))
        self.assertEqual(1,sum(results))
    def test_rolling_window_does_not_allow_more_than_six_calls(self):
        for offset in range(0,60,10):
            with patch('provider_budget.time.time',return_value=1000+offset):
                budget=ProviderBudget('nvidia');self.assertTrue(budget.acquire());budget.release()
        with patch('provider_budget.time.time',return_value=1059):
            self.assertFalse(ProviderBudget('nvidia').acquire())
        with patch('provider_budget.time.time',return_value=1061):
            self.assertTrue(ProviderBudget('nvidia').acquire())
