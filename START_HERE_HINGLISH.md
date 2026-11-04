# YESMIN AI — pehle yeh padhein

Yeh actual native Android Kotlin/Compose source project hai, website ya sirf UI demo nahi.

## Sabse important

- Gemini ke liye aapka Firebase project chahiye. `FIREBASE_SETUP.md` follow karke `app/google-services.json` add karein aur App Check debug token register karein. Koi secret Gemini API key source/APK mein nahi daalni.
- Cloud consent app ke Privacy screen se khud on karein. Bina Firebase config ke local app launcher, calculator, notes aur dusre local tools chal sakte hain; AI chat connect nahi hogi.
- Standard Google Linux SDK ka AAPT2 x86-64/glibc binary hai. ARM Termux par seedha chalne ka fake promise nahi kiya gaya. Reliable route: Termux se GitHub Actions build trigger → APK download → phone par install.
- `TERMUX_BUILD.md` mein har exact command, repo setup, Firebase config secret, build/download/install aur troubleshooting diya hai.

## Shuruaat

1. Termux mein `pkg update` aur `pkg install git gh unzip`.
2. `termux-setup-storage` ke baad ZIP ko `$HOME/projects` mein extract karein.
3. Project ke andar `TERMUX_BUILD.md` ke GitHub/Firebase steps complete karein.
4. `bash scripts/termux-cloud-build.sh` chalayein.
5. Script ke printed `termux-open .../app-debug.apk` command se APK installer kholein.

Full build ko Android SDK 35, build-tools 35.0.0, full JDK 17 aur Gradle 8.11.1 chahiye; included cloud workflow inhe set karta hai. Cloud workflow ke liye phone par JDK/SDK install karna zaroori nahi.

## Test karne se pehle

Mic sirf tap se shuru hota hai; speech provider audio ko network par process kar sakta hai. Voice engines phone par installed hone chahiye. Female TTS voice ki availability engine par depend karti hai; Settings mein voice preview/selection hai.

Screen gestures optional hain. Privacy disclosure padhein, Android Accessibility service manually enable karein, phir har gesture confirm karein. Gemini gestures execute nahi kar sakta. Payments/passwords/messages/delete controls par gesture use na karein. Stop/disable controls available hain.

Chat history saving default OFF hai. Preferences aap khud approve karte hain. Notes/reminders local hain; reminders inexact hain, critical alarm nahi. Core navigation English/Hindi/Bangla hai; kuch technical Settings/Privacy text English hai.

`BUILD_VERIFICATION.md` actual build/test results batata hai. Real-phone voice/gesture checks aur live Firebase test ko bina kiye tested nahi bola gaya. Inke liye `TESTING.md` use karein.
