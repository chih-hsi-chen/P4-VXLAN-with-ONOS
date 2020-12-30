# P4-VXLAN-with-ONOS
Implement VXLAN on BMV2 and Use P4Runtime to make ONOS communicate with BMV2

## Prerequisite

1. [ONOS (version 2.2.0)](https://github.com/opennetworkinglab/onos)
2. [P4 Compiler](https://github.com/p4lang/p4c)

## Testing Environment

* [VM file](https://drive.google.com/file/d/14NynWSQ5eJLBVeGjJj8T6So1FPvulFhy/view)
> If you cannot correctly start up the mininet, maybe you need run `upgrade.sh` to upgrade your python modules

## Folder Structure

```
├── p4
│   ├── utils (runtime environment scripts)
│   └── vxlan (P4 code and topology setting)
├── p4extensiontreatment (Define customized ONOS flow actions)
├── pipeconf-p4-final (Pipeconf for our p4 code)
└── vxlan-app (fixed ONOS App for testing functionality)
```

## How to start

1. Download this repository: `git clone https://github.com/chih-hsi-chen/P4-VXLAN-with-ONOS.git`
2. Add executable permission to `startup.sh` and `build.sh`
3. Run `./build.sh`
4. Start ONOS
5. Run `./startup.sh`
6. Open another terminal and run `onos-app localhost install! vxlan-app/target/vxlan-1.0-SNAPSHOT.oar`
7. Back to mininet CLI and type `pingall`, then all hosts are reachable to one another

## References

* [P4 Tutorials](https://github.com/p4lang/tutorials)
* [ONOS API](http://api.onosproject.org/2.2.0/apidocs/)