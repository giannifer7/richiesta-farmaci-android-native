# Richiesta Farmaci

App Android per inviare al medico la richiesta di rinnovo ricette, via **WhatsApp**, **SMS** o **Email**, nel formato esatto atteso dallo studio medico.

## Installazione rapida

Scansiona il QR code con il telefono Android:

![QR code installazione](qr-install.png)

Oppure scarica direttamente l'APK: [richiesta-farmaci-1.2.apk](https://github.com/giannifer7/richiesta-farmaci-android-native/releases/download/v1.2/richiesta-farmaci-1.2.apk)

> **Prima installazione:** Android chiederà di abilitare *Installa app sconosciute* per il browser — va fatto una volta sola.
> Dopo il download, se il browser non propone di aprire il file, aprire l'app **Download** e toccare `app-release.apk`.

## Funzionamento

<img src="screenshot-main.png" width="260" alt="Schermata principale" align="right" />

La schermata principale mostra la lista dei farmaci abituali con le checkbox già spuntate sui farmaci da richiedere di default. Si selezionano i farmaci desiderati e si preme **INVIA**: l'app apre WhatsApp, l'app SMS o il client email con il messaggio già compilato nel formato predefinito:

```
Farmaci: COGNOME Nome, 2 x Farmaco A, 1 x Farmaco B, ...
```

In fondo alla schermata viene mostrato un promemoria con la data dell'ultima richiesta inviata (es. *"Ultima richiesta 41 giorni fa (23 gennaio 2026)"*).

## Impostazioni

Nella schermata **Impostazioni** si configurano:

- **Nome** e **Cognome** del paziente
- **Codice Fiscale**
- **Numero di telefono** del medico
- **Email** del medico
- **Metodo di invio**: WhatsApp, SMS o Email
- **Modello messaggio**: testo personalizzabile con segnaposto (vedi sotto)
- **Lista farmaci**: nome, quantità (confezioni) e selezione di default
- **Import da testo**: incolla una lista farmaci (es. dalla risposta del medico) per aggiornare la lista in un colpo solo

### Modello messaggio

Il testo inviato è personalizzabile tramite segnaposto:

| Segnaposto | Valore |
|---|---|
| `{nome}` | Nome del paziente |
| `{cognome}` | Cognome (come inserito) |
| `{COGNOME}` | Cognome in maiuscolo |
| `{cod_fisc}` | Codice fiscale |
| `{lista("sep", {qta} x {farmaco})}` | Lista farmaci con separatore e formato personalizzati |

Il separatore supporta `\n` (a capo) e `\t` (tabulazione). Esempio con farmaci su righe separate:

```
{COGNOME} {nome} - {cod_fisc}
{lista("\n", {qta} x {farmaco})}
```

Il pulsante **Ripristina default** riporta al formato originale:

```
Farmaci: {COGNOME} {nome}, {lista(", ", {qta} x {farmaco})}
```

### Formato import

```
[x] 2 x Paracetamolo 1000 mg
[ ] 1 x Ramipril 5 mg
Lansoprazolo 30 mg
```

Oppure direttamente il formato del medico (riga completa):

```
Farmaci: COGNOME Nome, 2 x Farmaco A, 1 x Farmaco B
```

## Build

### Prerequisiti

- Android Studio oppure SDK con Gradle
- JDK 8+

### Configurazione locale

Copia `local.properties.example` in `local.properties` e compila:

```properties
sdk.dir=/path/to/Android/Sdk

KEYSTORE_PATH=/path/to/your.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

> `local.properties` è escluso da git — non viene mai committato.

### Comandi

```bash
./gradlew assembleDebug    # APK di debug
./gradlew assembleRelease  # APK firmato di release
```

## Requisiti Android

- **minSdk** 21 (Android 5.0)
- **targetSdk** 34 (Android 14)

## Privacy / GDPR

I dati personali inseriti (nome, cognome, codice fiscale) sono salvati **solo localmente** sul dispositivo tramite `SharedPreferences` e il file `medicines.txt`. Non vengono trasmessi a nessun server o servizio esterno.

L'unica trasmissione avviene quando l'utente preme **INVIA**: in quel momento l'app apre WhatsApp, l'app SMS o il client email con il messaggio precompilato, e l'utente lo invia esplicitamente al solo destinatario configurato (il medico).

## Icona

Bastone di Asclepio — [Rod_of_Asclepius_vector.svg](https://commons.wikimedia.org/wiki/File:Rod_of_Asclepius_vector.svg), Wikimedia Commons, CC0 pubblico dominio.

## Licenza

MIT
