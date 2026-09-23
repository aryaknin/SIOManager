# SIOManager

> Dernière version publiée : **1.0.0** — [consulter les notes de publication](RELEASE_NOTES_v1.0.0.md)
>
> Version en développement : **1.1.0-SNAPSHOT**

SIOManager est une application de bureau destinée à centraliser les cours et les ressources d'une classe de BTS SIO. L'objectif est de réunir dans une même interface la consultation des cours, l'édition légère de code et l'exécution locale de petits programmes.

L'application vise les environnements Linux/Debian et Windows. Son interface s'inspire de l'organisation générale d'Obsidian et des IDE comme IntelliJ IDEA et Visual Studio Code : explorateur à gauche, documents au centre, console en bas et paramètres dans une fenêtre séparée.

> Le projet reste une application locale : comptes, droits, catalogue, favoris, progression, corbeille et journal d'activité sont conservés dans SQLite. En développement, les ressources communes proviennent de `demo-content/` ; l'application installée en copie une version initiale dans le dossier utilisateur. La synchronisation entre plusieurs ordinateurs nécessitera une API serveur distincte.

## Fonctionnalités actuelles

- interface JavaFX sombre et redimensionnable ;
- interface compacte par défaut, avec zoom global réglable de 83 à 133 % ;
- panneaux latéral et inférieur redimensionnables à la souris ;
- masquage rapide de l'explorateur et du panneau inférieur ;
- mémorisation du zoom, de la taille de fenêtre et de la position des séparateurs ;
- explorateur de ressources récursif ;
- détection automatique des dossiers et fichiers présents dans `demo-content/` ;
- actualisation de l'arborescence sans redémarrer l'application ;
- création depuis l'interface de dossiers, cours Markdown et fichiers Java, HTML, SQL ou Bash ;
- modèles de départ automatiques et validation des noms sous Linux et Windows ;
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
- base de données locale SQLite créée automatiquement ;
- catalogue SQLite des ressources avec chemin, matière, chapitre, type, taille, empreinte SHA-256 et contenu textuel indexable ;
- recherche globale dans les noms, chemins, métadonnées et le contenu des fichiers texte ;
- tableau de bord avec favoris, documents récents et nombre de ressources terminées ;
- favoris et suivi de progression propres à chaque compte ;
- création guidée du premier compte administrateur, sans mot de passe par défaut ;
- authentification par identifiant et mot de passe ;
- mots de passe hachés avec PBKDF2-HMAC-SHA256, sel aléatoire et 210 000 itérations ;
- rôles `Administrateur` et `Élève` avec contrôles d'accès dans l'application ;
- menu de compte pour changer son mot de passe, se déconnecter ou changer d'utilisateur ;
- espace d'administration pour rechercher, créer, modifier, activer, désactiver ou supprimer les comptes et réinitialiser leurs mots de passe ;
- changement obligatoire du mot de passe temporaire à la première connexion ;
- journal d'activité consultable par l'administrateur ;
- import de fichiers ou d'une arborescence complète, création et renommage avec validation des chemins ;
- corbeille locale : toute suppression de ressource est récupérable par un administrateur ;
- restauration ou suppression définitive depuis l'espace d'administration ;
- téléchargement d'une copie d'une ressource par tous les utilisateurs ;
- ressources communes en lecture seule pour les élèves ;
- espace personnel isolé par compte pour créer, modifier, importer et exécuter du code ;
- sauvegarde ZIP cohérente de la base, des ressources communes et des espaces personnels ;
- restauration d'une sauvegarde depuis les paramètres, réservée aux administrateurs ;
- scripts de création d'une application autonome ou d'un paquet Debian et d'un installateur Windows ;
- thème CSS inspiré des environnements de développement.

## Fonctionnalités prévues

- console interactive ;
- API serveur avec Spring Boot ;
- stockage des ressources et métadonnées côté serveur ;
- synchronisation et consultation hors ligne ;
- import d'arborescences depuis une API distante ;
- aperçu HTML intégré et connexion SQL configurable.

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
| Base locale | SQLite + sqlite-jdbc 3.53.4.0 |
| Sécurité des mots de passe | PBKDF2-HMAC-SHA256 |
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

## Premier démarrage et comptes

Au premier lancement, aucune combinaison identifiant/mot de passe n'est prédéfinie. SIOManager ouvre un assistant qui demande de créer le premier administrateur. Le mot de passe doit contenir au moins 10 caractères.

