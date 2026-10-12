// Inicializa Firebase una sola vez y comparte la base de datos con el resto de archivos.
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getFirestore, connectFirestoreEmulator } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import { firebaseConfig, USAR_EMULADOR } from "./firebase-config.js";

export const modoEmulador = USAR_EMULADOR || new URLSearchParams(location.search).has("emulador");

const configuracion = modoEmulador
  ? { apiKey: "demo-key", authDomain: "demo-sabor-limeno.firebaseapp.com", projectId: "demo-sabor-limeno" }
  : firebaseConfig;

export const app = initializeApp(configuracion);
export const db = getFirestore(app);

if (modoEmulador) connectFirestoreEmulator(db, "127.0.0.1", 8080);
