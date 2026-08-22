[![Release](https://img.shields.io/github/v/release/aparmar2000/XenForoPostScheduler?style=flat)](https://github.com/aparmar2000/XenForoPostScheduler/releases)
[![Java 17](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat)](https://opensource.org/licenses/MIT)

# XenForo Post Scheduler

This is a desktop application designed to automate, schedule, and safely dispatch posts to XenForo-powered discussion forums. It handles things like scheduled posting intervals and rate limiting, and (basic) encrypted credential storage, along with a live BBCode editor preview.

This was created to make managing scheduled forum posts easy and reasonably safe. Feel free to open an issue or pull request if you run into a bug or have feature suggestions.

## Features

- **Automated Scheduling**: Dispatch posts at specific dates/times or recurring intervals with priority queuing.
- **Safety & Conditional Rules**: Pre-dispatch checks including anti-necropost protection, post cooldown intervals, active thread verification, and time-of-day posting windows.
- **BBCode Editor & Live Preview**: Syntax-highlighted editor with formatting toolbar and real-time BBCode rendering.
- **Multi-Account & Security**: Manage multiple forum profiles with credentials encrypted at rest via AES-256-GCM.
- **Rate Limiting & Logging**: Hardcoded posting and scraping rate limits to minimize accidental spam and forum server load.
- **Plugin System**: Assuming you have some skill in Java, it can be easily extended with custom rules & editor toolbar shortcuts.

## Download & Usage

1. Make sure you have **Java 17** (or newer) installed.
2. Download the latest packaged `.jar` from the [Releases](https://github.com/aparmar2000/XenForoPostScheduler/releases) page.
3. Launch by double-clicking the `.jar` file, or run from the command line:

```bash
java -jar XenForoPostScheduler-0.2.1.jar
```

## Building from Source

To build the executable `.jar` yourself from source with Maven:

```bash
mvn clean package
```

The compiled `.jar` will be placed in the `target/` directory.

## License

This project is licensed under the [MIT License](LICENSE).
