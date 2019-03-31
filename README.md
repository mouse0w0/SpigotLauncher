# SpigotLauncher
[![](https://jitpack.io/v/Mouse0w0/SpigotLauncher.svg)](https://jitpack.io/#Mouse0w0/SpigotLauncher)

A launcher for better modify Spigot server. To transform bytecode based on ASM.

**Warning: This project is developing. Highly possible to change API.**

## How to run

Windows:
```
java -jar SpigotLauncher.jar --server-file Spigot.jar
```

## How to develop a CorePlugin
Please to see Demo：https://github.com/Mouse0w0/CorePluginDemo

## How to export a transformed server

Windows:
```
java -jar SpigotLauncher.jar --server-file Spigot.jar --static-mode --static-output ./server_transformed.jar
```
