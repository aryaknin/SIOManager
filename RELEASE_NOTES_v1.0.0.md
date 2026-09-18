# SIOManager v1.0.0

**Date de publication : 18 septembre 2026**

SIOManager v1.0.0 est la première version fonctionnelle de l'espace de travail destiné aux étudiants de BTS SIO. Elle réunit dans une même application la consultation des cours, l'édition légère de documents et de code, la lecture de PDF ainsi que l'exécution locale de petits programmes.

Cette version constitue le socle de l'application. Les ressources sont encore stockées localement ; la connexion à une API et à une base de données arrivera dans une version ultérieure.

## Points forts de la version

### Espace de travail BTS SIO

- classement des ressources par enseignements communs, SISR et SLAM ;
- prise en charge de la CEJM, des mathématiques, de l'anglais et de la culture générale et expression ;
- arborescence récursive avec matières, chapitres, dossiers et sous-dossiers ;
- découverte automatique du contenu réel de `demo-content/` ;
- actualisation de l'explorateur sans redémarrage ;
- création de dossiers, cours Markdown et fichiers Java, HTML, SQL ou Bash depuis l'interface ;
- génération d'un modèle adapté pour chaque nouveau fichier ;
- validation multiplateforme des noms et protection contre les chemins sortant du dossier de ressources ;
- ouverture des ressources dans des onglets ;
- prévention de l'ouverture en double d'un même document ;
- prise en charge des fichiers Markdown, PDF et des principaux fichiers de code du prototype.

### Édition de code et de Markdown

- éditeur RichTextFX avec numéros de ligne ;
- coloration syntaxique pour Java, HTML, XML, FXML, SQL, Bash et Markdown ;
- lecture et enregistrement des fichiers texte en UTF-8 ;
- indication des modifications non enregistrées dans les onglets ;
- confirmation avant la fermeture d'un document modifié ;
- enregistrement avec `Ctrl+S` ;
- modes `Édition` et `Aperçu` pour les documents Markdown ;
- rendu CommonMark dans un aperçu HTML sombre ;
- désactivation de JavaScript et neutralisation du HTML et des URL dangereuses dans l'aperçu.

### Lecture de PDF

- affichage des PDF directement dans l'application ;
- rendu des pages en arrière-plan pour conserver une interface réactive ;
- navigation entre les pages ;
- zoom de 50 à 250 % ;
- génération automatique de PDF de démonstration pour les mathématiques, la CEJM et le réseau SISR.

### Compilation et exécution

- compilation et exécution des fichiers Java dans un processus séparé ;
- prise en charge des classes Java déclarées dans un package ;
- exécution des scripts Bash sous Linux ;
- enregistrement automatique du fichier avant son exécution ;
- affichage des commandes, erreurs de compilation et résultats dans le panneau `SORTIE` ;
- arrêt du processus en cours depuis l'interface ;
- raccourci `F6` pour lancer le fichier sélectionné.

### Interface et confort d'utilisation

- thème sombre inspiré d'Obsidian, de Visual Studio Code et des IDE JetBrains ;
- interface compacte adaptée aux écrans de portable ;
- explorateur à gauche, éditeur au centre et console en bas ;
- panneaux redimensionnables à la souris ;
- possibilité de masquer l'explorateur et le panneau inférieur ;
- zoom global de l'interface de 83 à 133 % ;
- fenêtre de paramètres séparée ;
- mémorisation de la taille de la fenêtre, du zoom et de la position des séparateurs ;
- restauration automatique de la disposition au lancement suivant ;
- commande de réinitialisation de la disposition.

## Raccourcis principaux

| Action | Raccourci |
| --- | --- |
| Enregistrer le document | `Ctrl+S` |
| Créer une ressource | `Ctrl+N` |
| Actualiser les ressources | `F5` |
| Exécuter le fichier sélectionné | `F6` |
| Afficher ou masquer l'explorateur | `Ctrl+B` |
| Afficher ou masquer le panneau inférieur | `Ctrl+J` |
| Réduire le zoom de l'interface | `Ctrl+-` |
| Augmenter le zoom de l'interface | `Ctrl++` |
| Revenir au zoom normal | `Ctrl+0` |

