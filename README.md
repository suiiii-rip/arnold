# MediaSpyy Bot

A simple twitch bot that responds to commands in chat which are being
configured using a REST interface.

## SBT config

## Build

```bash
./sbt clean compile
```

## TODO

- Config from environment with zio-config
- add twitch4j
- connect to chat with bot account
- provide command prefix (!)
- provide handler to custom event handling thingy within zio
- send the command from command service if exists
