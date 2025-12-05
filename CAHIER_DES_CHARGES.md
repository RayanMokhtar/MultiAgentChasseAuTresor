# 📋 Cahier des Charges - Simulation Multi-Agents Chasse au Trésor

## 1. Vue d'Ensemble

### 1.1 Description du Projet
Ce projet implémente un **Système Multi-Agents (SMA)** simulant une chasse au trésor. Les agents explorateurs doivent collecter des trésors dispersés sur une carte tout en évitant les dangers (animaux sauvages, obstacles). Le système utilise le **multithreading Java** avec une architecture thread-safe.

### 1.2 Technologies Utilisées
- **Langage**: Java 17
- **Build**: Maven 3.9.11
- **Architecture**: Multi-thread avec ExecutorService
- **Collections Thread-Safe**: ConcurrentHashMap, CopyOnWriteArrayList, AtomicInteger

---

## 2. Architecture de l'Environnement

### 2.1 La Carte (`Carte.java`)

| Paramètre | Valeur | Description |
|-----------|--------|-------------|
| Dimensions | 600 × 600 pixels | Taille totale de la carte |
| Grille de zones | 3 × 3 = 9 zones | Division en zones carrées |
| Taille d'une zone | 200 × 200 pixels | 10 × 10 cases par zone |
| Taille d'une case | 20 × 20 pixels | Unité de base |
| Position QG | Zone [0,0] | Coin supérieur gauche |

### 2.2 Les Zones (`Zone.java`)

Chaque zone possède :
- **ID unique** et coordonnées de grille (ligne, colonne)
- **Listes d'objets** : trésors, animaux, obstacles, fusils
- **Liste des agents présents** (thread-safe)
- **État d'exploration** : exploré ou non
- **Niveau de danger** : calculé selon les animaux présents
- **Couleur** : Vert clair (QG) ou alternance beige/marron (damier)

#### Zone QG (Quartier Général)
- Position fixe en [0,0]
- **Toujours explorée** par défaut
- **Zone sûre** : pas d'animaux
- Permet aux agents de :
  - Se reposer et récupérer des PV
  - Déposer les trésors collectés
  - Réapparaître après une mort

---

## 3. Les Objets de l'Environnement

### 3.1 Trésors (`Tresor.java`)

| Propriété | Valeur |
|-----------|--------|
| Nombre par défaut | 10 |
| État | Collecté ou non |
| Visuel | 💰 Or (jaune) |

**Règles :**
- Un trésor ne peut être collecté qu'une seule fois (thread-safe avec `synchronized`)
- La simulation se termine quand tous les trésors sont collectés
- Les trésors sont répartis dans toutes les zones (y compris le QG)

### 3.2 Animaux (`Animal.java`)

| Type | Emoji | Force | Vitesse | Couleur | PV Max |
|------|-------|-------|---------|---------|--------|
| LOUP | 🐺 | 15 | 4 | Gris | 30 |
| OURS | 🐻 | 25 | 3 | Marron | 50 |
| CROCODILE | 🐊 | 20 | 3 | Vert foncé | 40 |

**Constantes :**
- `RANGE_DETECTION` = 5 cases (50 pixels)
- Distance d'attaque = 15 pixels

**Règles de comportement :**
1. **Confinement** : Un animal ne peut **jamais sortir de sa zone assignée**
2. **Déplacement aléatoire** : Se déplace au hasard dans sa zone
3. **Détection d'agent** : Si un agent entre dans le range de détection, l'animal le poursuit
4. **Poursuite** : Si 2+ agents sont détectés, poursuit le **plus proche**
5. **Attaque** : Si à portée (≤ 15 pixels), inflige des dégâts
6. **Protection de trésor** : Peut rester autour d'un trésor
7. **Exclusion** : N'attaque pas les Sentinelles (AgentCommunicant)

### 3.3 Obstacles (`Obstacle.java`)

| Type | Emoji | Franchissable | Couleur |
|------|-------|---------------|---------|
| ROCHER | 🪨 | ❌ Non | Gris |
| MUR | 🧱 | ❌ Non | Rouge brique |
| ARBRE | 🌲 | ✅ Oui | Vert forêt |
| RIVIERE | 🌊 | ✅ Oui | Bleu |

