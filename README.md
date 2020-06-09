# TipOfTheSpearBot

[![Task Force Elite Logo](https://content.invisioncic.com/f299184/monthly_2020_05/Logo_Force_TSTFE.png.ce5720e9c45f10b2776bd2e38d5e7e36.png)](https://www.taskforceelite.com/)

Discord bot for [Tip of the Spear - Task Force Elite](https://www.taskforceelite.com/) by [Red Jake Studios](https://redjake.com/).

### Functionality

#### Player Count
**?pc**

Displays the current player count.

#### Announcements

Configure announcements to be posted in the General channel at certain intervals.

Only available for RJS_Psycho.

The created announcements get persisted to a file *announcements.json* so they survive a bot restart.

**?announcement create {interval} {text}**

Posts {text} in the General channel every {interval} seconds.

example: ?announcement create 3600 Hello, World!

3600 seconds = 1 hour

**?announcement list**

Returns a list of all active announcements and their IDs.

**?announcement delete {id}**

Removes the announcement with the given id, stopping it from being posted again.
