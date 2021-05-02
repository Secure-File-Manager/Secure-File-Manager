# Frequently Asked Questions

## Content

<details><summary>Show</summary>
<p>

 - [Is Secure File Manager free?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#is-secure-file-manager-free)
 - [Is Secure File Manager open source?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#is-secure-file-manager-open-source)
 - [Where I can find a privacy policy?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#where-i-can-find-a-privacy-policy)
 - [I uninstalled my app and all my hidden files are gone. Also, I can't decrypt encrypted files. What should I do now?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#i-uninstalled-my-app-and-all-my-hidden-files-are-gone-also-i-cant-decrypt-encrypted-files-what-should-i-do-now)
 - [How can I safely uninstall the app?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#how-can-i-safely-uninstall-the-app)
 - [How do you store my password?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#how-do-you-store-my-password)
 - [How can I lock the app?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#how-can-i-lock-the-app)
 - [How are my files encrypted?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#how-are-my-files-encrypted)
 - [Where can I request a new feature?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#where-can-i-request-a-new-feature)
 - [Where can I report a security issue or a vulnerability that I found?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#where-can-i-report-a-security-issue-or-a-vulnerability-that-i-found)
 - [The application behaves unexpectedly. What should I do now?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#the-application-behaves-unexpectedly-what-should-i-do-now)
 - [Why the app needs _(specific)_ permission?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#why-the-app-needs-specific-permission)
 - [Has Secure File manager been audited?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#has-secure-file-manager-been-audited)
 - [The app doesn't work when I have night light enabled. What can I do?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#the-app-doesnt-work-when-i-have-night-light-enabled-what-can-i-do)
 - [I have a rooted device. Are there some security implications](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#i-have-a-rooted-device-are-there-some-security-implications)
 - [I still haven't found an answer?](https://github.com/Secure-File-Manager/Secure-File-Manager/wiki/Frequently-Asked-Questions/_edit#i-still-havent-found-an-answer)

</p>
</details>

---

### Is Secure File Manager free?

Yes, Secure File Manager is free forever.

### Is Secure File Manager open source?

Yes, Secure File Manager is published as open-source under [GPLv3](https://github.com/Secure-File-Manager/Secure-File-Manager/LICENSE). Check out our [GitHub repository](https://github.com/Secure-File-Manager). We welcome you to review the code, give us feedback or contribute!﻿

### Where I can find a privacy policy?

You can read our privacy policy [here](https://github.com/Secure-File-Manager/Secure-File-Manager/PRIVACY_POLICY.md).

### I uninstalled my app and all my hidden files are gone. Also, I can't decrypt encrypted files. What should I do now?

Your hidden files are gone forever, there is no way to get them back. Additionally, as your private keys are gone forever you will never be able to decrypt your encrypted files. You were warned! Multiple times...

### How can I safely uninstall the app?

Unhide all your hidden files and decrypt all encrypted files. After that, you can uninstall the app without a doubt.

### How do you store my password?

We do not store your password while you are creating an encrypted Zip file. If you create an app password, your password is hashed, before it is stored. We use the [Argon2](https://en.wikipedia.org/wiki/Argon2) algorithm, more precisely the [Argon2Kt](https://github.com/lambdapioneer/argon2kt) library. We use the following parameters for the hashing algorithm as a compromise of security and user-friendliness:

- **Mode:** Argon2id
- **Salt:** random 258 bits generated with [SecureRandom](https://developer.android.com/reference/java/security/SecureRandom)
- **Iterations:** 1
- **Memory cost:** 5% of memory in your device _(default 64 MB)_
- **Parallelism:** Number of processors in your device
- **Hash length:** 258 bits

The hashed password is stored encrypted in [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences) using [androidx.security.crypto](https://developer.android.com/reference/androidx/security/crypto/package-summary) library. We using AES256-GCM without padding as a key to encrypt the hashed password. If is available, the StrongBox security chip is used. The key is stored in the [Android](https://developer.android.com/training/articles/keystore) keystore. If you are more curious, you are can check the source code.

### How can I lock the app?

To lock the app you must first setup authentication at the settings. After that, the app is locked if one of the conditions is met:

- app was quit
- app was locked via the notification
- device was locked
- app was failed _(some app bug)_
- device was booted _(if a device was unexpectedly turned off and the app is in unlocked state)_

These requirements are designed for a better user experience.

### How are my files encrypted?

We use the [androidx.security.crypto](https://developer.android.com/reference/androidx/security/crypto/package-summary) library, more precisely [EncryptedFile](https://developer.android.com/reference/androidx/security/crypto/EncryptedFile) to encrypt your files. We are using AES256-GCM without padding as a key to encrypt your files. If it's available, the StrongBox security chip is used. The key is stored in the [Android](https://developer.android.com/training/articles/keystore) keystore. If you are more curious you can check the source code.

### Where can I request a new feature?

You can create [a new issue in the Github repository](https://github.com/Secure-File-Manager/Secure-File-Manager/issues).

### Where can I report a security issue or a vulnerability that I found?

The app code is published on [GitHub](https://github.com/Secure-File-Manager), and we invite security experts to check the code. In case you find a vulnerability in the app, please report it directly to us so we can fix it.

### The application behaves unexpectedly. What should I do now?

Please, create [a new issue in the Github repository](https://github.com/Secure-File-Manager/Secure-File-Manager/issues) or contact us directly so we can fix it.

### Why the app needs _(specific)_ permission?

The app needs the following permissions:

 - [READ_EXTERNAL_STORAGE](https://developer.android.com/reference/android/Manifest.permission#READ_EXTERNAL_STORAGE) and [WRITE_EXTERNAL_STORAGE](https://developer.android.com/reference/android/Manifest.permission#WRITE_EXTERNAL_STORAGE) - this is the file manager
 - [FOREGROUND_SERVICE](https://developer.android.com/reference/android/Manifest.permission#FOREGROUND_SERVICE) - needed for creating a notification
 - [RECEIVE_BOOT_COMPLETED](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_BOOT_COMPLETED) - to lock the app after a phone is booted _(if the phone was unexpectedly turned off and the app was in an unlocked state)_

### Has Secure File manager been audited?

Not yet. We would be pleased if somebody could do it.

### The app doesn't work when I have night light enabled. What can I do?

If you use a third-party night light app, you should turn off the night light. Alternatively, you can use native night light mode if your Android OS supports it.

_Note: The night light is not working, because of the enabled [filterTouchesWhenObscured](https://developer.android.com/reference/android/view/View.html#attr_android:filterTouchesWhenObscured) flag. This flag prevents tapjacking attacks._

### I have a rooted device. Are there some security implications?

If you have a rooted device, this app is useless. In this case, all security features in this app can be easily exploited.

### I still haven't found an answer?

Please, create [a new issue in the Github repository](https://github.com/Secure-File-Manager/Secure-File-Manager/issues) or contact us directly so we can fix it.
