# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - 0.1.0-SNAPSHOT

### Added
- In-memory session store provider with LRU eviction
- Table-based session store provider using QQQ tables
- Redis session store provider with native TTL via Jedis
- Custom provider support via QCodeReference
- Sliding expiration (configurable per-provider)
- Scheduled cleanup process for expired sessions
- QBit producer for declarative configuration
