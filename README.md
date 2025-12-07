# 🏴‍☠️ SMA - Chasse au Trésor Multi-Agents

Simulation d'un système multi-agents où des explorateurs collaborent pour collecter des trésors sur une carte, tout en évitant les dangers.

## 📋 Description

Le projet simule une **chasse au trésor** avec 3 types d'agents qui explorent une carte divisée en 9 zones (3x3), chaque zone contenant 100 cases (10x10).

## 🤖 Types d'Agents


| Agent | Comportement | Particularité |
|-------|--------------|---------------|
| **Réactif** | Exploration aléatoire | Simple, peut mourir et respawn au QG |
| **Cognitif** | Suit le plus court chemin (Dijkstra) | Reçoit les messages, secourt les agents blessés |
| **Communicant** | Scanne les zones à distance | Envoie les positions des trésors/animaux aux cognitifs |

## 🗺️ Éléments de la Carte

- **Trésors** 🟡 : À collecter par les agents
- **Animaux** 🔴 : Infligent des dégâts aux agents
- **Obstacles** ⬛ : Cases infranchissables
- **QG** 🟢 : Point de respawn (Zone 0, Case 0,0)

## 🔄 Fonctionnement

1. Les **Communicants** scannent leur zone et envoient des messages aux Cognitifs
2. Les **Cognitifs** reçoivent les infos, calculent le chemin optimal (Dijkstra) et collectent les trésors
3. Les **Réactifs** explorent aléatoirement
4. Si un agent meurt → respawn au QG après un délai (peut être secouru par un Cognitif)
5. Simulation terminée quand tous les trésors sont collectés

## 🛠️ Lancer le projet

```bash
cd sma-explorateurs
mvn compile exec:java
```

## 📁 Structure

```
sma-explorateurs/
├── agents/          # AgentReactif, AgentCognitif, AgentCommunicant
├── environnement/   # Carte, Zone, Case
├── objets/          # Tresor, Animal, Obstacle
├── concurrent/      # AgentManager (threads)
├── gui/             # Interface graphique (Dashboard, MainGui)
└── simulation/      # Logique de simulation
```

## ⚙️ Paramètres (SimuPara.java)

| Paramètre | Valeur par défaut |
|-----------|-------------------|
| Agents Réactifs | 5 |
| Agents Cognitifs | 4 |
| Agents Communicants | 3 |
| Trésors par zone | 1 |
| Animaux par zone | 2 |
| Obstacles par zone | 5 |

## 📊 Algorithmes

- **Dijkstra** : Calcul du plus court chemin pour les agents cognitifs
- **Distance de Manhattan** : Estimation des distances entre cases

## 👥 Auteurs
MOKHTARI Rayan / TAGHELIT Wassim / HAMMAL Zahreddine
