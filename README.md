# LPA Unbound

LPA Unbound is an Xposed module for HyperOS's eUICC stack. It allows a platform-signed LPA installed under `/data/app` to participate in LPA selection without mounting files into a system partition.

After installation, you should see "Enable eSIM" toggle in the Network settings.

## Requirements

- Hardware with eUICC support
- Android 13 or newer
- libxposed API 102
- The LPA must already be signed with the device platform certificate and declare the permissions/components required by `EuiccConnector`.
