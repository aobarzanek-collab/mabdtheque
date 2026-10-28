# Ma BDthèque 📚

Application Android (Kotlin + Jetpack Compose) pour cataloguer une collection de **BD et mangas** (1000+ titres) sans les saisir à la main.

## Fonctionnalités

- **Scan du code-barres (ISBN)** avec la caméra (ML Kit) → l'ISBN est rempli automatiquement
- **Photo de la couverture** (CameraX), stockée sur le téléphone
- **Base de données locale Room**, sauvegardée sur l'appareil — aucun compte requis
- Champs : **titre (série), tome, auteur (scénario), dessinateur, coloriste, année de parution, ISBN, dédicace (oui/non), notes, photo de couverture**
- **Recherche instantanée** (titre, auteur, dessinateur, coloriste, ISBN)
- **Export CSV** (séparateur `;`, compatible Excel) via le menu en haut à droite

## Comment compiler

1. Installer [Android Studio](https://developer.android.com/studio) (Koala ou plus récent)
2. Ouvrir le dossier du projet (`File > Open`)
3. Laisser Gradle synchroniser (télécharge les dépendances)
4. Brancher le téléphone en USB avec le **mode développeur** activé, puis `Run > Run 'app'`
   - Ou `Build > Build APK` pour générer un APK à installer manuellement

## Comment utiliser

1. Appuyer sur **+** pour ajouter un album
2. Appuyer sur **Scanner** pour lire le code-barres (l'ISBN est rempli automatiquement)
3. Appuyer sur **Photo de la couverture** pour photographier la couverture
4. Compléter ou corriger les champs (titre, tome, auteur, dessinateur, coloriste, année)
5. Cocher **Dédicacé** si l'album est signé
6. **Enregistrer**

Pour retrouver plus tard un album : utiliser la barre de recherche en haut de la liste, ou faire défiler la liste (triée par titre de série puis numéro de tome).

## Pour voir "où j'en suis"

La liste affiche le nombre total d'albums. Chaque série est triée par tome : en regardant les numéros de tomes d'une série, vous repérez immédiatement les tomes manquants (ex. `Tome 1, Tome 2, Tome 5` → il vous manque le 3 et le 4). Vous pouvez aussi exporter la liste en CSV pour la consulter sur un ordinateur.

## Permissions

- **Caméra** : scan du code-barres et photo de couverture
- **Internet** : recherche du titre via Open Library (facultatif, l'app fonctionne hors ligne)

## Structure du projet

```
app/src/main/java/com/example/mabdtheque/
├── data/           # Base de données Room (Album, AlbumDao, AppDatabase, AlbumRepository)
├── scanner/        # Écran de scan du code-barres (ML Kit + CameraX)
├── camera/         # Écran photo de couverture (CameraX) + stockage fichiers
├── lookup/         # Recherche par ISBN (Open Library)
├── export/         # Export CSV
├── ui/             # Liste des albums, écran d'ajout/édition, ViewModel
└── MainActivity.kt
```
