**Run as admin**

see also https://www.windowspro.de/wolfgang-sommergut/nat-switch-hyper-v-einrichten-windows-10-server-2016
```
$> New-NetNat -Name LinuxNat -InternalIPInterfaceAddressPrefix <ip of hyper-v-container/32>
```

```
$> Add-NetNatStaticMapping 
    -NatName LinuxNat 
    -Protocol TCP 
    -ExternalIPAddress 0.0.0.0 
    -ExternalPort 5757 
    -InternalIPAddress <ip of hyper-v-container>
    -InternalPort 5757
    
$> Remove-NetNatStaticMapping
```