# SIOManager

SIOManager est une application de bureau destinée à centraliser les cours et les ressources d'une classe de BTS SIO. L'objectif est de réunir dans une même interface la consultation des cours, l'édition légère de code et, à terme, l'exécution locale de programmes.

L'application vise les environnements Linux/Debian et Windows. Son interface s'inspire de l'organisation générale d'Obsidian et des IDE comme IntelliJ IDEA et Visual Studio Code : explorateur à gauche, documents au centre, console en bas et paramètres dans une fenêtre séparée.

> Le projet est actuellement au stade de prototype. Les ressources affichées sont des données locales de démonstration et ne proviennent pas encore d'une base de données.

## Fonctionnalités actuelles

- interface JavaFX sombre et redimensionnable ;
- explorateur de ressources récursif ;
- classement par enseignements communs, SISR et SLAM ;
- présence de la CEJM, des mathématiques, de l'anglais et de la culture générale ;
- dossiers de chapitres et sous-dossiers ;
- distinction entre fichiers Markdown, PDF et fichiers de code ;
- ouverture des fichiers dans des onglets ;
- prévention de l'ouverture en double d'une même ressource ;
- squelette d'éditeur Markdown avec modes `Édition` et `Aperçu` ;
- éditeur texte provisoire pour Java, HTML, SQL et Bash ;
- emplacement réservé au futur lecteur PDF ;
- panneau inférieur avec console, sortie et problèmes ;
- fenêtre de paramètres ;
- thème CSS inspiré des environnements de développement.

## Fonctionnalités prévues

- ouverture et enregistrement de vrais fichiers locaux ;
- coloration syntaxique avec RichTextFX ;
- rendu Markdown complet ;
- lecteur PDF avec navigation et zoom ;
- compilation et exécution locale du code ;
- console interactive ;
- API serveur avec Spring Boot ;
- authentification et gestion des droits ;
- stockage des cours et ressources ;
- synchronisation et consultation hors ligne ;
- import automatique d'arborescences de cours ;
- création d'installateurs Linux et Windows.

## Technologies

| Élément | Technologie |
| --- | --- |
| Langage | Java 25 |
| Interface | JavaFX 21.0.6 |
| Description des vues | FXML |
| Apparence | CSS JavaFX |
| Construction | Maven Wrapper |
| Tests | JUnit 5 |
| IDE conseillé | IntelliJ IDEA |

## Prérequis

- un JDK 25 complet ;
- Git pour récupérer et versionner le projet ;
- IntelliJ IDEA recommandé, mais non obligatoire.

Il n'est pas nécessaire d'installer Maven globalement : les scripts `mvnw` et `mvnw.cmd` sont inclus dans le dépôt.

## Ouvrir le projet dans IntelliJ IDEA

1. Ouvrir le dossier du projet ou directement le fichier `pom.xml`.
2. Sélectionner un JDK 25 dans `File > Project Structure > Project SDK`.
3. Ouvrir la fenêtre Maven et utiliser `Reload All Maven Projects`.
4. Ouvrir la classe `MainApplication`.
5. Cliquer sur le triangle vert situé à côté de la déclaration de la classe.

La classe de démarrage est :

```text
com.example.siomanager.MainApplication
```

## Lancer depuis un terminal

### Linux

Si `JAVA_HOME` désigne déjà un JDK 25 :

```bash
./mvnw javafx:run
```

Sinon, indiquer temporairement le chemin du JDK :

```bash
JAVA_HOME=/chemin/vers/jdk-25 \
PATH=/chemin/vers/jdk-25/bin:$PATH \
./mvnw javafx:run
```

### Windows PowerShell

```powershell
$env:JAVA_HOME = "C:\chemin\vers\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd javafx:run
```

L'application peut être arrêtée avec `Ctrl+C` dans le terminal ou avec le bouton d'arrêt d'IntelliJ.

## Compiler le projet

Sous Linux :

```bash
./mvnw clean package
```

Sous Windows :

```powershell
.\mvnw.cmd clean package
```

Le fichier JAR généré est placé dans le dossier `target/`. Ce dossier contient uniquement des résultats de compilation et ne doit pas être modifié manuellement.

## Architecture actuelle

```text
SIOManager/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── src/
    └── main/
        ├── java/com/example/siomanager/
        │   ├── Launcher.java
        │   ├── MainApplication.java
        │   ├── MainController.java
        │   ├── SettingsController.java
        │   ├── model/
        │   │   ├── ResourceNode.java
        │   │   └── ResourceType.java
        │   ├── repository/
        │   │   └── DemoResourceRepository.java
        │   └── view/
        │       ├── ResourceDocumentFactory.java
        │       └── ResourceTreeCell.java
        └── resources/com/example/siomanager/
            ├── main-view.fxml
            ├── settings-view.fxml
            └── styles/
                └── application.css
```

### Responsabilités des composants

- `MainApplication` démarre JavaFX et affiche la fenêtre principale.
- `MainController` coordonne l'explorateur, les onglets, la console et les paramètres.
- `ResourceNode` représente une ressource et ses éventuels enfants.
- `ResourceType` distingue sections, matières, dossiers, Markdown, PDF et code.
- `DemoResourceRepository` fournit temporairement une arborescence locale de démonstration.
- `ResourceTreeCell` personnalise l'affichage des éléments dans l'explorateur.
- `ResourceDocumentFactory` crée la vue correspondant au type de fichier ouvert.
- les fichiers FXML décrivent la disposition des fenêtres.
- `application.css` centralise le thème graphique.

## Arborescence de démonstration

```text
Ressources
├── Enseignements communs
│   ├── Culture générale et expression
│   ├── Mathématiques
│   ├── Anglais
│   └── CEJM
├── SISR
│   ├── Réseaux
│   ├── Systèmes
│   └── Cybersécurité
└── SLAM
    ├── Java
    ├── Web
    ├── Bases de données
    └── Cybersécurité
```

Chaque matière peut contenir autant de chapitres, sous-dossiers et fichiers que nécessaire. Cette structure récursive pourra ensuite être alimentée par une API sans modifier le fonctionnement général de l'interface.

## État de la compilation

La commande suivante est utilisée pour valider le prototype :

```bash
./mvnw clean package
```

La compilation est actuellement réussie avec Microsoft OpenJDK 25. Les avertissements relatifs aux accès natifs proviennent de l'utilisation de JavaFX 21 avec un JDK récent et ne bloquent pas l'exécution.

## Convention de commits

Le projet peut utiliser des messages inspirés de Conventional Commits :

```text
feat: ajouter une fonctionnalité
fix: corriger un comportement
docs: modifier la documentation
refactor: réorganiser le code sans changer son comportement
test: ajouter ou modifier des tests
chore: modifier la configuration ou les outils
```

Un commit doit idéalement rester centré sur un changement cohérent et indiquer dans son corps les principales modifications ainsi que la commande utilisée pour les vérifier.