Une fois connecté, l'administrateur dispose du menu `Administration` pour :

- rechercher un compte ;
- créer un compte `Administrateur` ou `Élève` ;
- modifier son nom d'affichage et son rôle ;
- activer ou désactiver un compte ;
- supprimer un compte devenu inutile ;
- définir un nouveau mot de passe temporaire ;
- consulter le journal d'activité ;
- restaurer ou vider les éléments de la corbeille.

Un utilisateur créé par un administrateur doit remplacer son mot de passe temporaire lors de sa première connexion. Il ne peut pas accéder à l'application tant que cette étape n'est pas terminée.

Les données locales se trouvent dans le dossier personnel de l'utilisateur du système :

```text
.siomanager/
├── siomanager.db                 comptes, catalogue, favoris et journal
├── shared-resources/             ressources communes de l'application installée
├── trash/                        ressources supprimées récupérables
└── workspaces/
    └── <identifiant>/            espace personnel du compte
```

Sous Linux, le chemin est généralement `~/.siomanager/`. Sous Windows, il correspond à `C:\Users\<nom>\.siomanager\`.

| Action | Administrateur | Élève |
| --- | :---: | :---: |
| Consulter/télécharger une ressource commune | Oui | Oui |
| Modifier l'arborescence commune | Oui | Non |
| Importer/modifier/exécuter dans l'espace personnel | Oui | Oui |
| Gérer les comptes | Oui | Non |
| Restaurer la corbeille et gérer les sauvegardes | Oui | Non |

Les droits protègent les actions effectuées dans SIOManager. Ils ne remplacent pas les permissions du système d'exploitation : une personne ayant un accès direct aux fichiers de l'ordinateur peut encore ouvrir `demo-content/` en dehors de l'application. Pour une utilisation dans toute une classe sur plusieurs postes, l'étape suivante sera une API centrale avec authentification serveur.

## Éditer et exécuter un fichier

1. Déplier une matière puis un chapitre dans l'explorateur.
2. Double-cliquer sur un fichier Markdown, Java, HTML, SQL, Bash ou PDF.
3. Enregistrer les modifications avec `Ctrl+S`.
4. Pour un fichier Java ou Bash, cliquer sur `▶ Exécuter` ou appuyer sur `F6`.
5. Consulter la compilation et le résultat dans l'onglet inférieur `SORTIE`.

Avant une exécution, SIOManager enregistre automatiquement le document modifié. Un fichier Java est compilé dans `target/siomanager-run/`, puis lancé avec le JDK qui exécute l'application. Les scripts Bash sont pris en charge sous Linux ; sous Windows, Bash ou WSL devra être configuré lors d'une étape ultérieure. Les fichiers SQL nécessiteront une connexion de base de données et HTML pourra ensuite être associé à un aperçu navigateur.

Pour un compte élève, les cours et exemples communs s'ouvrent en lecture seule. Les fichiers créés sous `Mon espace` sont modifiables et exécutables. Un clic droit dans l'explorateur permet également de créer, renommer, supprimer ou télécharger une ressource selon les droits du compte.

## Rechercher et suivre ses révisions

Le champ de recherche situé au-dessus de l'explorateur interroge le catalogue local. Il prend en compte le titre, le chemin, la matière, le chapitre et le contenu des fichiers Markdown ou de code de moins de 1 Mio. L'index est reconstruit automatiquement lors de l'actualisation de l'arborescence.

Un clic droit sur une ressource permet de l'ajouter aux favoris ou de la marquer comme terminée. Ces informations sont propres au compte connecté. Le tableau de bord affiche les favoris, les ouvertures récentes et le nombre de ressources terminées ; un double-clic ouvre directement le document.

## Importer, supprimer et restaurer des ressources

L'administrateur peut importer un fichier ou un dossier complet dans les ressources communes. Tous les comptes peuvent faire de même dans leur espace personnel. Les sous-dossiers sont conservés et l'import refuse d'écraser silencieusement un fichier existant.

Une suppression depuis SIOManager déplace l'élément dans la corbeille au lieu de le détruire. L'onglet `Corbeille` de l'espace d'administration permet ensuite de restaurer la ressource à son emplacement d'origine ou de la supprimer définitivement.

## Sauvegarder et restaurer les données

Dans `Paramètres > Sauvegardes`, un administrateur peut créer une archive ZIP comprenant un instantané cohérent de SQLite, les ressources communes et les espaces personnels. La restauration demande une confirmation, remplace la base locale par celle de l'archive et recopie les fichiers inclus. L'application revient ensuite à l'écran de connexion.

Conserver les sauvegardes hors de `demo-content/` et de `.siomanager/`, par exemple sur un support externe. Ne restaurer que des archives créées par SIOManager et provenant d'une source fiable.

## Personnaliser la disposition

Les séparateurs entre l'explorateur, l'éditeur et le panneau inférieur se déplacent directement à la souris. La position choisie est restaurée au prochain lancement.

| Action | Raccourci |
| --- | --- |
| Afficher ou masquer l'explorateur | `Ctrl+B` |
| Afficher ou masquer le panneau inférieur | `Ctrl+J` |
| Réduire l'interface | `Ctrl+-` |
| Agrandir l'interface | `Ctrl++` |
| Revenir à la taille normale | `Ctrl+0` |
| Exécuter le fichier sélectionné | `F6` |
| Enregistrer | `Ctrl+S` |
| Créer une ressource | `Ctrl+N` |
| Actualiser les ressources | `F5` |

Le menu `Affichage` permet également de contrôler les panneaux, le zoom ou de réinitialiser entièrement la disposition. Sous macOS, la touche de raccourci principale est automatiquement `⌘` à la place de `Ctrl`.

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

## Créer une application distribuable

Les scripts de packaging exécutent les tests, construisent le JAR, rassemblent ses dépendances puis utilisent `jpackage`. `JAVA_HOME` doit pointer vers un JDK 25 complet contenant `javac`, `jlink` et `jpackage`.

Sous Linux, créer d'abord une image autonome facile à tester :

```bash
JAVA_HOME=/chemin/vers/jdk-25 ./scripts/package-linux.sh app-image
```

Pour fabriquer un paquet Debian :

```bash
JAVA_HOME=/chemin/vers/jdk-25 ./scripts/package-linux.sh deb
```

Sous Windows PowerShell, créer l'installateur `.exe` :

```powershell
$env:JAVA_HOME = "C:\chemin\vers\jdk-25"
.\scripts\package-windows.ps1
```

Le résultat est placé dans `target/installer/`. Un paquet natif doit être construit sur son système cible : le script Linux ne produit pas l'installateur Windows et inversement.

## Exécuter les tests

Sous Linux :

```bash
./mvnw test
```

Sous Windows :

```powershell
.\mvnw.cmd test
```

Les 28 tests actuels vérifient notamment la cohérence du modèle de ressources, la lecture et l'enregistrement UTF-8, la coloration, le rendu Markdown sécurisé, les liaisons FXML, l'authentification, les droits, le catalogue, l'import de dossiers, la corbeille, les sauvegardes et l'exécution d'un vrai programme Java.

## Architecture actuelle

```text
SIOManager/
├── demo-content/                  ressources locales de démonstration
├── pom.xml
├── mvnw
├── mvnw.cmd
├── scripts/                      packaging Linux et Windows
└── src/
    ├── main/
    │   ├── java/com/example/siomanager/
    │   │   ├── Launcher.java
    │   │   ├── MainApplication.java
    │   │   ├── MainController.java
    │   │   ├── AdminController.java
    │   │   ├── SettingsController.java
    │   │   ├── data/DatabaseManager.java
    │   │   ├── model/
    │   │   │   ├── AuditEntry.java
    │   │   │   ├── ResourceMetadata.java
    │   │   │   ├── ResourceNode.java
    │   │   │   ├── ResourceType.java
    │   │   │   ├── TrashEntry.java
    │   │   │   ├── UserAccount.java
    │   │   │   └── UserRole.java
    │   │   ├── repository/
    │   │   │   ├── LocalResourceRepository.java
    │   │   │   ├── PersonalWorkspaceRepository.java
    │   │   │   ├── ResourceCatalogRepository.java
    │   │   │   └── UserRepository.java
    │   │   ├── service/
    │   │   │   ├── AccountService.java
    │   │   │   ├── BackupService.java
    │   │   │   ├── CodeExecutionService.java
    │   │   │   ├── DemoContentInitializer.java
    │   │   │   ├── LocalFileService.java
    │   │   │   ├── MarkdownRendererService.java
    │   │   │   ├── ResourceCreationService.java
    │   │   │   ├── PasswordHasher.java
    │   │   │   ├── ResourceCatalogService.java
    │   │   │   ├── ResourceManagementService.java
    │   │   │   ├── SyntaxHighlighter.java
    │   │   │   └── TrashService.java
    │   │   └── view/
    │   │       ├── DocumentSession.java
    │   │       ├── AuthenticationDialog.java
    │   │       ├── PasswordChangeDialog.java
    │   │       ├── PdfDocumentView.java
    │   │       ├── ResourceCreationDialog.java
    │   │       ├── ResourceDocumentFactory.java
    │   │       └── ResourceTreeCell.java
    │   └── resources/com/example/siomanager/
    │       ├── admin-view.fxml
    │       ├── main-view.fxml
    │       ├── settings-view.fxml
    │       └── styles/
    │           └── application.css
    └── test/java/com/example/siomanager/
        ├── model/ResourceNodeTest.java
        ├── repository/LocalResourceRepositoryTest.java
        └── service/
            ├── CodeExecutionServiceTest.java
            ├── DemoContentInitializerTest.java
            ├── LocalFileServiceTest.java
            ├── MarkdownRendererServiceTest.java
            ├── ResourceCreationServiceTest.java
            └── SyntaxHighlighterTest.java