## Technologies

| Composant | Version ou technologie |
| --- | --- |
| Langage | Java 25 |
| Interface | JavaFX 26.0.2 et FXML |
| Éditeur | RichTextFX 0.11.7 |
| Markdown | commonmark-java 0.30.0 et JavaFX WebView |
| PDF | Apache PDFBox 3.0.8 |
| Construction | Maven Wrapper |
| Tests | JUnit 5 et Maven Surefire 3.6.0 |

## Prérequis

- Linux/Debian ou Windows ;
- un JDK 25 complet comprenant `java` et `javac` ;
- Git pour récupérer le projet ;
- IntelliJ IDEA est recommandé, mais reste facultatif.

Maven ne doit pas être installé séparément : le Maven Wrapper est inclus dans le projet.

## Installation et lancement

Cette première version est distribuée depuis les sources. Les installateurs Linux et Windows ne sont pas encore disponibles.

### Linux

```bash
git clone https://github.com/aryaknin/SIOManager.git
cd SIOManager
./mvnw clean javafx:run
```

Si le JDK 25 n'est pas sélectionné par défaut :

```bash
JAVA_HOME=/chemin/vers/jdk-25 \
PATH=/chemin/vers/jdk-25/bin:$PATH \
./mvnw clean javafx:run
```

### Windows PowerShell

```powershell
git clone https://github.com/aryaknin/SIOManager.git
Set-Location SIOManager
$env:JAVA_HOME = "C:\chemin\vers\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean javafx:run
```

## Compilation et tests

### Linux

```bash
./mvnw clean package
```

### Windows PowerShell

```powershell
.\mvnw.cmd clean package
```

La v1.0.0 est validée par **15 tests automatisés** couvrant notamment :

- le modèle récursif des ressources ;
- la découverte automatique de nouveaux dossiers et fichiers ;
- la création sécurisée de ressources et de modèles de départ ;
- la lecture et l'enregistrement UTF-8 ;
- la coloration syntaxique ;
- le rendu Markdown sécurisé ;
- la génération et la lecture des PDF de démonstration ;
- la compilation et l'exécution d'un véritable programme Java.

## Limites connues

- les ressources proviennent encore du dossier local `demo-content/` ;
- aucune API, base de données distante ou synchronisation n'est connectée ;
- l'authentification et la gestion des droits ne sont pas encore disponibles ;
- aucun installateur `.deb`, `.msi` ou `.exe` n'est fourni dans cette version ;
- le lancement Java est conçu pour de petits exemples locaux et ne gère pas encore les dépendances Maven propres à chaque programme ;
- l'exécution Bash nécessite Bash sous Linux ; elle demandera Bash ou WSL sous Windows ;
- les fichiers SQL ne disposent pas encore de connexion à une base de données ;
- les fichiers HTML sont éditables mais ne disposent pas encore d'un aperçu navigateur intégré ;
- la console affiche les sorties, mais n'est pas encore un terminal interactif ;
- le contenu distant et le mode hors ligne synchronisé seront ajoutés ultérieurement.

## Suite du projet

Les prochaines versions pourront introduire :

- la modification, le déplacement et la suppression de ressources depuis l'interface ;
- l'import d'une arborescence depuis une source distante ;
- une API Spring Boot et une base de données centralisée ;
- l'authentification et les rôles étudiant, enseignant et administrateur ;
- la synchronisation locale et la consultation hors ligne ;
- un terminal interactif mieux isolé ;
- des aperçus HTML et des connexions SQL configurables ;
- des paquets autonomes pour Linux et Windows.

## Remerciements

Merci aux étudiants et enseignants de BTS SIO qui testeront cette première version. Les retours sur l'organisation des matières, l'ergonomie de l'éditeur et les besoins propres aux parcours SISR et SLAM aideront à guider les prochaines évolutions.

---

**Version :** `1.0.0`  
**Statut :** première version fonctionnelle  
**Compatibilité visée :** Linux/Debian et Windows