**Règle :**
- Les obstacles non-franchissables bloquent le passage (distance < 10 pixels)
- Les obstacles franchissables permettent le passage mais avec un coût de traversée = 2

### 3.4 Fusils (`Fusil.java`)

| Propriété | Valeur |
|-----------|--------|
| Nombre par défaut | 1 |
| Munitions | 2 balles |
| Position | Centre de la carte (360, 300) |

**Règles :**
- Un agent ne peut posséder qu'un seul fusil
- Permet de **tuer instantanément** un animal (2 fois max)
- Le fusil devient inutile une fois vide

---

## 4. Les Agents

### 4.1 Classe de Base (`Agent.java`)

#### Attributs Communs

| Attribut | Type | Description |
|----------|------|-------------|
| id | int | Identifiant unique auto-généré |
| nom | String | Nom de l'agent |
| position | Position (volatile) | Position courante |
| pointsDeVie / pointsDeVieMax | int | Santé |
| force | int | Puissance d'attaque |
| enVie | AtomicBoolean | État vivant |
| actif | AtomicBoolean | Thread en cours |
| blesse | boolean | En attente de secours |
| fusil | Fusil | Arme équipée |

#### Constantes Communes

| Constante | Valeur | Description |
|-----------|--------|-------------|
| DELAI_ACTION | 100 ms | Temps entre chaque action |
| TEMPS_ATTENTE_BLESSURE | 10 000 ms | Temps avant respawn automatique |

#### Statistiques Tracées (AtomicInteger)
- `nombreCombats` : Combats engagés
- `nombreVictoires` : Combats gagnés
- `nombreMorts` : Fois mort/blessé
- `distanceParcourue` : Pixels parcourus
- `animauxTues` : Animaux éliminés

#### Système de Blessure
Quand un agent perd tous ses PV :
1. L'agent devient **blessé** (pas mort immédiatement)
2. Reste immobile à sa position
3. **Pendant 10 secondes** :
   - Si un autre agent passe dessus → `respawnSurPlace()` (PV max, reprend exploration)
   - Sinon → `reapparaitre()` au QG

---

### 4.2 Agent Réactif (`AgentReactif.java`)

> **Philosophie** : Réagit aux stimuli immédiats selon des règles prédéfinies. Pas de mémoire ni planification.

#### Caractéristiques

| Propriété | Valeur |
|-----------|--------|
| Couleur | 🟠 Orange (255, 100, 0) |
| Type | ⚡ Réactif |
| PV Max | 90 |
| Force | 28 |
| Vision | 3 cases (60 pixels) |

#### Règles de Comportement (par ordre de priorité)

| # | Règle | Description |
|---|-------|-------------|
| 1 | `RETOUR_SI_FAIBLE` | Si PV < 25%, retour au QG pour repos |
| 2 | `SECOURIR_AGENT` | Secourt un agent blessé à portée |
| 3 | `UTILISER_FUSIL` | Tire sur un animal si armé et à portée |
| 4 | `FUIR_SI_DANGER` | Fuit les animaux vers la position la plus éloignée |
| 5 | `RAMASSER_FUSIL` | Ramasse un fusil visible |
| 6 | `COLLECTER_TRESOR_VISIBLE` | Collecte un trésor proche |
| 7 | `EVITER_OBSTACLES` | Contourne les obstacles |
| 8 | `EXPLORER_ALEATOIRE` | Déplacement aléatoire en 8 directions |

#### Algorithme de Fuite
1. Calcule la direction opposée à l'animal
2. Essaye 5 positions de fuite :
   - Direction opposée directe
   - Direction horizontale seule
   - Direction verticale seule
   - Directions perpendiculaires (2 sens)
3. Vitesse de fuite = 12 pixels

---

### 4.3 Agent Cognitif (`AgentCognitif.java`)

> **Philosophie** : Architecture **BDI** (Beliefs-Desires-Intentions). Planifie ses actions, mémorise l'environnement.

#### Caractéristiques

