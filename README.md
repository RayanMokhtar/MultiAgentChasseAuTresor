# 🗺️ Système Multi-Agents - Chasse au Trésor

## CY Cergy Paris Université - Master IISC 2 Pro
### Mini Projet : Équipe Hybride d'Explorateurs

---

## 📋 Description du Projet

Ce projet implémente un **système multi-agents** simulant une équipe d'explorateurs hybrides dans un environnement contenant des trésors, des obstacles et des animaux sauvages. Le projet met en œuvre des concepts avancés de programmation concurrente et de systèmes multi-agents.

## 🏗️ Architecture du Projet

```
sma-explorateurs/
├── pom.xml                          # Configuration Maven
└── src/
    └── main/
        └── java/
            └── sma/
                ├── App.java                 # Point d'entrée
                ├── agents/                  # Package des agents
                │   ├── Agent.java           # Classe abstraite (Thread)
                │   ├── AgentCognitif.java   # Agent avec planification
                │   ├── AgentReactif.java    # Agent à règles
                │   └── AgentCommunicant.java # Agent stationnaire
                ├── environnement/           # Package environnement
                │   ├── Carte.java           # Carte de l'environnement
                │   └── Zone.java            # Zone de la carte
                ├── objets/                  # Package objets
                │   ├── ObjetEnvironnement.java # Classe abstraite
                │   ├── Position.java        # Coordonnées (x, y)
                │   ├── Tresor.java          # Trésor à collecter
                │   ├── Animal.java          # Animal hostile
                │   └── Obstacle.java        # Obstacle bloquant
                ├── simulation/              # Package simulation
                │   ├── Simulation.java      # Moteur de simulation
                │   └── Statistiques.java    # Statistiques globales
                └── gui/                     # Package interface graphique
                    ├── MainFrame.java       # Fenêtre principale
                    ├── CartePanel.java      # Affichage carte
                    ├── StatsPanel.java      # Affichage statistiques
                    ├── ControlPanel.java    # Contrôles simulation
                    └── ConfigDialog.java    # Configuration
```

## 🎯 Types d'Agents

### 1. 🧠 Agent Cognitif
- **Caractéristiques** : Planification, mémoire des zones explorées
- **Missions** : Chercher trésors, secourir agents, explorer
- **Comportement** : Exécute un plan étape par étape
- **Spécial** : Peut téléporter les agents blessés au QG

### 2. ⚡ Agent Réactif
- **Caractéristiques** : Réaction immédiate selon règles
- **Règles disponibles** :
  - `FUIR_SI_DANGER` - Fuit les animaux dangereux
  - `ATTAQUER_SI_FORT` - Attaque si avantage
  - `COLLECTER_TRESOR_VISIBLE` - Collecte trésors proches
  - `EXPLORER_ALEATOIRE` - Exploration aléatoire
  - `RETOUR_SI_FAIBLE` - Retour au QG si blessé

### 3. 📡 Agent Communicant
- **Caractéristiques** : Stationnaire dans une zone
- **Rôle** : Émet des signaux d'information
- **Nombre** : 1 agent pour 2 zones (automatique)
- **Informations transmises** : Trésors, animaux, niveau de danger

## 🌍 Environnement

### Objets de l'environnement
- **💰 Trésors** : Valeur variable (50-200 points)
- **🐾 Animaux** : Loup, Ours, Serpent, Tigre (forces variables)
- **🪨 Obstacles** : Rocher, Arbre, Rivière, Mur (certains franchissables)

### Zones
- **🏠 QG (Quartier Général)** : Zone centrale sécurisée
- Zones explorables en grille configurable (5x5 par défaut)
- Niveau de danger calculé dynamiquement

## 🔧 Fonctionnalités Techniques

### Multithreading
- Chaque agent s'exécute dans son propre **thread**
- **Synchronisation** des accès aux ressources partagées
- Utilisation de `CopyOnWriteArrayList`, `ConcurrentHashMap`
- Variables `AtomicInteger`, `AtomicBoolean` pour les compteurs
- `ExecutorService` pour la gestion des pools de threads
- Verrous `ReentrantReadWriteLock` pour la carte

### Interface Graphique (Swing)
- Affichage temps réel de la carte (30 FPS)
- Panel de statistiques dynamique
- Contrôles : Démarrer, Pause, Arrêter, Reset
- Légende visuelle des éléments
- Barres de vie pour agents et animaux

## 🚀 Compilation et Exécution

### Prérequis
- Java 17 ou supérieur
- Maven 3.6+

### Compilation
```bash
cd sma-explorateurs
mvn clean compile
```

### Exécution
```bash
mvn exec:java
```

### Création du JAR exécutable
```bash
mvn clean package
java -jar target/sma-explorateurs-1.0-SNAPSHOT.jar
```

## 📊 Statistiques Collectées

- ⏱️ Durée de simulation
- 🔄 Nombre d'itérations
- ⚔️ Nombre de combats
- 💀 Nombre de morts
- 💰 Trésors collectés
- 🏆 Score total
- 🗺️ Zones explorées

## 🎮 Gameplay et Contraintes

1. **Énergie** : Les agents consomment de l'énergie en se déplaçant
2. **Points de vie** : Les combats réduisent les PV
3. **Mort** : Un agent vaincu réapparaît au QG après un délai
4. **Téléportation** : Les agents cognitifs peuvent évacuer les blessés vers le QG
5. **Victoire** : Tous les trésors collectés = fin de simulation

## 📝 Conception des Notions

### Relations Agents-Objets

| Agent | Trésor | Animal | Obstacle |
|-------|--------|--------|----------|
| Cognitif | Collecte planifiée | Combat stratégique | Contourne |
| Réactif | Collecte si proche | Fuit ou combat | Évite |
| Communicant | Signale position | Signale danger | - |

### Cycle de Vie d'un Agent
1. **Spawn** au QG
2. **Exploration** de l'environnement
3. **Interaction** avec objets/animaux
4. **Retour** au QG si blessé/épuisé
5. **Repos** et récupération
6. Répéter jusqu'à fin de simulation

### Diagramme des Classes Simplifié

```
                    ┌─────────────────┐
                    │   Simulation    │
                    │  (ExecutorService)│
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│    Carte      │    │   Agent       │    │ Statistiques  │
│  (RW Lock)    │    │ (Runnable)    │    │   (Atomic)    │
└───────┬───────┘    └───────┬───────┘    └───────────────┘
        │                    │
        ▼                    ├──────────────┬──────────────┐
┌───────────────┐            │              │              │
│    Zone       │            ▼              ▼              ▼
│(CopyOnWrite)  │    ┌─────────────┐ ┌────────────┐ ┌─────────────┐
└───────┬───────┘    │  Cognitif   │ │  Réactif   │ │Communicant  │
        │            │(Planification)│ │ (Règles)  │ │ (Signaux)  │
        ▼            └─────────────┘ └────────────┘ └─────────────┘
┌───────────────┐
│ ObjetEnv      │
│ (Abstract)    │
└───────┬───────┘
        │
        ├──────────────┬──────────────┐
        ▼              ▼              ▼
┌───────────┐  ┌───────────┐  ┌───────────┐
│  Tresor   │  │  Animal   │  │ Obstacle  │
│(Collecte  │  │(Combat    │  │(Blocage   │
│ atomique) │  │ sync)     │  │ check)    │
└───────────┘  └───────────┘  └───────────┘
```

---

## 👥 Équipe de Projet

- **NOM 1** : [Rôle]
- **NOM 2** : [Rôle]  
- **NOM 3** : [Rôle]

---

*CY Cergy Paris Université - Master IISC 2 Pro - Décembre 2025*
