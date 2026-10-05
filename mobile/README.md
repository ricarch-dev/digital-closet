# Digital Closet mobile prototype

A Spanish-language Expo app for browsing a sample wardrobe, adding a garment from a photo, building an outfit idea, swapping a piece, and saving or marking a combination as worn.

## Run

From this directory:

```sh
npm install
npx expo start
```

Open the development server's QR code in Expo Go, or press `w` to open the web preview. To start the web preview directly:

```sh
npx expo start --web
```

## Prototype limits

- Sample garments, newly selected photos, saved ideas, and feedback exist only in memory for the current app session. Reloading resets them.
- Outfit suggestions are deterministic and use sample garment tags. There is no backend, account, cloud sync, or real personalization.
- Sample garment photos load from Unsplash. If a photo is unavailable, the app shows a local color-based fallback.
- A selected personal photo stays on the device for this session and is not uploaded.
