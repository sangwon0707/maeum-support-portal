"""공급자 호출 예산은 SQLite 원자 예약으로 여러 프로세스에서 공유한다."""
import os
from contextlib import closing
from pathlib import Path
import sqlite3
import time
import uuid


class ProviderBudget:
    def __init__(self, provider, timeout=120):
        self.provider = provider
        self.timeout = timeout
        self.token = uuid.uuid4().hex
        default = Path.home() / '.cache/portfolio-ai/provider-budget.sqlite3'
        self.path = Path((os.getenv('AI_RATE_STATE_PATH') or str(default))).expanduser()
        self.rpm = max(1, min(10, int(os.getenv('AI_REQUESTS_PER_MINUTE', '6'))))
        self.retry_after = 0
        self.status = 'LOCAL_RATE_LIMITED'

    def connect(self):
        self.path.parent.mkdir(parents=True, exist_ok=True)
        db = sqlite3.connect(self.path, timeout=.2, isolation_level=None)
        os.chmod(self.path, 0o600)
        db.execute('CREATE TABLE IF NOT EXISTS calls (provider TEXT, token TEXT PRIMARY KEY, started REAL, lease_until REAL)')
        db.execute('CREATE TABLE IF NOT EXISTS cooldown (provider TEXT PRIMARY KEY, until_at REAL)')
        return db

    def acquire(self):
        now = time.time()
        try:
            with closing(self.connect()) as db:
                db.execute('BEGIN IMMEDIATE')
                db.execute('DELETE FROM calls WHERE started < ? AND lease_until < ?', (now-60, now))
                cooldown = db.execute('SELECT until_at FROM cooldown WHERE provider=?', (self.provider,)).fetchone()
                calls = db.execute('SELECT started,lease_until FROM calls WHERE provider=?', (self.provider,)).fetchall()
                blocked_until = max([now] + ([cooldown[0]] if cooldown else [])
                                    + [lease for _, lease in calls if lease > now])
                recent = sorted(started for started, _ in calls if started > now-60)
                if recent:
                    blocked_until = max(blocked_until, recent[-1] + 60/self.rpm)
                if len(recent) >= self.rpm:
                    blocked_until = max(blocked_until, recent[-self.rpm] + 60)
                if blocked_until > now:
                    self.retry_after = max(1, int(blocked_until-now)+1)
                    db.commit()
                    return False
                db.execute('INSERT INTO calls VALUES (?,?,?,?)',
                           (self.provider,self.token,now,now+self.timeout+15))
                db.commit()
                return True
        except (sqlite3.Error, OSError):
            self.status = 'BUDGET_UNAVAILABLE'
            return False

    def cooldown(self, retry_after):
        try:
            seconds = int(retry_after or 60)
        except (ValueError, TypeError):
            from email.utils import parsedate_to_datetime
            try:
                seconds = int(parsedate_to_datetime(retry_after).timestamp() - time.time()) + 1
            except (ValueError, TypeError, OverflowError):
                seconds = 60
        seconds = max(1, min(600, seconds))
        self.retry_after = seconds
        try:
            with closing(self.connect()) as db:
                db.execute('INSERT INTO cooldown VALUES (?,?) ON CONFLICT(provider) DO UPDATE SET until_at=MAX(until_at,excluded.until_at)',
                           (self.provider,time.time()+seconds))
        except (sqlite3.Error, OSError):
            pass

    def release(self):
        try:
            with closing(self.connect()) as db:
                db.execute('UPDATE calls SET lease_until=0 WHERE token=?', (self.token,))
        except (sqlite3.Error, OSError):
            # 해제가 실패해도 lease 만료 이후 다시 시도할 수 있다.
            pass
