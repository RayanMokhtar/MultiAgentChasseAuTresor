# SMA - Chasse au Trésor Multi-Agents

Projet de simulation multi-agents en Java.

## Description

Des agents explorent une carte pour collecter des trésors tout en évitant les animaux dangereux.

**Carte** : 9 zones (3x3), chaque zone = 100 cases (10x10)

## Les 3 types d'agents

- **Réactif** : se déplace aléatoirement, peut mourir
- **Cognitif** : utilise Dijkstra pour trouver le chemin optimal, peut secourir les autres
- **Communicant** : scanne la zone et envoie les infos (trésors, animaux) aux cognitifs

## Objets sur la carte

- Trésors : à collecter
- Animaux : font des dégâts
- Obstacles : bloquent le passage
- QG : point de respawn

## Lancer le projet

```bash
cd sma-explorateurs
mvn compile exec:java
```

## Structure du code

```
agents/          → les 3 types d'agents
environnement/   → Carte, Zone, Case
objets/          → Tresor, Animal, Obstacle
concurrent/      → gestion des threads
gui/             → interface graphique
simulation/      → logique principale
```

## Paramètres modifiables (SimuPara.java)

- Nombre d'agents (réactifs, cognitifs, communicants)
- Nombre de trésors/animaux/obstacles par zone
- Délai entre les actions

## Auteurs

MOKHTARI Rayan - TAGHELIT Wassim - HAMMAL Zahreddine
