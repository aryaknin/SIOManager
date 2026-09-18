# SIOManager

SIOManager est une application de bureau destinée à centraliser les cours et les ressources d'une classe de BTS SIO. L'objectif est de réunir dans une même interface la consultation des cours, l'édition légère de code et l'exécution locale de petits programmes.

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
- lecture et modification de vrais fichiers texte locaux en UTF-8 ;
- enregistrement depuis le menu, la barre d'outils ou avec `Ctrl+S` ;
- indication `*` sur les onglets contenant des changements non enregistrés ;
- confirmation avant la fermeture d'un document modifié ;
- prévention de l'ouverture en double d'une même ressource ;
- éditeur Markdown avec numéros de ligne, coloration et modes `Édition` / `Aperçu` ;
- rendu CommonMark dans un aperçu HTML sombre, avec JavaScript désactivé ;
- éditeur de code RichTextFX avec numéros de ligne ;
- coloration syntaxique pour Java, HTML/XML/FXML, SQL et Bash ;
- lecteur PDF avec rendu en arrière-plan, pagination et zoom de 50 à 250 % ;
- trois PDF de démonstration générés automatiquement s'ils sont absents ;
- compilation puis exécution des fichiers Java dans un processus séparé ;
- exécution des scripts Bash sous Linux, avec message explicite sous Windows ;
- panneau `SORTIE` dédié aux programmes, raccourci `F6` et bouton d'arrêt ;
- panneau inférieur avec console, sortie et problèmes ;
- fenêtre de paramètres ;
- thème CSS inspiré des environnements de développement.

## Fonctionnalités prévues

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
| Interface | JavaFX 26.0.2 |
| Description des vues | FXML |
| Apparence | CSS JavaFX |
| Éditeur enrichi | RichTextFX 0.11.7 |
| Rendu Markdown | commonmark-java 0.30.0 + JavaFX WebView |
| Lecture PDF | Apache PDFBox 3.0.8 |
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

## Éditer et exécuter un fichier

1. Déplier une matière puis un chapitre dans l'explorateur.
2. Double-cliquer sur un fichier Markdown, Java, HTML, SQL, Bash ou PDF.
3. Enregistrer les modifications avec `Ctrl+S`.
4. Pour un fichier Java ou Bash, cliquer sur `▶ Exécuter` ou appuyer sur `F6`.
5. Consulter la compilation et le résultat dans l'onglet inférieur `SORTIE`.

Avant une exécution, SIOManager enregistre automatiquement le document modifié. Un fichier Java est compilé dans `target/siomanager-run/`, puis lancé avec le JDK qui exécute l'application. Les scripts Bash sont pris en charge sous Linux ; sous Windows, Bash ou WSL devra être configuré lors d'une étape ultérieure. Les fichiers SQL nécessiteront une connexion de base de données et HTML pourra ensuite être associé à un aperçu navigateur.

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

## Exécuter les tests

Sous Linux :

```bash
./mvnw test
```

Sous Windows :

```powershell
.\mvnw.cmd test
```

Les tests actuels vérifient la cohérence du modèle de ressources, la lecture et l'enregistrement de fichiers UTF-8, la coloration syntaxique, le rendu Markdown sécurisé, la génération de PDF lisibles ainsi que la compilation et l'exécution d'un vrai programme Java.

## Architecture actuelle

```text
SIOManager/
├── demo-content/                  ressources locales de démonstration
├── pom.xml
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/example/siomanager/
    │   │   ├── Launcher.java
    │   │   ├── MainApplication.java
    │   │   ├── MainController.java
    │   │   ├── SettingsController.java
    │   │   ├── model/
    │   │   │   ├── ResourceNode.java
    │   │   │   └── ResourceType.java
    │   │   ├── repository/
    │   │   │   └── DemoResourceRepository.java
    │   │   ├── service/
    │   │   │   ├── CodeExecutionService.java
    │   │   │   ├── DemoContentInitializer.java
    │   │   │   ├── LocalFileService.java
    │   │   │   ├── MarkdownRendererService.java
    │   │   │   └── SyntaxHighlighter.java
    │   │   └── view/
    │   │       ├── DocumentSession.java
    │   │       ├── PdfDocumentView.java
    │   │       ├── ResourceDocumentFactory.java
    │   │       └── ResourceTreeCell.java
    │   └── resources/com/example/siomanager/
    │       ├── main-view.fxml
    │       ├── settings-view.fxml
    │       └── styles/
    │           └── application.css
    └── test/java/com/example/siomanager/
        ├── model/ResourceNodeTest.java
        └── service/
            ├── CodeExecutionServiceTest.java
            ├── DemoContentInitializerTest.java
            ├── LocalFileServiceTest.java
            ├── MarkdownRendererServiceTest.java
            └── SyntaxHighlighterTest.java
```

### Responsabilités des composants

- `MainApplication` démarre JavaFX et affiche la fenêtre principale.
- `MainController` coordonne l'explorateur, les onglets, la console et les paramètres.
- `ResourceNode` représente une ressource et ses éventuels enfants.
- `ResourceType` distingue sections, matières, dossiers, Markdown, PDF et code.
- `DemoResourceRepository` fournit temporairement une arborescence locale de démonstration.
- `LocalFileService` lit et enregistre les fichiers texte en UTF-8.
- `CodeExecutionService` compile et exécute Java ou lance Bash sans bloquer l'interface.
- `SyntaxHighlighter` choisit les règles de coloration selon l'extension du fichier.
- `MarkdownRendererService` transforme le Markdown en HTML et neutralise le HTML et les URL dangereuses.
- `DemoContentInitializer` crée les PDF de démonstration manquants au premier lancement.
- `DocumentSession` conserve l'état ouvert ou modifié d'un document.
- `PdfDocumentView` charge, rend, pagine et zoome les documents PDF.
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

Les fichiers Markdown, les exemples de code et les PDF sont actuellement chargés depuis `demo-content/`. Ce dossier fait partie du prototype : enregistrer un document depuis l'application modifie réellement le fichier correspondant dans ce dossier.

## État de la compilation

La commande suivante est utilisée pour valider le prototype :

```bash
./mvnw clean package
```

La compilation et le lancement sont actuellement validés avec Microsoft OpenJDK 25. Maven active explicitement les accès natifs requis par JavaFX 26 et l'accès utilisé par PDFBox lors du lancement.

Les onze tests automatisés actuels passent avec JUnit 5 et Maven Surefire 3.6.0.

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