| Propriété | Valeur |
|-----------|--------|
| Couleur | 🔵 Bleu (0, 100, 255) |
| Type | 🧠 Cognitif |
| PV Max | 120 |
| Force | 35 |
| Vision | 3 cases (60 pixels) |

#### Missions Disponibles

| Mission | Emoji | Description |
|---------|-------|-------------|
| CHERCHER_TRESOR | 🎯 | Se dirige vers un trésor connu |
| ACCOMPAGNER_AGENT | 🤝 | Accompagne un autre agent |
| EXPLORER_ZONE | 🔍 | Explore une zone inconnue |
| RETOUR_QG | 🏠 | Retourne au QG |
| SECOURIR_AGENT | 🚑 | Va secourir un agent blessé |
| REPOS | 😴 | Se repose au QG |
| FUIR_VERS_TRESOR | 🏃 | Fuit un animal (priorité max) |
| AUCUNE | ⏸️ | En attente d'une mission |

#### Architecture BDI

**Beliefs (Croyances)** :
- `zonesExplorees` : Set des zones visitées
- `connaissanceTresors` : Map Zone → Liste de trésors connus
- `beliefsDangers` : Map Position → Type de danger

**Desires (Désirs)** :
- Collecter tous les trésors
- Aider les agents en danger
- Explorer la carte

**Intentions (Plan)** :
- Queue de positions à atteindre
- Recalculée quand la mission change