```

### Responsabilités des composants

- `MainApplication` démarre JavaFX et affiche la fenêtre principale.
- `DatabaseManager` crée le fichier SQLite et applique le schéma local.
- `AccountService`, `UserRepository` et `PasswordHasher` gèrent l'authentification, les rôles et les mots de passe sans exposer ceux-ci en clair.
- `AdminController` gère les comptes, le journal d'activité et la corbeille depuis une fenêtre réservée aux administrateurs.
- `MainController` coordonne le tableau de bord, la recherche, l'explorateur, les onglets, la console et les paramètres.
- `ResourceNode` représente une ressource et ses éventuels enfants.
- `ResourceType` distingue sections, matières, dossiers, Markdown, PDF et code.
- `LocalResourceRepository` analyse récursivement le dossier local et construit l'arborescence.
- `PersonalWorkspaceRepository` construit l'arborescence privée du compte connecté.
- `ResourceCatalogRepository` conserve les métadonnées, favoris, ouvertures récentes et progressions dans SQLite.
- `ResourceCatalogService` indexe les arborescences et fournit la recherche globale et le tableau de bord.
- `LocalFileService` lit et enregistre les fichiers texte en UTF-8.
- `ResourceCreationService` valide les noms et crée les dossiers ou fichiers avec un modèle adapté.
- `ResourceManagementService` importe et renomme les ressources en restant dans la racine autorisée.
- `TrashService` déplace, restaure ou purge les ressources supprimées.
- `BackupService` crée et restaure les archives de sauvegarde locales.
- `CodeExecutionService` compile et exécute Java ou lance Bash sans bloquer l'interface.
- `SyntaxHighlighter` choisit les règles de coloration selon l'extension du fichier.
- `MarkdownRendererService` transforme le Markdown en HTML et neutralise le HTML et les URL dangereuses.
- `DemoContentInitializer` crée les PDF de démonstration manquants au premier lancement.
- `DocumentSession` conserve l'état ouvert ou modifié d'un document.
- `PdfDocumentView` charge, rend, pagine et zoome les documents PDF.
- `ResourceCreationDialog` recueille le type et le nom d'une nouvelle ressource.
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

Chaque matière peut contenir autant de chapitres, sous-dossiers et fichiers que nécessaire. L'arborescence est reconstruite automatiquement depuis le contenu réel de `demo-content/` et pourra ensuite être alimentée par une API sans modifier le fonctionnement général de l'interface.

Les fichiers Markdown, les exemples de code et les PDF sont actuellement chargés depuis `demo-content/`. Ce dossier fait partie du prototype : enregistrer un document depuis l'application modifie réellement le fichier correspondant dans ce dossier.

## État de la compilation

La commande suivante est utilisée pour valider le prototype :

```bash
./mvnw clean package
```

La compilation et le lancement sont actuellement validés avec Microsoft OpenJDK 25. Maven active explicitement les accès natifs requis par JavaFX 26 et l'accès utilisé par PDFBox lors du lancement.

Les 28 tests automatisés actuels passent avec JUnit 5 et Maven Surefire 3.6.0. Ils couvrent notamment SQLite, l'authentification, les restrictions de rôle, l'isolation de l'espace personnel, le catalogue, l'import récursif, la corbeille et les sauvegardes.

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
