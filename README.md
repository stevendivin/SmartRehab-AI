<div align="right">

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue?style=for-the-badge)

</div>

# SmartRehab AI

Assistant de rééducation sur Android. La caméra du téléphone analyse le mouvement du patient, mesure l'angle de l'articulation suivie, compte les répétitions et juge leur amplitude, comme le ferait un kinésithérapeute qui observe une séance. L'application ne remplace pas un professionnel de santé.

L'analyse se fait entièrement sur le téléphone. Aucune vidéo n'est enregistrée et aucune donnée n'est envoyée sur internet.

## Sommaire

- [Cas d'usage](#cas-dusage)
- [Fonctionnalités](#fonctionnalités)
- [Exercices](#exercices)
- [Fonctionnement](#fonctionnement)
- [Architecture](#architecture)
- [Stack technique](#stack-technique)
- [Installation](#installation)
- [Utilisation](#utilisation)
- [Tests](#tests)
- [Confidentialité](#confidentialité)
- [Limites connues](#limites-connues)
- [Feuille de route](#feuille-de-route)
- [Avertissement](#avertissement)

<!--
Captures d'ecran : une fois les images ajoutees dans docs/screenshots/,
supprimer ce commentaire (les lignes "<!--" et "-->") pour afficher la section.

## Captures d'écran

| Accueil | Exercices | Consignes |
|---|---|---|
| ![Accueil](docs/screenshots/01_accueil.png) | ![Exercices](docs/screenshots/02_exercices.png) | ![Consignes](docs/screenshots/03_consignes.png) |

| Séance en cours | Résumé | Historique |
|---|---|---|
| ![Séance](docs/screenshots/04_seance.png) | ![Résumé](docs/screenshots/05_resume.png) | ![Historique](docs/screenshots/06_historique.png) |
-->

## Cas d'usage

Un patient opéré du genou doit faire 15 flexions par jour. Au lieu de suivre une simple vidéo, il ouvre l'application, pose son téléphone face à lui et fait ses répétitions. L'application détecte son corps, mesure l'angle du genou, compte les répétitions complètes, signale celles dont l'amplitude est insuffisante, enregistre la séance et produit un rapport PDF que le kinésithérapeute peut consulter.

## Fonctionnalités

**Analyse du mouvement**
- Estimation de pose en temps réel avec MediaPipe Pose (33 points du corps), squelette dessiné sur l'image
- Calcul de l'angle articulaire en pixels, pour ne pas déformer les angles sur une image en portrait
- Comptage des répétitions par machine à états, qui ignore les mouvements partiels
- Jugement de l'amplitude de chaque répétition, avec un message de correction en direct
- Contrôle de visibilité des 3 points suivis : hors champ, rien n'est compté et un message l'indique

**Suivi du patient**
- Choix du côté suivi, gauche ou droit, avec un côté par défaut enregistré dans le profil
- Profil : nom du patient et côté par défaut
- Résumé de fin de séance : durée, répétitions, correctes, à corriger, angle moyen atteint, score
- Historique filtrable par exercice, suppression d'une séance avec confirmation
- Graphique de progression du score, calculé sur l'exercice filtré
- Indicateur de progression sur l'accueil (taux de répétitions correctes sur les 10 dernières séances)

**Accompagnement et partage**
- Écran de consignes avant chaque exercice : placement, position de départ, mouvement, conseils d'éclairage
- Vidéo de démonstration optionnelle par exercice, lue en boucle
- Rapport PDF partageable : synthèse, détail par exercice, courbe du score, liste des séances

## Exercices

| Exercice | Points suivis | Position conseillée |
|---|---|---|
| Flexion du genou | hanche, genou, cheville | de face ou de profil, jambe entière visible |
| Élévation du bras | hanche, épaule, coude | de face |
| Squat | hanche, genou, cheville | de profil, corps entier visible |
| Élévation de jambe | épaule, hanche, genou | de profil, jambe tendue |

Seuils utilisés par la machine à états (en degrés) :

| Exercice | Position tendue | Position fléchie | Amplitude jugée correcte |
|---|---|---|---|
| Flexion du genou | au-dessus de 160 | en dessous de 110 | 100 ou moins |
| Squat | au-dessus de 170 | en dessous de 100 | 90 ou moins |
| Élévation de jambe | au-dessus de 160 | en dessous de 120 | 100 ou moins |
| Élévation du bras | angle inversé (180 moins l'angle mesuré), au-dessus de 150 | en dessous de 100 | 90 ou moins |

Pour le squat, une répétition n'est prise en compte que si la hanche descend réellement, afin qu'un simple lever de jambe ne soit pas compté. Pour le bras, l'angle est inversé car le mouvement part d'un angle proche de zéro (bras le long du corps) et augmente quand le bras monte.

Ces seuils sont des valeurs raisonnables choisies pour ce projet, pas des normes cliniques validées. Ils sont définis dans `camera/CameraScreen.kt` et s'ajustent en une ligne.

## Fonctionnement

```text
Caméra (CameraX)
      |
Image redressée selon la rotation de l'appareil
      |
MediaPipe Pose : 33 points du corps
      |
Contrôle de visibilité des 3 points suivis
      |
Angle articulaire (produit scalaire, en pixels)
      |
Machine à états : tendu, descente, fléchi, remontée, tendu
      |
Répétition validée + jugement d'amplitude
      |
Sauvegarde locale (Room), puis historique, graphique, rapport PDF
```

Machine à états de comptage :

```text
EXTENDED -> DESCENDING -> FLEXED -> ASCENDING -> EXTENDED  (+1 répétition)
```

Une répétition n'est validée qu'au retour complet en position tendue, après être passé par la position fléchie. Un mouvement qui ne descend pas assez fait revenir l'état à `EXTENDED` sans rien compter.

Données enregistrées pour chaque séance (table `sessions`) : exercice et côté, date, durée, nombre total de répétitions, répétitions correctes, répétitions à corriger, angle moyen atteint. Le score est le rapport entre répétitions correctes et répétitions totales.

## Architecture

```text
com.steven.smartrehab
  analysis/        AngleCalculator, RepCounter
  camera/          CameraScreen (CameraX + MediaPipe + comptage en direct)
  data/local/      Room (SessionEntity, SessionDao, AppDatabase), UserPreferences
  pose/            PoseDetectorHelper, PoseOverlay
  presentation/
    home/          écran d'accueil
    exercise/      liste des exercices, écran de consignes
    session/       résumé de séance
    history/       historique, graphique de progression
    profile/       profil du patient
  ui/theme/        thème Material 3 (clair et sombre)
  utils/           ReportGenerator (rapport PDF)
```

La navigation entre écrans est gérée dans `MainActivity` par un état simple (`AppScreen`).

## Stack technique

| Domaine | Technologie |
|---|---|
| Langage et interface | Kotlin 2.2.10, Jetpack Compose, Material 3 |
| Caméra | CameraX |
| Vision par ordinateur | MediaPipe Tasks Vision (Pose Landmarker lite) |
| Persistance | Room 2.7.2 avec KSP, SharedPreferences |
| Rapport | PdfDocument (Android), FileProvider |
| Tests | JUnit 4 |
| Build | Android Gradle Plugin 8.10.0, compileSdk 36, minSdk 26, JVM 17 |

## Installation

**Option 1 : installer l'APK**
1. Télécharger `app-debug.apk` dans la page [Releases](../../releases).
2. L'ouvrir sur un téléphone Android et autoriser l'installation d'applications inconnues.
3. Accepter l'accès à la caméra au premier lancement.

**Option 2 : compiler le projet**
1. Cloner le dépôt et l'ouvrir dans Android Studio.
2. Vérifier que `pose_landmarker_lite.task` est dans `app/src/main/assets/`. Ce modèle MediaPipe est publié par Google.
3. Brancher un téléphone avec le débogage USB activé et lancer l'application.

**Vidéos de démonstration (optionnel)**
Placer des vidéos courtes, sans son, dans `app/src/main/res/raw/` avec ces noms exacts : `guide_knee_flexion.mp4`, `guide_arm_raise.mp4`, `guide_squat.mp4`, `guide_leg_raise.mp4`. Sans vidéo, l'écran de consignes affiche uniquement le texte.

## Utilisation

1. Renseigner son nom et son côté à rééduquer dans **Mon profil**.
2. **Commencer une séance**, choisir un exercice et le côté à suivre.
3. Lire les consignes de placement, poser le téléphone sur un support stable à environ 2 mètres, puis **Commencer**.
4. Faire les répétitions. L'angle, le compteur et le retour d'amplitude s'affichent en direct.
5. **STOP** pour terminer : la séance est enregistrée si elle contient au moins une répétition.
6. Dans **Historique**, filtrer par exercice, consulter la progression, supprimer une séance ou générer le **Rapport PDF** à envoyer au kinésithérapeute.

Conseils pour de meilleurs résultats : éclairage de face, vêtements ajustés, une seule personne dans le champ, téléphone immobile.

## Tests

Les tests unitaires couvrent le calcul d'angle (angle droit, jambe tendue, 45 degrés, symétrie, points confondus) et la machine à états de comptage (répétition complète, mouvement partiel, amplitude insuffisante, oscillations, angle moyen, réinitialisation, angle inversé).

Lancement : clic droit sur `app/src/test/java` dans Android Studio, puis **Run**, ou `./gradlew testDebugUnitTest`.

## Confidentialité

- Le flux caméra est analysé en direct puis jeté : aucune image ni vidéo n'est enregistrée.
- Les séances et le profil restent dans le stockage privé de l'application sur le téléphone.
- L'application ne demande que l'autorisation d'accès à la caméra et n'utilise pas internet.
- Le rapport PDF n'est partagé que sur action explicite de l'utilisateur, via la fenêtre de partage d'Android.

## Limites connues

- Mesure en deux dimensions avec une seule caméra : la précision dépend du placement, de la lumière et de vêtements ajustés.
- Les seuils d'amplitude sont indicatifs. Un professionnel de santé devrait pouvoir les ajuster selon le patient.
- La détection du squat repose sur le déplacement de la hanche et peut demander un réglage selon la distance à la caméra.
- Des répétitions très rapides peuvent être manquées (modèle léger exécuté sur le processeur).
- Les données restent sur le téléphone : pas de synchronisation entre appareils ni de sauvegarde.
- Une même vidéo de consigne sert pour le côté gauche et le côté droit.

## Feuille de route

- [x] Version 1.0 : application mobile complète (analyse, comptage, historique, rapport PDF)
- [ ] Version 2 : backend Spring Boot et PostgreSQL, authentification, synchronisation des séances
- [ ] Version 2 : comptes patient et kinésithérapeute, tableau de bord du praticien
- [ ] Version 2 : modèle TensorFlow Lite pour évaluer la qualité du mouvement (angles, vitesse, stabilité)
- [ ] Autres exercices et réglage des seuils par le praticien
- [ ] Mesure de la vitesse et de la stabilité du mouvement

## Avertissement

Outil de suivi personnel. Ce n'est pas un dispositif médical et il ne remplace pas l'avis d'un professionnel de santé.

## Auteur

Steven Divin Iteriteka, GitHub : [stevendivin](https://github.com/stevendivin)