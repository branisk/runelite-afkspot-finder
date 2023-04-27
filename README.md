# AFKspot Finder

This plugin finds nearby npcs and their paths, and marks the tiles with the highest density of unique npcs passing over it. This allows for you to find a location where you are benefitting the most from afking combat, as you will be spending the least amount of time waiting for an npc to attack you. The config allows for you to determine how many tiles you want to display, based on a sorted list of tiles starting with the tile with the highest density of movement.


## Example:

![image](https://user-images.githubusercontent.com/56201891/234792548-dfa8ac43-aa6f-470f-a506-47474c6d3b1a.png)


## Overlay:

The color of the tile denotes how many NPC's touched the respective tile.
- Green: 1 NPC
- Yellow: 2 NPC's
- Red: 3 or more NPC's


## Config:
- *Number of Tiles*: Choose how many tiles to display, sorted by highest density
- *NPC Name*: The name of the NPC whose path to track.  If left blank, tracks all attackable NPC's
