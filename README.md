# ServerSystem
A Bukkit/Spigot/PaperMC System with an included BanSystem. Requires MongoDB.
<h2>BanSystem</h2>
To use the BanSystem you need to enable it in the Config File. If you want you can change the MongoDatabase.
<h3>Ban-Command</h3>
Usage: /ban {Player} {ID}
Permission: Depends on ID 
description: You can use this command to banmute a player by the name and the ID

<h3>Mute-Command</h3>
Usage: /mute {Player} {ID}
Permission: Depends on ID
description: You can use this command to mute a player by the name and the ID

<h3>Unban-Command</h3>
Usage: /unban {UnbanID}
Permission: serversystem.unban
description: You can use this command to unmute a player by the BanID

<h3>Unmute-Command</h3>
Usage: /unmute {UnmuteID}<br>
Permission: serversystem.unmute
description: You can use this command to unmute a player by the MuteID

<h3>Check-Command</h3>
Usage: /check [{Player};{BanID/MuteID};{UUID}]
Permission: serversystem.check
description: You can use this command to check the History of an Player by name or UUID or by the Ban-/MuteID.

Information:
You have to create the IDs you want to use in the config.yml An example will look like this:

```yaml
ban:
  '1':
    time: 60000
    reason: Your reason here
    permission: ban.1

banids:
  - 1

mute:
  '1':
    time: 60000
    reason: Your reason here
    permission: mute.1

muteids:
  - 1
 ```
 
You have to set the time in milliseconds. The Reason can be any String you want. It is recommed to set the permission with "." between categorys.

<h2>Other Features</h2>
<h3>Reboot-Command</h3>
Usage: /reboot
Permission: serversystem.reboot
description: You can use this command to reboot the Server with a notification 10 Seconds before it will stop.

<h3>Heal-Command</h3>
Usage: /heal {Player}
Permission: serversystem.heal
description: You can use this command to heal a player.

<h3>Feed-Command</h3>
Usage: /feed {Player}
Permission: serversystem.feed
description: You can use this command to feed a player.

<h2>Information</h2>
You can set every message the System can send in the messages.yml