#### Comportement Principal (méthode `agir()`)
1. Observer l'environnement (marquer zones explorées)
2. Vérifier si un agent a besoin d'aide (mission SECOURIR)
3. Gérer les dangers immédiats (fuite si animal proche)
4. Vérifier état de santé (retour QG si PV < 25%)
5. Choisir une nouvelle mission si nécessaire
6. Exécuter le plan (avancer vers l'objectif)

#### Système de Leurre (Coordination)
Quand un animal "garde" un trésor :
1. Un agent cognitif se désigne comme **leurre**
2. Il attire l'animal loin du trésor
3. Un autre agent peut alors collecter le trésor
4. `Simulation.assignerLeurre()` / `libererLeurre()` gère la coordination

---

### 4.4 Agent Communicant / Sentinelle (`AgentCommunicant.java`)

> **Philosophie** : Agent **stationnaire** qui observe et partage des informations avec les autres agents.

#### Caractéristiques

| Propriété | Valeur |
|-----------|--------|
| Couleur | 🟣 Violet (148, 0, 211) |
| Type | 📡 Communicant |
| PV Max | 60 |
| Force | 5 |
| Mobilité | ❌ Stationnaire |

#### Règles Strictes
- **NE BOUGE JAMAIS** (méthode `deplacer()` retourne toujours `false`)
- **NE PEUT PAS secourir** les agents
- **NE PEUT PAS téléporter** les agents
- Est assigné à une zone spécifique au démarrage
- Les animaux ne l'attaquent pas

#### Informations Partagées
Met à jour toutes les secondes :
- `zone_id` : Identifiant de la zone
- `tresors_disponibles` : Nombre de trésors non collectés
- `animaux_presents` : Nombre d'animaux actifs
- `obstacles` : Nombre d'obstacles
- `niveau_danger` : Somme des forces des animaux
- `exploree` : État d'exploration

> ⚠️ **Important** : Ne partage PAS les positions exactes des trésors (anti-clairvoyance)

#### Système de Messages

| Type | Emoji | Contenu |
|------|-------|---------|
| ALERTE_DANGER | ⚠️ | Danger détecté |
| TRESOR_TROUVE | 💰 | Trésor dans la zone |
| ZONE_SURE | ✅ | Zone sécurisée |
| AGENT_EN_DANGER | 🆘 | Agent a besoin d'aide |
| INFO_ZONE | 📍 | Infos générales |

---

## 5. Ressources Partagées (`RessourcesPartagees.java`)

### 5.1 Objectif
Permettre la **coordination inter-threads** entre agents indépendants pour éviter :
- Que plusieurs agents visent le même trésor
- Que plusieurs agents explorent la même zone
- Les conflits d'accès

### 5.2 Structures de Données Thread-Safe

| Structure | Type | Utilisation |
|-----------|------|-------------|
| `destinationsZones` | ConcurrentHashMap<AgentID, Zone> | Réservation de zones |
| `destinationsPositions` | ConcurrentHashMap<AgentID, Position> | Positions cibles |
| `tresorsReserves` | ConcurrentHashMap<TresorID, AgentID> | Réservation de trésors |
| `alertesDanger` | ConcurrentLinkedQueue<AlerteDanger> | File d'alertes |
| `demandesAide` | ConcurrentLinkedQueue<DemandeAide> | Demandes de secours |
| `zonesExplorees` | ConcurrentHashMap<ZoneID, Boolean> | Connaissance partagée |
| `infosAgents` | ConcurrentHashMap<AgentID, InfoAgent> | État de chaque agent |

### 5.3 API Principales

```java
// Zones
boolean reserverZone(int agentId, Zone zone)
boolean zoneReservee(int agentId, Zone zone)
void libererZone(int agentId)

// Trésors
boolean reserverTresor(int agentId, Tresor tresor)
boolean tresorReserve(int agentId, Tresor tresor)
void libererTresorParAgent(int agentId)

// Alertes (TTL = 5 secondes)
void emettreAlerte(Position pos, String type, int emetteur)
List<AlerteDanger> getAlertesRecentes()

// Demandes d'aide (TTL = 10 secondes)
void demanderAide(int agentId, Position pos, String raison)
DemandeAide getDemandeAideNonTraitee()
```

---

## 6. La Simulation (`Simulation.java`)

### 6.1 Configuration par Défaut

| Paramètre | Valeur |
|-----------|--------|
| Agents Cognitifs | 2 ("Einstein", "Newton") |
| Agents Réactifs | 3 ("Flash", "Bolt", "Storm") |
| Sentinelles | (nb_zones - 1) / 2 = 4 |
| Total Agents | 9 |

### 6.2 Architecture Multithreading

```
┌─────────────────────────────────────────────────────┐
│              ExecutorService (agents)               │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │Agent 1  │ │Agent 2  │ │Agent 3  │ │   ...   │   │
│  │(Thread) │ │(Thread) │ │(Thread) │ │(Thread) │   │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│           RessourcesPartagees (Thread-Safe)         │
│  ┌───────────┐ ┌────────────┐ ┌─────────────────┐  │
│  │Destinations│ │Réservations│ │Alertes/Demandes│  │
│  └───────────┘ └────────────┘ └─────────────────┘  │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│          ScheduledExecutorService (stats)           │
│  - Mise à jour stats (100ms)                        │
│  - Comportement animaux                             │
│  - Vérification fin simulation                      │
└─────────────────────────────────────────────────────┘
```

### 6.3 Cycle de Vie de la Simulation

1. **Initialisation** :
   - Création de la carte (zones, objets)
   - Création des agents (tous au QG)
   - Initialisation des ressources partagées

2. **Démarrage** (`demarrer()`) :
   - Création du pool de threads (1 par agent)
   - Démarrage du scheduler (100ms)
   - Chronométrage

3. **Exécution** :
   - Chaque agent exécute sa méthode `agir()` en boucle
   - Le scheduler fait agir les animaux et met à jour les stats

4. **Condition de Fin** :
   - Tous les trésors collectés (`getTousTresorsNonCollectes().isEmpty()`)

5. **Arrêt** (`arreter()`) :
   - Signal d'arrêt à tous les agents
   - Fermeture des ExecutorService
   - Affichage des statistiques finales

---

## 7. Statistiques (`Statistiques.java`)

| Métrique | Description |
|----------|-------------|
| ⏱️ Durée | Temps de simulation (MM:SS) |
| 🔄 Itérations | Nombre de cycles de 100ms |
| ⚔️ Combats | Affrontements agent-animal |
| 💀 Morts | Nombre de fois qu'un agent est tombé |
| 🤕 Blessures | Agents tombés en attente de secours |
| 🚑 Secours | Agents secourus par d'autres |
| 💰 Trésors | Trésors collectés |
| 🗺️ Zones explorées | Zones marquées comme explorées |
| 🐾 Animaux tués | Animaux éliminés (par fusil ou combat) |

---

## 8. Règles Récapitulatives

### 8.1 Règles de Déplacement
- ✅ Un agent ne peut se déplacer que sur des positions accessibles
- ✅ Position clampée aux bords de la carte (5px de marge)
- ✅ Les obstacles non-franchissables bloquent (distance < 10px)
- ❌ Un agent Communicant ne se déplace jamais

### 8.2 Règles de Combat
- ⚔️ Un animal attaque si un agent est à ≤ 15 pixels
- 🏃 La fuite est la priorité #1 pour tous les agents
- 🔫 Le fusil tue instantanément un animal (2 utilisations)
- 💔 Dégâts = force de l'attaquant + aléa (0-9)

### 8.3 Règles de Secours
- 🤕 Un agent "mort" devient blessé pendant 10 secondes
- 🚑 Un autre agent peut le secourir s'il passe à ≤ 20 pixels
- ✨ Le secours fait respawn sur place avec PV max
- 🏠 Sans secours, respawn automatique au QG

### 8.4 Règles des Animaux
- 🐾 Confinés à leur zone assignée
- 👁️ Détection à 5 cases (50 pixels)
- 🎯 Poursuivent l'agent le plus proche détecté
- 🛡️ N'attaquent pas les Sentinelles

### 8.5 Règles de Coordination (Agents Cognitifs)
- 📌 Réservation de zones pour éviter les doublons
- 🎯 Réservation de trésors pour éviter la compétition
- 🔓 Libération des réservations quand l'agent meurt
- 🤝 Système de leurre pour distraire les gardiens

---

## 9. Diagramme de Classes Simplifié

```
                    ┌───────────────────┐
                    │      Agent        │
                    │   (abstract)      │
                    └─────────┬─────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  AgentReactif   │ │  AgentCognitif  │ │AgentCommunicant │
│  (⚡ Orange)    │ │  (🧠 Bleu)      │ │  (📡 Violet)    │
│  PV=90, F=28    │ │  PV=120, F=35   │ │  PV=60, F=5     │
│  Règles simples │ │  Architecture   │ │  Stationnaire   │
│                 │ │  BDI + Plan     │ │  Observation    │
└─────────────────┘ └─────────────────┘ └─────────────────┘

┌───────────┐     ┌───────────┐     ┌───────────┐
│   Carte   │────▶│   Zone    │────▶│  Objets   │
│  600×600  │     │  200×200  │     │ (Tresor,  │
│  3×3 zones│     │           │     │  Animal,  │
└───────────┘     └───────────┘     │  Obstacle,│
                                    │  Fusil)   │
                                    └───────────┘

┌─────────────────────────────────────────────────────────┐
│                     Simulation                          │
│  - ExecutorService (agents)                             │
│  - ScheduledExecutorService (animaux, stats)            │
│  - RessourcesPartagees (coordination thread-safe)       │
└─────────────────────────────────────────────────────────┘
```

---

## 10. Points Techniques Importants

### 10.1 Thread-Safety
- `volatile` pour les variables partagées simples
- `AtomicInteger/Boolean` pour les compteurs et flags
- `synchronized` pour les blocs critiques
- `ConcurrentHashMap` pour les maps partagées
- `CopyOnWriteArrayList` pour les listes itérées souvent

### 10.2 Synchronisation
- `lockMouvement` (Object) : Verrou pour les déplacements
- `ReentrantReadWriteLock` dans Carte pour accès concurrent

### 10.3 Gestion des Threads
```java
// Pool dimensionné au nombre d'agents
ExecutorService executorAgents = Executors.newFixedThreadPool(agents.size());

// Scheduler pour les tâches périodiques (100ms)
ScheduledExecutorService schedulerStats = Executors.newSingleThreadScheduledExecutor();
```

---

## 11. Lancement de la Simulation

### 11.1 Compilation
```bash
cd sma-explorateurs
mvn compile
```

### 11.2 Exécution
```bash
mvn exec:java -Dexec.mainClass=sma.App
```

### 11.3 Contrôles GUI
- **Démarrer** : Lance la simulation
- **Pause/Reprendre** : Met en pause ou reprend
- **Arrêter** : Termine la simulation et affiche les stats

---

*Document généré le 5 décembre 2025*
