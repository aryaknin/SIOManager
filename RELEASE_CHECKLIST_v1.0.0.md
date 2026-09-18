# Checklist de publication — SIOManager v1.0.0

Cette checklist prépare la publication de la première version fonctionnelle de SIOManager.

## 1. Vérifier le contenu du dépôt

- [ ] Relire les modifications de code et de documentation.
- [ ] Décider si la modification locale de `demo-content/sisr/reseaux/modele-osi/cours.md` doit faire partie de la release.
- [ ] Vérifier qu'aucun secret, mot de passe ou jeton d'API n'est présent.
- [ ] Vérifier que `target/` reste ignoré par Git.
- [ ] Vérifier que la version du `pom.xml` est `1.0.0`.
- [ ] Vérifier les liens et la date dans `RELEASE_NOTES_v1.0.0.md`.

## 2. Valider la version

### Linux

```bash
JAVA_HOME=/chemin/vers/jdk-25 \
PATH=/chemin/vers/jdk-25/bin:$PATH \
./mvnw clean package
```

### Windows PowerShell

```powershell
$env:JAVA_HOME = "C:\chemin\vers\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean package
```

- [x] Compilation avec Java 25.
- [x] Exécution des 15 tests automatisés.
- [x] Génération de `target/SIOManager-1.0.0.jar`.
- [x] Test manuel final sous Linux.
- [ ] Test manuel final sous Windows.

## 3. Créer le commit de release

Avant d'utiliser `git add -A`, vérifier la modification indépendante du cours réseau signalée dans la première section.

Message proposé :

```text
release: préparer SIOManager v1.0.0

- finaliser l'interface compacte et redimensionnable
- mémoriser la disposition et le zoom
- ajouter les notes de publication
- passer la version Maven à 1.0.0
- valider la compilation et les 15 tests automatisés
```

Commandes possibles après sélection des bons fichiers :

```bash
git status
git diff --check
git commit -m "release: préparer SIOManager v1.0.0"
```

## 4. Créer le tag

À effectuer uniquement après le commit de release :

```bash
git tag -a v1.0.0 -m "SIOManager v1.0.0"
git show v1.0.0
```

## 5. Publier sur GitHub

```bash
git push SIOManager main
git push SIOManager v1.0.0
```

Créer ensuite une release GitHub avec :

- **Tag :** `v1.0.0`
- **Titre :** `SIOManager v1.0.0 — Première version fonctionnelle`
- **Description :** contenu de `RELEASE_NOTES_v1.0.0.md`
- **Statut :** release stable, sauf si un test Windows reste bloquant.

## 6. Vérifications après publication

- [ ] Le tag `v1.0.0` est visible sur GitHub.
- [ ] La note de release s'affiche correctement.
- [ ] L'archive automatique du code source peut être téléchargée.
- [ ] Un nouveau clone du dépôt compile avec le JDK 25.
- [ ] Les commandes Linux et Windows de la documentation sont correctes.

## Distribution autonome

La v1.0.0 est distribuée depuis les sources. La génération `jlink` standard est actuellement bloquée par le module automatique `org.fxmisc.flowless`, dépendance transitive de RichTextFX. La création d'images ou d'installateurs autonomes devra utiliser une stratégie de packaging adaptée et être validée séparément sur Linux et Windows.
