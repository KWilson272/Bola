# ElectroBola
ElectroBola is an addon ability for the ProjectKorra plugin. Using this ability, chiblockers can 
throw electrified bolas at their enemies to stun them momentarily! Stunned entities
will be unable to move for a period of time.
#### Usage
To use this ability, tap-sneak. Additionally, where the config allows it, drag your mouse to direct 
the bola.

### Installation
To install this ability, place the ElectroBola.jar into your minecraft server's
``plugins/ProjectKorra/Abilities/`` directory. 

#### Requirements
This ability was made for the following versions. It is not guaranteed to work on different versions.
- ProjectKorra ``1.11.2``
- Spigot ``1.20.1``

### Config Documentation
```yaml
ExtraAbilities:
  KWilson272:
    ElectroBola:
      Enabled: true
      OnHitCooldown: 12000
      OnMissCooldown: 8500
      Speed: 1.5
      Range: 25.0
      StunDuration: 2100
      AffectRadius: 1.0
      CollisionRadius: 1.0
      VerticalSensitivity: 10.0
      HorizontalSensitivity: 30.0
```
- **OnHitCooldown** - Cooldown applied when the ability hits an entity; in milliseconds.
- **OnMissCooldown** - Cooldown applied if the ability removes without hitting an entity; in milliseconds.
- **Speed** - How many blocks the ability travels per tick.
- **Range** - How many blocks the ability can travel during its lifetime.
- **StunDuration** - How long the hit entity is immobilized for.
- **AffectRadius** - The distance from the center of the ability in which entities are affected.
- **CollisionRadius** - The distance from the center of the ability in which other abilities will collide. (No collisions are enabled by default)
- **VerticalSensitivity** - How controllable the ability is on the y-axis. (0-100 inclusive; 0 = no vertical control, 100 = total vertical control) 
- **HorizontalSensitivity** - How controllable the ability is on the x/z axes. (0-100 inclusive; 0 = no horizontal control, 100 = total horizontal control)